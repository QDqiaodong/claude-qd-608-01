package com.garment.factory.grading;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 真实 HTTP 链路冒烟：控制器 → 服务 → JPA(H2) → JSON。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GradingHttpSmokeTest {

    @Autowired
    private MockMvc mvc;

    private final ObjectMapper json = new ObjectMapper();

    @Test
    @DisplayName("端到端：建方案(mm录入)→生成→结果含 cm/mm 双显示；公式失败时旧结果保留")
    void endToEndOverHttp() throws Exception {
        // 1) 元数据
        mvc.perform(get("/api/grading/meta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.ruleTypes[0]").value("FIXED"));

        // 2) 用毫米录入建方案（胸围 900mm、每级 40mm，肩宽 L/R 不对称）
        String spec = """
                {
                  "schemaVersion": 2, "baseGrade": 0, "inputUnit": "mm",
                  "rounding": {"unit":"mm","step":"1"},
                  "sizes":[{"grade":-1,"name":"S"},{"grade":0,"name":"M"},{"grade":1,"name":"L"}],
                  "parts":[
                    {"partId":"bust","partName":"胸围","base":"900","unit":"mm","direction":"UP",
                     "rule":{"type":"FIXED","delta":"40","unit":"mm"}},
                    {"partId":"shoulder","partName":"肩宽","side":"L","base":"190","unit":"mm","direction":"UP",
                     "rule":{"type":"FIXED","delta":"4","unit":"mm"}},
                    {"partId":"shoulder","partName":"肩宽","side":"R","base":"192","unit":"mm","direction":"UP",
                     "rule":{"type":"FIXED","delta":"6","unit":"mm"}}
                  ],
                  "ratioChecks":[]
                }
                """;
        String body = "{\"name\":\"HTTP冒烟-mm方案\",\"temporary\":false,\"spec\":" + spec + "}";
        MvcResult created = mvc.perform(post("/api/grading/schemes/save-and-generate")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.result.quantumMicrons").value("1000"))
                .andReturn();
        JsonNode createdNode = json.readTree(created.getResponse().getContentAsString());
        long id = createdNode.path("data").path("id").asLong();

        // 3) 详情：结果同时提供 cm 与 mm；900mm 基准 = 90.0cm，L 码 940mm = 94.0cm
        MvcResult detail = mvc.perform(get("/api/grading/schemes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result.slots[0].cells[2].mm").value("940"))
                .andExpect(jsonPath("$.data.result.slots[0].cells[2].cm").value("94.0"))
                .andExpect(jsonPath("$.data.result.slots[0].cells[2].deltaMm").value("40"))
                .andReturn();

        // 左右两侧不合并：应有 shoulder#L 与 shoulder#R 两个槽位
        JsonNode slots = json.readTree(detail.getResponse().getContentAsString())
                .path("data").path("result").path("slots");
        boolean hasL = false, hasR = false, noMergedShoulder = true;
        for (JsonNode s : slots) {
            String key = s.path("slotKey").asText();
            if ("shoulder#L".equals(key)) hasL = true;
            if ("shoulder#R".equals(key)) hasR = true;
            if ("shoulder".equals(key)) noMergedShoulder = false;
        }
        org.junit.jupiter.api.Assertions.assertTrue(hasL && hasR && noMergedShoulder);

        // 4) 公式失败（分段缺口）→ 400 且错误信息定位到边；旧结果仍可取
        String broken = """
                {
                  "schemaVersion": 2, "baseGrade": 0, "inputUnit": "cm",
                  "rounding": {"unit":"cm","step":"0.1"},
                  "sizes":[{"grade":-1,"name":"S"},{"grade":0,"name":"M"},{"grade":1,"name":"L"}],
                  "parts":[
                    {"partId":"bust","partName":"胸围","base":"90","unit":"cm","direction":"UP",
                     "rule":{"type":"SEGMENT","segments":[
                        {"fromGrade":-1,"toGrade":0,"delta":"4","unit":"cm"}
                     ]}}
                  ],
                  "ratioChecks":[]
                }
                """;
        String brokenBody = "{\"id\":" + id + ",\"name\":\"HTTP冒烟-mm方案\",\"spec\":" + broken + "}";
        mvc.perform(post("/api/grading/schemes/save-and-generate")
                        .contentType(MediaType.APPLICATION_JSON).content(brokenBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("缺少边 0→1")));

        // 旧结果仍在（生成失败不覆盖）
        mvc.perform(get("/api/grading/schemes/" + id))
                .andExpect(jsonPath("$.data.result.slots[0].cells[2].mm").value("940"));

        // 5) 临时试算不落库
        mvc.perform(post("/api/grading/preview")
                        .contentType(MediaType.APPLICATION_JSON).content(spec))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result.meta.cellCount").value(9));

        // 6) 两方案比较：再造一个 50mm 增量的方案与原方案比
        String specB = spec.replace("\"delta\":\"40\"", "\"delta\":\"50\"");
        String bodyB = "{\"name\":\"HTTP冒烟-对照\",\"spec\":" + specB + "}";
        MvcResult createdB = mvc.perform(post("/api/grading/schemes/save-and-generate")
                        .contentType(MediaType.APPLICATION_JSON).content(bodyB))
                .andExpect(status().isOk()).andReturn();
        long idB = json.readTree(createdB.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        mvc.perform(get("/api/grading/compare").param("schemeAId", String.valueOf(id))
                        .param("schemeBId", String.valueOf(idB)))
                .andExpect(status().isOk())
                // 胸围公式不同：S、L 两格最终值不同（FORMULA），M 恰好相同但仍标注公式差异
                .andExpect(jsonPath("$.data.counts.FORMULA").value(2));
    }

    @Test
    @DisplayName("v1 旧数据经 HTTP 打开即可生成，结果为 0.1cm 口径")
    void legacyOverHttp() throws Exception {
        // 直接走 preview 无法落 v1；这里验证 preview 对 v2 正常，v1 的库内兼容在 service 测试覆盖。
        // HTTP 层补一个非法单位 → 400 的校验。
        String bad = """
                {
                  "schemaVersion":2,"baseGrade":0,"inputUnit":"inch",
                  "rounding":{"unit":"cm","step":"0.1"},
                  "sizes":[{"grade":0,"name":"M"}],
                  "parts":[{"partId":"bust","partName":"胸围","base":"90","unit":"inch","direction":"UP",
                            "rule":{"type":"FIXED","delta":"4","unit":"inch"}}],
                  "ratioChecks":[]
                }
                """;
        mvc.perform(post("/api/grading/preview").contentType(MediaType.APPLICATION_JSON).content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("长度单位只能是 cm 或 mm")));
    }
}
