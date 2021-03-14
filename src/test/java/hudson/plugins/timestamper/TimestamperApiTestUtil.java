package hudson.plugins.timestamper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import hudson.model.Run;
import hudson.plugins.timestamper.api.TimestamperAPI;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TimestamperApiTestUtil {

    public static void timestamperApi(Run<?, ?> build, List<String> unstampedLines)
            throws IOException {
        time(build, unstampedLines, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 24, "UTC", 0, 0, false);
        time(build, unstampedLines, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 24, "UTC", 3, 0, false);
        time(build, unstampedLines, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 24, "UTC", -4, 0, false);
        time(build, unstampedLines, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 24, "UTC", 0, 4, false);
        time(build, unstampedLines, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 24, "UTC", 0, -3, false);
        time(build, unstampedLines, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 24, "UTC", 2, 5, false);
        time(build, unstampedLines, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 24, "UTC", 0, 0, true);
        time(build, unstampedLines, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 24, "UTC", 3, 0, true);
        time(build, unstampedLines, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 24, "UTC", -4, 0, true);
        time(build, unstampedLines, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 24, "UTC", 0, 4, true);
        time(build, unstampedLines, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 24, "UTC", 0, -3, true);
        time(build, unstampedLines, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 24, "UTC", 2, 5, true);

        elapsed(build, unstampedLines, "'P'd'DT'H'H'm'M's.S'S'", 14, 0, 0, false);
        elapsed(build, unstampedLines, "'P'd'DT'H'H'm'M's.S'S'", 14, 3, 0, false);
        elapsed(build, unstampedLines, "'P'd'DT'H'H'm'M's.S'S'", 14, -4, 0, false);
        elapsed(build, unstampedLines, "'P'd'DT'H'H'm'M's.S'S'", 14, 0, 4, false);
        elapsed(build, unstampedLines, "'P'd'DT'H'H'm'M's.S'S'", 14, 0, -3, false);
        elapsed(build, unstampedLines, "'P'd'DT'H'H'm'M's.S'S'", 14, 2, 5, false);
        elapsed(build, unstampedLines, "'P'd'DT'H'H'm'M's.S'S'", 14, 0, 0, true);
        elapsed(build, unstampedLines, "'P'd'DT'H'H'm'M's.S'S'", 14, 3, 0, true);
        elapsed(build, unstampedLines, "'P'd'DT'H'H'm'M's.S'S'", 14, -4, 0, true);
        elapsed(build, unstampedLines, "'P'd'DT'H'H'm'M's.S'S'", 14, 0, 4, true);
        elapsed(build, unstampedLines, "'P'd'DT'H'H'm'M's.S'S'", 14, 0, -3, true);
        elapsed(build, unstampedLines, "'P'd'DT'H'H'm'M's.S'S'", 14, 2, 5, true);

        currentTime(build, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "UTC");
    }

    private static void time(
            Run<?, ?> build,
            List<String> unstampedLines,
            String pattern,
            int patternLength,
            String timezone,
            int startLine,
            int endLine,
            boolean appendLog)
            throws IOException {
        List<String> results =
                getQueryResults(
                        build,
                        String.format(
                                "time=%s&timeZone=%s%s%s%s",
                                pattern,
                                timezone,
                                startLine != 0 ? String.format("&startLine=%s", startLine) : "",
                                endLine != 0 ? String.format("&endLine=%s", endLine) : "",
                                appendLog ? "&appendLog" : ""));
        int lower;
        if (startLine < 0) {
            lower = unstampedLines.size() + startLine + 1;
        } else if (startLine > 0) {
            lower = startLine;
        } else {
            lower = 1;
        }
        int upper;
        if (endLine < 0) {
            upper = unstampedLines.size() + endLine + 1;
        } else if (endLine > 0) {
            upper = endLine;
        } else {
            upper = unstampedLines.size();
        }
        assertEquals(
                IntStream.rangeClosed(lower, upper).count(),
                results.size());
        for (int i = 0; i < results.size(); i++) {
            if (appendLog) {
                assertTrue(results.get(i).length() >= patternLength);
            } else {
                assertEquals(patternLength, results.get(i).length());
            }

            String timestamp = results.get(i).substring(0, patternLength);
            assertNotNull(ZonedDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME));

            if (appendLog) {
                assertEquals(
                        String.format("%s  %s", timestamp, unstampedLines.get(i + lower - 1)),
                        results.get(i));
            }
        }
    }

    private static void elapsed(
            Run<?, ?> build,
            List<String> unstampedLines,
            String pattern,
            int patternLength,
            int startLine,
            int endLine,
            boolean appendLog)
            throws IOException {
        List<String> results =
                getQueryResults(
                        build,
                        String.format(
                                "elapsed=%s%s%s%s",
                                pattern,
                                startLine != 0 ? String.format("&startLine=%s", startLine) : "",
                                endLine != 0 ? String.format("&endLine=%s", endLine) : "",
                                appendLog ? "&appendLog" : ""));
        int lower;
        if (startLine < 0) {
            lower = unstampedLines.size() + startLine + 1;
        } else if (startLine > 0) {
            lower = startLine;
        } else {
            lower = 1;
        }
        int upper;
        if (endLine < 0) {
            upper = unstampedLines.size() + endLine + 1;
        } else if (endLine > 0) {
            upper = endLine;
        } else {
            upper = unstampedLines.size();
        }
        assertEquals(
                IntStream.rangeClosed(lower, upper).count(),
                results.size());
        for (int i = 0; i < results.size(); i++) {
            if (appendLog) {
                assertTrue(results.get(i).length() >= patternLength);
            } else {
                assertEquals(patternLength, results.get(i).length());
            }

            String timestamp = results.get(i).substring(0, patternLength);
            assertNotNull(Duration.parse(timestamp));

            if (appendLog) {
                assertEquals(
                        String.format("%s  %s", timestamp, unstampedLines.get(i + lower - 1)),
                        results.get(i));
            }
        }
    }

    private static void currentTime(Run<?, ?> build, String pattern, String timezone)
            throws IOException {
        List<String> results =
                getQueryResults(
                        build, String.format("currentTime&time=%s&timeZone=%s", pattern, timezone));
        assertEquals(1, results.size());
        assertNotNull(ZonedDateTime.parse(results.get(0), DateTimeFormatter.ISO_DATE_TIME));
    }

    private static List<String> getQueryResults(Run<?, ?> build, String queryString)
            throws IOException {
        List<String> result;
        try (BufferedReader reader = TimestamperAPI.get().read(build, queryString)) {
            result = reader.lines().collect(Collectors.toList());
        }
        return result;
    }
}
