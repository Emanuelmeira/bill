package com.bill.controller;

import com.bill.model.Income;
import com.bill.service.IncomeService;
import com.bill.util.FormatUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/incomes")
public class IncomeController {

    private static final String[] MONTH_NAMES = {
            "", "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };

    private final IncomeService incomeService;

    public IncomeController(IncomeService incomeService) {
        this.incomeService = incomeService;
    }

    @GetMapping
    public String list(Model model) {
        List<Income> incomes = incomeService.findAll();

        var incomeViews = incomes.stream().map(inc -> new IncomeView(
                inc.getId(),
                inc.getFormattedValue(),
                MONTH_NAMES[inc.getMonth()] + " " + inc.getYear(),
                inc.getDescription() != null ? inc.getDescription() : ""
        )).toList();

        model.addAttribute("incomes", incomeViews);
        model.addAttribute("activePage", "incomes");
        return "incomes";
    }

    @PostMapping("/add")
    public String add(@RequestParam String value,
                      @RequestParam String refMonth,
                      @RequestParam(defaultValue = "") String description) {
        String valueStr = value.replace(",", ".");
        double val;
        try {
            val = Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            return "redirect:/incomes";
        }

        int month, year;
        try {
            String[] parts = refMonth.split("-");
            year = Integer.parseInt(parts[0]);
            month = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return "redirect:/incomes";
        }

        var existing = incomeService.findByMonthAndYear(month, year);
        if (existing.isPresent()) {
            Income inc = existing.get();
            inc.setValue(val);
            inc.setDescription(description);
            incomeService.save(inc);
        } else {
            var income = new Income();
            income.setValue(val);
            income.setMonth(month);
            income.setYear(year);
            income.setDescription(description);
            income.setCreatedAt(LocalDateTime.now());
            incomeService.save(income);
        }

        return "redirect:/incomes";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id) {
        incomeService.deleteById(id);
        return "redirect:/incomes";
    }

    public record IncomeView(String id, String formattedValue, String monthLabel, String description) {}
}
