package com.biorxiv.util;

import java.util.Locale;

/**
 * Categories are matched as plain strings between {@code articles.category} and
 * {@code subscriptions.category}, so both sides must be stored in the same canonical form.
 */
public final class CategoryUtils {

    private CategoryUtils() {
    }

    /**
     * Canonical form: trimmed, inner whitespace collapsed to one space, lower case.
     * Returns {@code null} for {@code null} or blank input.
     */
    public static String normalize(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return category.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
