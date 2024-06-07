package xyz.duncanruns.prismarinetracker.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public final class FormattingUtil {
    private FormattingUtil() {
    }

    public static String formatDate(long millis) {
        Date date = new Date(millis);

        // Modify the day part to add appropriate suffix
        String dayWithSuffix = new SimpleDateFormat("d").format(date);
        int day = Integer.parseInt(dayWithSuffix);
        String suffix = getDayOfMonthSuffix(day);

        // Reconstruct the formatted date string with the proper suffix
        return new SimpleDateFormat("MMMM ").format(date) + day + suffix +
                new SimpleDateFormat(", yyyy, HH:mm").format(date);
    }

    private static String getDayOfMonthSuffix(int n) {
        if (n >= 11 && n <= 13) {
            return "th";
        }
        switch (n % 10) {
            case 1:
                return "st";
            case 2:
                return "nd";
            case 3:
                return "rd";
            default:
                return "th";
        }
    }

    public static long getAverageTime(List<Long> times) {
        if (times.isEmpty()) return 0;
        long total = times.stream().mapToLong(v -> v).sum();
        return total / times.size();
    }

    public static String formatMillis(long totalMillis) {
        return formatSeconds(totalMillis / 1000);
    }

    public static String formatSeconds(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours == 0) {
            return String.format("%d:%02d", minutes, seconds);
        }
        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }

    public static String getPerHour(int total, long timePlayed) {
        double hoursPlayed = timePlayed / 3600000.0;
        double occurrencesPerHour = total / hoursPlayed;
        return String.format("%.2f", occurrencesPerHour);
    }
}