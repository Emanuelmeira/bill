package com.bill.controller;

import com.bill.model.Category;
import com.bill.model.Cost;
import com.bill.service.CategoryService;
import com.bill.service.CostService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final CategoryService categoryService;
    private final CostService costService;

    public ApiController(CategoryService categoryService, CostService costService) {
        this.categoryService = categoryService;
        this.costService = costService;
    }

    @GetMapping("/costs")
    public List<Map<String, Object>> costs() {
        List<Category> categories = categoryService.findAll();
        List<Cost> allCosts = costService.findAll();

        Map<String, List<Cost>> costsByCategory = allCosts.stream()
                .filter(c -> c.getCategoryId() != null)
                .collect(Collectors.groupingBy(Cost::getCategoryId));

        return categories.stream().map(cat -> {
            List<Cost> catCosts = costsByCategory.getOrDefault(cat.getId(), List.of());
            double total = catCosts.stream().mapToDouble(Cost::getValue).sum();

            List<Map<String, Object>> costList = catCosts.stream().map(c -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", c.getId());
                map.put("value", c.getValue());
                map.put("comment", c.getComment() != null ? c.getComment() : "");
                map.put("month", c.getMonth());
                map.put("year", c.getYear());
                return map;
            }).toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("category", cat.getName());
            result.put("color", cat.getColor());
            result.put("total", total);
            result.put("costs", costList);
            return result;
        }).toList();
    }
}
