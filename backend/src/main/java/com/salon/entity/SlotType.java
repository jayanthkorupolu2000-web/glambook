package com.salon.entity;

public enum SlotType {
    WORKING,      // bookable by customers
    LUNCH_BREAK,  // lunch break — not bookable
    BREAK,        // short break — not bookable
    BLOCKED       // blocked off — not bookable
}
