package com.bill.model;

import com.bill.util.FormatUtils;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "incomes")
public class Income {

    @Id
    private String id;
    private double value;
    private int month;
    private int year;
    private String description;
    private LocalDateTime createdAt;

    public Income() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getFormattedValue() {
        return FormatUtils.formatCurrency(value);
    }
}
