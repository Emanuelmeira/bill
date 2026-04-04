package com.bill.repository;

import com.bill.model.Income;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface IncomeRepository extends MongoRepository<Income, String> {

    Optional<Income> findByMonthAndYear(int month, int year);

    List<Income> findAllByOrderByYearDescMonthDesc();
}
