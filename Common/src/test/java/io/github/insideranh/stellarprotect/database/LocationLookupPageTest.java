package io.github.insideranh.stellarprotect.database;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocationLookupPageTest {

    @Test
    void keepsOneLookaheadRowOutOfTheVisiblePage() {
        LocationLookupPage<Integer> page = LocationLookupPage.fromCandidates(
            Arrays.asList(1, 2, 3, 4),
            0,
            3
        );

        assertEquals(Arrays.asList(1, 2, 3), page.getRows());
        assertTrue(page.hasMore());
        assertEquals(4L, page.getEstimatedTotal());
    }

    @Test
    void reportsTheCurrentPageAsTerminalWithoutALookaheadRow() {
        LocationLookupPage<Integer> page = LocationLookupPage.fromCandidates(
            Arrays.asList(21, 22),
            20,
            10
        );

        assertEquals(Arrays.asList(21, 22), page.getRows());
        assertFalse(page.hasMore());
        assertEquals(22L, page.getEstimatedTotal());
    }

    @Test
    void requestsOnlyEnoughDatabaseRowsToCompleteThePageAndLookAhead() {
        assertEquals(11, LocationLookupPage.databaseFetchLimit(10, 0));
        assertEquals(4, LocationLookupPage.databaseFetchLimit(10, 7));
        assertEquals(0, LocationLookupPage.databaseFetchLimit(10, 11));
    }

    @Test
    void saturatesTheDatabaseOffsetInsteadOfOverflowing() {
        assertEquals(13, LocationLookupPage.databaseSkip(10, 3));
        assertEquals(Integer.MAX_VALUE, LocationLookupPage.databaseSkip(Integer.MAX_VALUE, 1));
    }

    @Test
    void saturatesEstimatedTotalsInsteadOfOverflowing() {
        LocationLookupPage<Integer> page = LocationLookupPage.fromCandidates(
            Collections.singletonList(1),
            Integer.MAX_VALUE,
            1
        );

        assertEquals(2147483648L, page.getEstimatedTotal());
    }

    @Test
    void rejectsInvalidPaginationInput() {
        assertThrows(IllegalArgumentException.class,
            () -> LocationLookupPage.fromCandidates(Collections.emptyList(), -1, 10));
        assertThrows(IllegalArgumentException.class,
            () -> LocationLookupPage.fromCandidates(Collections.emptyList(), 0, 0));
        assertThrows(IllegalArgumentException.class,
            () -> LocationLookupPage.databaseFetchLimit(10, -1));
        assertThrows(IllegalArgumentException.class,
            () -> LocationLookupPage.databaseSkip(-1, 0));
        assertThrows(IllegalArgumentException.class,
            () -> LocationLookupPage.databaseSkip(0, -1));
    }
}
