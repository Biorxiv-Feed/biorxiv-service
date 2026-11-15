package com.biorxiv.entity;

/**
 * Delivery state of a {@link Notification}. Persisted by name ({@code EnumType.STRING}),
 * so constants must not be renamed without a data migration.
 */
public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED
}