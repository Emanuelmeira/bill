package com.bill.controller;

import com.bill.dto.CategoryView;
import com.bill.model.Category;
import com.bill.service.CategoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/manage")
    public String manage(Model model) {
        var categories = categoryService.findAll();
        Map<String, Long> costCounts = categoryService.getCostCounts();

        var categoryViews = categories.stream()
                .map(cat -> new CategoryView(
                        cat.getId(), cat.getName(), cat.getColor(), cat.getIcon(),
                        costCounts.getOrDefault(cat.getId(), 0L) + " lançamento(s)"))
                .toList();

        model.addAttribute("categories", categoryViews);
        model.addAttribute("activePage", "categories");
        return "categories";
    }

    @PostMapping("/add")
    public String add(@RequestParam String name,
                      @RequestParam(defaultValue = "#6366f1") String color,
                      @RequestParam(defaultValue = "receipt_long") String icon) {
        if (!name.isBlank() && !categoryService.existsByName(name.trim())) {
            categoryService.save(new Category(name.trim(), color, icon));
        }
        return "redirect:/categories/manage";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id) {
        categoryService.deleteWithCosts(id);
        return "redirect:/categories/manage";
    }
}
