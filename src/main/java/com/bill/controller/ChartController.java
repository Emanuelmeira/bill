package com.bill.controller;

import com.bill.model.Cost;
import com.bill.model.Income;
import com.bill.repository.CostRepository;
import com.bill.repository.IncomeRepository;
import com.bill.util.FormatUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class ChartController {

    private static final String[] MONTH_SHORT = {
            "", "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
            "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    };

    private static final String[] MONTH_FULL = {
            "", "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };

    private final CostRepository costRepository;
    private final IncomeRepository incomeRepository;

    public ChartController(CostRepository costRepository, IncomeRepository incomeRepository) {
        this.costRepository = costRepository;
        this.incomeRepository = incomeRepository;
    }

    @GetMapping("/charts")
    public String charts(Model model) {
        LocalDate now = LocalDate.now();

        List<int[]> months = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            LocalDate d = now.minusMonths(i);
            months.add(new int[]{d.getMonthValue(), d.getYear()});
        }

        Map<String, Double> costTotals = costRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        c -> c.getYear() + "-" + c.getMonth(),
                        Collectors.summingDouble(Cost::getValue)));

        Map<String, Double> incomeTotals = incomeRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        i -> i.getYear() + "-" + i.getMonth(),
                        Collectors.summingDouble(Income::getValue)));

        List<String> labels = new ArrayList<>();
        List<Double> expenses = new ArrayList<>();
        List<Double> incomes = new ArrayList<>();
        List<List<String>> tableRows = new ArrayList<>();

        for (int[] my : months) {
            String key = my[1] + "-" + my[0];
            double expense = costTotals.getOrDefault(key, 0.0);
            double income = incomeTotals.getOrDefault(key, 0.0);
            double balance = income - expense;

            labels.add(MONTH_SHORT[my[0]] + " " + my[1]);
            expenses.add(expense);
            incomes.add(income);

            tableRows.add(List.of(
                    MONTH_FULL[my[0]] + " " + my[1],
                    FormatUtils.formatCurrency(income),
                    FormatUtils.formatCurrency(expense),
                    FormatUtils.formatCurrency(balance),
                    balance >= 0 ? "positive" : "negative"
            ));
        }

        model.addAttribute("labels", labels);
        model.addAttribute("expenses", expenses);
        model.addAttribute("incomes", incomes);
        model.addAttribute("tableRows", tableRows);
        model.addAttribute("activePage", "charts");

        return "charts";
    }
}
