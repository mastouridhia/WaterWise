package com.waterwise.app.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class CareMapperTest {

    @Test
    public void benchmarkRangeGivesRoundedAverage() {
        assertEquals(Integer.valueOf(9), CareMapper.daysFromBenchmark("7-10"));
    }

    @Test
    public void benchmarkWithQuotesAsReturnedByApi() {
        assertEquals(Integer.valueOf(9), CareMapper.daysFromBenchmark("\"7-10\""));
    }

    @Test
    public void benchmarkSingleNumber() {
        assertEquals(Integer.valueOf(5), CareMapper.daysFromBenchmark("5"));
    }

    @Test
    public void benchmarkWithoutNumberIsNull() {
        assertNull(CareMapper.daysFromBenchmark("unknown"));
        assertNull(CareMapper.daysFromBenchmark(null));
    }

    @Test
    public void levelsMapToDays() {
        assertEquals(3, CareMapper.daysFromLevel("Frequent"));
        assertEquals(7, CareMapper.daysFromLevel("Average"));
        assertEquals(14, CareMapper.daysFromLevel("minimum"));
        assertEquals(CareMapper.DEFAULT_DAYS, CareMapper.daysFromLevel("weird"));
        assertEquals(CareMapper.DEFAULT_DAYS, CareMapper.daysFromLevel(null));
    }

    @Test
    public void benchmarkWinsOverLevel() {
        assertEquals(9, CareMapper.wateringDays("7-10", "Minimum"));
        assertEquals(14, CareMapper.wateringDays(null, "Minimum"));
    }

    @Test
    public void sunlightIsCapitalizedAndJoined() {
        assertEquals("Full sun, Part shade",
                CareMapper.formatSunlight(Arrays.asList("full sun", "part shade")));
        assertNull(CareMapper.formatSunlight(Collections.emptyList()));
    }
}
