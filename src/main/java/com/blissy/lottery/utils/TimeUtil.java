package com.blissy.lottery.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for time-related operations.
 */
public class TimeUtil {

    /**
     * Calculate the number of ticks until a specific time.
     * @param time The target time
     * @return Number of ticks
     */
    public static long getTicksUntil(LocalDateTime time) {
        LocalDateTime now = LocalDateTime.now();

        // If the time is in the past, return a small delay
        if (time.isBefore(now)) {
            return 20; // 1 second
        }

        Duration duration = Duration.between(now, time);
        return duration.getSeconds() * 20; // 20 ticks per second
    }

    /**
     * Format the time until a specific date in a readable format.
     * @param time The target time
     * @return Formatted time string (e.g. "2 days, 3 hours, 45 minutes")
     */
    public static String formatTimeUntil(LocalDateTime time) {
        LocalDateTime now = LocalDateTime.now();

        if (time.isBefore(now)) {
            return "0 minutes";
        }

        long days = ChronoUnit.DAYS.between(now, time);
        long hours = ChronoUnit.HOURS.between(now.plusDays(days), time);
        long minutes = ChronoUnit.MINUTES.between(now.plusDays(days).plusHours(hours), time);

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append(days == 1 ? " day" : " days");
        }

        if (hours > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
        }

        if (minutes > 0 || sb.length() == 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
        }

        return sb.toString();
    }
}