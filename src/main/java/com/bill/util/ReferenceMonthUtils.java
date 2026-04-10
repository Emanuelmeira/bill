package com.bill.util;

import java.time.LocalDate;

public final class ReferenceMonthUtils {

    public static final int CUTOFF_DAY = 12;

    private ReferenceMonthUtils() {}

    public static int[] referenceMonthYear(LocalDate date) {
        if (date.getDayOfMonth() <= CUTOFF_DAY) {
            return new int[]{date.getMonthValue(), date.getYear()};
        }
        var next = date.plusMonths(1);
        return new int[]{next.getMonthValue(), next.getYear()};
    }

    public static int[] referenceMonthYearToday() {
        return referenceMonthYear(LocalDate.now());
    }
}
