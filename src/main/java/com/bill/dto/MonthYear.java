package com.bill.dto;

public record MonthYear(int month, int year) implements Comparable<MonthYear> {

    @Override
    public int compareTo(MonthYear other) {
        int cmp = Integer.compare(other.year, this.year);
        return cmp != 0 ? cmp : Integer.compare(other.month, this.month);
    }
}
