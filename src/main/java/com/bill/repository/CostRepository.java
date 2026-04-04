package com.bill.repository;

import com.bill.model.Cost;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CostRepository extends MongoRepository<Cost, String> {

    List<Cost> findByMonthAndYearOrderByCreatedAtDesc(int month, int year);

    List<Cost> findAllByOrderByCreatedAtDesc();

    void deleteByCategoryId(String categoryId);

    long countByCategoryId(String categoryId);

    List<Cost> findByGroupId(String groupId);
}
