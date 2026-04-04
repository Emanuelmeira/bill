package com.bill.dto;

import com.bill.model.Cost;
import com.bill.util.FormatUtils;

import java.util.List;

public class CategoryCostGroup {

    private final String id;
    private final String name;
    private final String color;
    private final String icon;
    private final List<Cost> costs;
    private final double total;

    public CategoryCostGroup(String id, String name, String color, String icon, List<Cost> costs) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.icon = icon;
        this.costs = costs;
        this.total = costs.stream().mapToDouble(Cost::getValue).sum();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getColor() { return color; }
    public String getIcon() { return icon; }
    public List<Cost> getCosts() { return costs; }
    public double getTotal() { return total; }
    public String getFormattedTotal() { return FormatUtils.formatCurrency(total); }
}
