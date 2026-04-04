package com.bill.config;

import com.bill.model.Category;
import com.bill.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public DataSeeder(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0) return;

        categoryRepository.saveAll(List.of(
                new Category("Celular", "#6366f1", "smartphone"),
                new Category("Moradia", "#f59e0b", "home"),
                new Category("Compras", "#10b981", "shopping_cart"),
                new Category("Aline", "#ec4899", "person"),
                new Category("Emanuel", "#3b82f6", "person"),
                new Category("Casal", "#f43f5e", "favorite"),
                new Category("Custos Extras", "#8b5cf6", "add_circle"),
                new Category("Fixos Recorrentes", "#0ea5e9", "autorenew"),
                new Category("Saúde", "#14b8a6", "health_and_safety"),
                new Category("Theo", "#f97316", "child_care"),
                new Category("Transporte", "#64748b", "local_taxi")
        ));
    }
}
