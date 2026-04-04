package com.bill.util;

import java.text.NumberFormat;
import java.util.Locale;

public final class FormatUtils {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private FormatUtils() {}

    public static String formatCurrency(double value) {
        var nf = NumberFormat.getNumberInstance(PT_BR);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "R$ " + nf.format(value);
    }

    public static String formatInputValue(double value) {
        return String.format("%.2f", value).replace('.', ',');
    }

    public static String formatRefMonth(int year, int month) {
        return String.format("%04d-%02d", year, month);
    }
}
