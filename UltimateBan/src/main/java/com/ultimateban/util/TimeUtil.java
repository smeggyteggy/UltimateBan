package com.ultimateban.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    private static final Pattern TIME_PATTERN = Pattern.compile("(?:([0-9]+)\\s*y[a-z]*[,\\s]*)?" +
            "(?:([0-9]+)\\s*mo[a-z]*[,\\s]*)?" +
            "(?:([0-9]+)\\s*w[a-z]*[,\\s]*)?" +
            "(?:([0-9]+)\\s*d[a-z]*[,\\s]*)?" +
            "(?:([0-9]+)\\s*h[a-z]*[,\\s]*)?" +
            "(?:([0-9]+)\\s*m[a-z]*[,\\s]*)?" +
            "(?:([0-9]+)\\s*(?:s[a-z]*)?)?", Pattern.CASE_INSENSITIVE);

    /**
     * Parse a time string (e.g. 3h 5m) into milliseconds
     *
     * @param input The time string to parse
     * @return The parsed time in milliseconds
     * @throws IllegalArgumentException if the format is invalid
     */
    public static long parseDuration(String input) throws IllegalArgumentException {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Time input cannot be empty");
        }

        Matcher matcher = TIME_PATTERN.matcher(input);
        
        // Check if the input format matches our pattern
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid time format");
        }

        // If nothing was captured, the format is wrong
        if (matcher.group() == null || matcher.group().isEmpty()) {
            throw new IllegalArgumentException("Invalid time format");
        }

        long years = parseGroup(matcher, 1, TimeUnit.DAYS.toMillis(365));
        long months = parseGroup(matcher, 2, TimeUnit.DAYS.toMillis(30));
        long weeks = parseGroup(matcher, 3, TimeUnit.DAYS.toMillis(7));
        long days = parseGroup(matcher, 4, TimeUnit.DAYS.toMillis(1));
        long hours = parseGroup(matcher, 5, TimeUnit.HOURS.toMillis(1));
        long minutes = parseGroup(matcher, 6, TimeUnit.MINUTES.toMillis(1));
        long seconds = parseGroup(matcher, 7, TimeUnit.SECONDS.toMillis(1));

        long result = years + months + weeks + days + hours + minutes + seconds;
        
        if (result <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
        
        return result;
    }

    private static long parseGroup(Matcher matcher, int group, long multiplier) {
        String value = matcher.group(group);
        if (value != null && !value.isEmpty()) {
            try {
                return Long.parseLong(value) * multiplier;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid time format");
            }
        }
        return 0;
    }

    /**
     * Format milliseconds into a human-readable string
     *
     * @param millis Time in milliseconds to format
     * @return Formatted time string
     */
    public static String formatDuration(long millis) {
        if (millis < 1000) {
            return "less than a second";
        }

        StringBuilder sb = new StringBuilder();
        
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        if (days > 0) {
            sb.append(days).append(days == 1 ? " day" : " days");
            millis -= TimeUnit.DAYS.toMillis(days);
            
            if (millis > 0) {
                sb.append(", ");
            }
        }

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        if (hours > 0) {
            sb.append(hours).append(hours == 1 ? " hour" : " hours");
            millis -= TimeUnit.HOURS.toMillis(hours);
            
            if (millis > 0) {
                sb.append(", ");
            }
        }

        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        if (minutes > 0) {
            sb.append(minutes).append(minutes == 1 ? " minute" : " minutes");
            millis -= TimeUnit.MINUTES.toMillis(minutes);
            
            if (millis > 0) {
                sb.append(", ");
            }
        }

        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        if (seconds > 0) {
            sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }

        return sb.toString();
    }

    /**
     * Convert a duration (in milliseconds) to a compact time format
     * 
     * @param millis Time in milliseconds
     * @return Compact time format (e.g. "1d 6h 30m")
     */
    public static String toCompactTime(long millis) {
        if (millis < 1000) {
            return "0s";
        }

        StringBuilder sb = new StringBuilder();
        
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        
        if (days > 0) {
            sb.append(days).append("d ");
        }
        
        if (hours > 0 || (days > 0 && (minutes > 0 || seconds > 0))) {
            sb.append(hours).append("h ");
        }
        
        if (minutes > 0 || (hours > 0 && seconds > 0)) {
            sb.append(minutes).append("m ");
        }
        
        if (seconds > 0 || (sb.length() == 0)) {
            sb.append(seconds).append("s");
        }
        
        return sb.toString().trim();
    }

    /**
     * Format a timestamp as a readable date
     *
     * @param timestamp The timestamp to format
     * @return The formatted date
     */
    public static String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }
    
    /**
     * Format the remaining time from now until a future timestamp
     *
     * @param timeRemaining The time remaining in milliseconds
     * @return The formatted time remaining
     */
    public static String formatTimeRemaining(long timeRemaining) {
        if (timeRemaining <= 0) {
            return "Expired";
        }
        
        return formatDuration(timeRemaining);
    }
} 