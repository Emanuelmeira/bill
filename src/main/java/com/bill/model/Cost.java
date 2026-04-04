package com.bill.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.bill.util.FormatUtils;

import java.time.LocalDateTime;

@Document(collection = "costs")
public class Cost {

    @Id
    private String id;
    private double value;
    private String categoryId;
    private String comment;
    private int month;
    private int year;
    private LocalDateTime createdAt;
    private String groupId;

    public Cost() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }

    public boolean isInstallment() {
        return groupId != null && !groupId.isBlank();
    }

    public String getFormattedValue() {
        return FormatUtils.formatCurrency(value);
    }

    public String getDisplayComment() {
        return (comment != null && !comment.isBlank()) ? comment : "—";
    }
}
