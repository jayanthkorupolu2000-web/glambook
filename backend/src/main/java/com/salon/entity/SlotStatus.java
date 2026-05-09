package com.salon.entity;

/**
 * Lifecycle status of a time slot in the daily schedule.
 *
 * AVAILABLE   – future slot, open for booking
 * UNAVAILABLE – past slot or currently in-progress; cannot be booked
 * COMPLETED   – appointment in this slot has been completed
 */
public enum SlotStatus {
    AVAILABLE,
    UNAVAILABLE,
    COMPLETED
}
