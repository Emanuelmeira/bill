package com.bill.service;

import com.bill.model.Category;
import com.bill.repository.CategoryRepository;
import com.bill.repository.CostRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CostRepository costRepository;

    public CategoryService(CategoryRepository categoryRepository, CostRepository costRepository) {
        this.categoryRepository = categoryRepository;
        this.costRepository = costRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    public Optional<Category> findById(String id) {
        return categoryRepository.findById(id);
    }

    public boolean existsByName(String name) {
        return categoryRepository.findByName(name).isPresent();
    }

    public Category save(Category category) {
        return categoryRepository.save(category);
    }

    public void deleteWithCosts(String categoryId) {
        costRepository.deleteByCategoryId(categoryId);
        categoryRepository.deleteById(categoryId);
    }

    public Map<String, Long> getCostCounts() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        cat -> costRepository.countByCategoryId(cat.getId())
                ));
    }
}
