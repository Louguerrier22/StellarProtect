package io.github.insideranh.stellarprotect.database;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LocationLookupPage<T> {

    private final List<T> rows;
    private final boolean hasMore;
    private final long estimatedTotal;

    private LocationLookupPage(List<T> rows, boolean hasMore, long estimatedTotal) {
        this.rows = rows;
        this.hasMore = hasMore;
        this.estimatedTotal = estimatedTotal;
    }

    public static <T> LocationLookupPage<T> fromCandidates(List<T> candidates, int skip, int pageSize) {
        if (candidates == null) {
            throw new IllegalArgumentException("candidates cannot be null");
        }
        if (skip < 0) {
            throw new IllegalArgumentException("skip cannot be negative");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }

        boolean hasMore = candidates.size() > pageSize;
        int visibleRows = Math.min(candidates.size(), pageSize);
        List<T> rows = Collections.unmodifiableList(new ArrayList<>(candidates.subList(0, visibleRows)));
        long estimatedTotal = (long) skip + visibleRows + (hasMore ? 1L : 0L);

        return new LocationLookupPage<>(rows, hasMore, estimatedTotal);
    }

    public static int databaseFetchLimit(int pageSize, int cachedCandidates) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
        if (cachedCandidates < 0) {
            throw new IllegalArgumentException("cachedCandidates cannot be negative");
        }

        long required = (long) pageSize + 1L - cachedCandidates;
        if (required <= 0L) {
            return 0;
        }
        return (int) Math.min(required, Integer.MAX_VALUE);
    }

    public static int databaseSkip(int skip, int cachedCandidates) {
        if (skip < 0) {
            throw new IllegalArgumentException("skip cannot be negative");
        }
        if (cachedCandidates < 0) {
            throw new IllegalArgumentException("cachedCandidates cannot be negative");
        }

        long databaseSkip = (long) skip + cachedCandidates;
        return (int) Math.min(databaseSkip, Integer.MAX_VALUE);
    }

    public List<T> getRows() {
        return rows;
    }

    public boolean hasMore() {
        return hasMore;
    }

    public long getEstimatedTotal() {
        return estimatedTotal;
    }
}
