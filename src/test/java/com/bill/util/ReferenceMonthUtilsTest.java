package com.bill.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReferenceMonthUtilsTest {

    @ParameterizedTest(name = "{0} → mês {1}/{2}")
    @CsvSource(delimiter = '|', value = {
            "2026-04-10|4|2026",
            "2026-04-01|4|2026",
            "2026-04-12|4|2026",
            "2026-01-01|1|2026",
            "2024-02-12|2|2024",
            "2026-12-05|12|2026",
            "2026-12-12|12|2026",
    })
    void ateDia12Inclusive_usaMesCivilAtual(String isoDate, int expectedMonth, int expectedYear) {
        int[] my = ReferenceMonthUtils.referenceMonthYear(LocalDate.parse(isoDate));
        assertThat(my).containsExactly(expectedMonth, expectedYear);
    }

    @ParameterizedTest(name = "{0} → mês {1}/{2}")
    @CsvSource(delimiter = '|', value = {
            "2026-04-13|5|2026",
            "2026-04-30|5|2026",
            "2026-01-15|2|2026",
            "2026-11-20|12|2026",
            "2026-12-13|1|2027",
            "2026-12-31|1|2027",
            "2024-02-29|3|2024",
    })
    void aposDia12_usaMesCivilSeguinte(String isoDate, int expectedMonth, int expectedYear) {
        int[] my = ReferenceMonthUtils.referenceMonthYear(LocalDate.parse(isoDate));
        assertThat(my).containsExactly(expectedMonth, expectedYear);
    }
}
