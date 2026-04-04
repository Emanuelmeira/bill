package com.bill.service;

import com.bill.dto.CategoryCostGroup;
import com.bill.dto.MonthYear;
import com.bill.model.Category;
import com.bill.model.Cost;
import com.bill.repository.CostRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
public class CostService {

    private final CostRepository costRepository;
    private final CategoryService categoryService;

    public CostService(CostRepository costRepository, CategoryService categoryService) {
        this.costRepository = costRepository;
        this.categoryService = categoryService;
    }

    public Optional<Cost> findById(String id) {
        return costRepository.findById(id);
    }

    public Cost save(Cost cost) {
        return costRepository.save(cost);
    }

    public void deleteById(String id) {
        costRepository.deleteById(id);
    }

    public List<Cost> findAll() {
        return costRepository.findAllByOrderByCreatedAtDesc();
    }

    public void addCostWithInstallments(double value, String categoryId, String comment,
                                        int month, int year, int installments) {
        var now = LocalDateTime.now();
        String groupId = installments > 1 ? UUID.randomUUID().toString() : null;

        for (int i = 0; i < installments; i++) {
            int m = month + i;
            int y = year;
            while (m > 12) {
                m -= 12;
                y++;
            }

            String label;
            if (installments > 1) {
                label = (comment != null && !comment.isBlank())
                        ? "%s (%d/%d)".formatted(comment, i + 1, installments)
                        : "Parcela %d/%d".formatted(i + 1, installments);
            } else {
                label = comment;
            }

            var cost = new Cost();
            cost.setValue(value);
            cost.setCategoryId(categoryId);
            cost.setComment(label);
            cost.setMonth(m);
            cost.setYear(y);
            cost.setCreatedAt(now);
            cost.setGroupId(groupId);
            costRepository.save(cost);
        }
    }

    public List<CategoryCostGroup> getCostsByCategory(int month, int year) {
        List<Category> categories = categoryService.findAll();
        List<Cost> costs = costRepository.findByMonthAndYearOrderByCreatedAtDesc(month, year);

        Map<String, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        Map<String, List<Cost>> costsByCategory = costs.stream()
                .filter(c -> c.getCategoryId() != null && categoryMap.containsKey(c.getCategoryId()))
                .collect(Collectors.groupingBy(Cost::getCategoryId));

        return costsByCategory.entrySet().stream()
                .map(entry -> {
                    Category cat = categoryMap.get(entry.getKey());
                    return new CategoryCostGroup(
                            cat.getId(), cat.getName(), cat.getColor(),
                            cat.getIcon(), entry.getValue());
                })
                .sorted(Comparator.comparing(CategoryCostGroup::getName))
                .toList();
    }

    public int deleteFutureInstallments(String costId) {
        var costOpt = costRepository.findById(costId);
        if (costOpt.isEmpty()) return 0;

        Cost origin = costOpt.get();
        if (origin.getGroupId() == null) return 0;

        List<Cost> siblings = costRepository.findByGroupId(origin.getGroupId());
        int originDate = origin.getYear() * 12 + origin.getMonth();

        List<Cost> future = siblings.stream()
                .filter(c -> (c.getYear() * 12 + c.getMonth()) > originDate)
                .toList();

        costRepository.deleteAll(future);
        return future.size();
    }

    public List<MonthYear> findAvailableMonths() {
        return costRepository.findAll().stream()
                .map(c -> new MonthYear(c.getMonth(), c.getYear()))
                .distinct()
                .sorted()
                .toList();
    }
}
