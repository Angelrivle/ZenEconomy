package com.cuac_xd.zeneconomy.currency;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class NumberFormatterTest {

    @Test
    void testStandardFormatting() {
        assertEquals("1,234.50", NumberFormatter.formatStandard(1234.5, 2));
        assertEquals("1,000,000", NumberFormatter.formatStandard(1000000, 0));
        assertEquals("0.00", NumberFormatter.formatStandard(0, 2));
    }

    @Test
    void testAbbreviatedFormatting() {
        assertEquals("1.5k", NumberFormatter.formatAbbreviated(1500, 2));
        assertEquals("2.5M", NumberFormatter.formatAbbreviated(2500000, 2));
        assertEquals("10B", NumberFormatter.formatAbbreviated(10000000000.0, 2));
        assertEquals("500", NumberFormatter.formatAbbreviated(500, 2));
    }
}
