package com.bill.controller;

import com.bill.dto.CategoryCostGroup;
import com.bill.dto.MonthOption;
import com.bill.dto.MonthYear;
import com.bill.model.Cost;
import com.bill.service.CategoryService;
import com.bill.service.CostService;
import com.bill.service.IncomeService;
import com.bill.util.FormatUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Controller
public class CostController {

    private static final String[] MONTH_NAMES = {
            "", "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };

    private final CostService costService;
    private final CategoryService categoryService;
    private final IncomeService incomeService;

    public CostController(CostService costService, CategoryService categoryService, IncomeService incomeService) {
        this.costService = costService;
        this.categoryService = categoryService;
        this.incomeService = incomeService;
    }

    private int[] nextMonthYear() {
        var now = LocalDateTime.now();
        int m = now.getMonthValue() + 1;
        int y = now.getYear();
        if (m > 12) { m = 1; y++; }
        return new int[]{m, y};
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("activePage", "index");
        return "index";
    }

    @PostMapping("/costs/add")
    public String addCost(@RequestParam String value,
                          @RequestParam String categoryId,
                          @RequestParam(defaultValue = "") String comment,
                          @RequestParam String refMonth,
                          @RequestParam(defaultValue = "1") int installments) {
        String valueStr = value.replace(",", ".");
        double val;
        try {
            val = Double.parseDouble(valueStr);
        } catch (NumberFormatException e) {
            return "redirect:/";
        }

        int costMonth, costYear;
        try {
            String[] parts = refMonth.split("-");
            costYear = Integer.parseInt(parts[0]);
            costMonth = Integer.parseInt(parts[1]);
        } catch (Exception e) {
            return "redirect:/";
        }

        installments = Math.max(1, Math.min(installments, 48));
        costService.addCostWithInstallments(val, categoryId, comment, costMonth, costYear, installments);

        return "redirect:/";
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) Integer month,
                            @RequestParam(required = false) Integer year,
                            Model model) {
        int[] def = nextMonthYear();
        int m = month != null ? month : def[0];
        int y = year != null ? year : def[1];

        List<CategoryCostGroup> groups = costService.getCostsByCategory(m, y);
        double grandTotal = groups.stream().mapToDouble(CategoryCostGroup::getTotal).sum();

        List<MonthYear> availableMonths = new ArrayList<>(costService.findAvailableMonths());
        Set<MonthYear> availableSet = new HashSet<>(availableMonths);
        var defPair = new MonthYear(def[0], def[1]);
        var currentPair = new MonthYear(m, y);
        if (!availableSet.contains(defPair)) availableMonths.add(defPair);
        if (!availableSet.contains(currentPair)) availableMonths.add(currentPair);
        availableMonths.sort(MonthYear::compareTo);

        List<MonthOption> monthOptions = availableMonths.stream()
                .map(my -> new MonthOption(
                        my.month(), my.year(),
                        MONTH_NAMES[my.month()] + " " + my.year(),
                        my.month() == m && my.year() == y))
                .toList();

        int prevM = m == 1 ? 12 : m - 1;
        int prevY = m == 1 ? y - 1 : y;
        int nextM = m == 12 ? 1 : m + 1;
        int nextY = m == 12 ? y + 1 : y;

        double incomeValue = incomeService.findByMonthAndYear(m, y)
                .map(inc -> inc.getValue())
                .orElse(0.0);
        double balance = incomeValue - grandTotal;

        model.addAttribute("categories", groups);
        model.addAttribute("grandTotal", grandTotal);
        model.addAttribute("grandTotalFormatted", FormatUtils.formatCurrency(grandTotal));
        model.addAttribute("totalLabel", "Gastos — " + MONTH_NAMES[m] + " " + y);
        model.addAttribute("incomeFormatted", FormatUtils.formatCurrency(incomeValue));
        model.addAttribute("incomeLabel", "Entrada — " + MONTH_NAMES[m] + " " + y);
        model.addAttribute("balanceFormatted", FormatUtils.formatCurrency(balance));
        model.addAttribute("balanceLabel", "Saldo — " + MONTH_NAMES[m] + " " + y);
        model.addAttribute("balancePositive", balance >= 0);
        model.addAttribute("hasIncome", incomeValue > 0);
        model.addAttribute("emptyMessage", "Nenhum custo em " + MONTH_NAMES[m] + " " + y);
        model.addAttribute("monthOptions", monthOptions);
        model.addAttribute("prevMonth", prevM);
        model.addAttribute("prevYear", prevY);
        model.addAttribute("nextMonth", nextM);
        model.addAttribute("nextYear", nextY);
        model.addAttribute("activePage", "dashboard");

        return "dashboard";
    }

    @GetMapping("/costs/{id}/edit")
    public String editPage(@PathVariable String id, Model model) {
        var costOpt = costService.findById(id);
        if (costOpt.isEmpty()) return "redirect:/dashboard";

        Cost cost = costOpt.get();
        model.addAttribute("cost", cost);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("refMonthValue", FormatUtils.formatRefMonth(cost.getYear(), cost.getMonth()));
        model.addAttribute("costValueFormatted", FormatUtils.formatInputValue(cost.getValue()));
        model.addAttribute("activePage", "dashboard");

        return "edit";
    }

    @PostMapping("/costs/{id}/edit")
    public String editCost(@PathVariable String id,
                           @RequestParam String value,
                           @RequestParam String categoryId,
                           @RequestParam(defaultValue = "") String comment,
                           @RequestParam(defaultValue = "") String refMonth) {
        int[] next = nextMonthYear();
        int cm = next[0], cy = next[1];

        if (!refMonth.isBlank()) {
            try {
                String[] parts = refMonth.split("-");
                cy = Integer.parseInt(parts[0]);
                cm = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}
        }

        final int costMonth = cm;
        final int costYear = cy;

        String valueStr = value.replace(",", ".");
        try {
            double val = Double.parseDouble(valueStr);
            costService.findById(id).ifPresent(cost -> {
                cost.setValue(val);
                cost.setCategoryId(categoryId);
                cost.setComment(comment);
                cost.setMonth(costMonth);
                cost.setYear(costYear);
                costService.save(cost);
            });
        } catch (NumberFormatException ignored) {}

        return "redirect:/dashboard?month=" + costMonth + "&year=" + costYear;
    }

    @PostMapping("/costs/{id}/delete")
    public String deleteCost(@PathVariable String id) {
        costService.deleteById(id);
        return "redirect:/dashboard";
    }

    @PostMapping("/costs/{id}/delete-future")
    public String deleteFutureInstallments(@PathVariable String id) {
        costService.deleteFutureInstallments(id);
        return "redirect:/dashboard";
    }
}
