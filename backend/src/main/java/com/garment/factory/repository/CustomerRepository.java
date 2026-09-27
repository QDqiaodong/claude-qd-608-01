package com.garment.factory.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garment.factory.domain.Customer;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findAllByOrderByCustomerNoAsc();

    boolean existsByCustomerNo(String customerNo);

    boolean existsByCustomerNoAndIdNot(String customerNo, Long id);
}
