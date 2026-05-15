package com.openshelves.model.enums;

/**
 * Defines the strategy used to resolve conflicts when multiple metadata providers
 * supply different values for the same field.
 */
public enum MergeStrategy {
    /** Select the value from the provider with the highest defined priority. */
    PRIORITY,

    /** Select the first non-null value encountered among providers. */
    FIRST_NON_NULL,

    /** Select the value with the greatest character length (useful for descriptions). */
    LONGEST_TEXT,

    /** Select the value most commonly supplied by all providers (majority wins). */
    VOTING,

    /** Use an AI model to synthesize a consolidated value from all provider inputs. */
    AI_SYNTHESIZE,

    /** Mark the field for manual resolution by a human moderator. */
    MANUAL
}
