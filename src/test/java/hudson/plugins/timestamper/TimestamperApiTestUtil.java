package hudson.plugins.timestamper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import hudson.model.Run;
import hudson.plugins.timestamper.action.TimestampsActionOutput;
import hudson.plugins.timestamper.action.TimestampsActionQuery;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class TimestamperApiTestUtil {

    public static void timestamperApi(Run<?, ?> build) throws IOException {
        List<String> unstampedLines = build.getLog(Integer.MAX_VALUE);

        time(build, unstampedLines, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 24, "UTC", false);
        time(build, unstampedLines, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", 24, "UTC", true);

        elapsed(build, unstampedLines, "'P'd'DT'H'H'm'M's.S'S'", 14, false);
        elapsed(build, unstampedLines, "'P'd'DT'H'H'm'M's.S'S'", 14, true);

        currentTime(build, "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "UTC");
    }

    private static void time(
            Run<?, ?> build,
            List<String> unstampedLines,
            String pattern,
            int patternLength,
            String timezone,
            boolean appendLog)
            throws IOException {
        List<String> results =
                getQueryResults(
                        build,
                        String.format(
                                "time=%s&timeZone=%s%s",
                                pattern, timezone, appendLog ? "&appendLog" : ""));
        assertEquals(unstampedLines.size(), results.size());
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
                        String.format("%s  %s", timestamp, unstampedLines.get(i)), results.get(i));
            }
        }
    }

    private static void elapsed(
            Run<?, ?> build,
            List<String> unstampedLines,
            String pattern,
            int patternLength,
            boolean appendLog)
            throws IOException {
        List<String> results =
                getQueryResults(
                        build,
                        String.format("elapsed=%s%s", pattern, appendLog ? "&appendLog" : ""));
        assertEquals(unstampedLines.size(), results.size());
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
                        String.format("%s  %s", timestamp, unstampedLines.get(i)), results.get(i));
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
        TimestampsActionQuery query = TimestampsActionQuery.create(queryString);
        List<String> result;
        try (BufferedReader reader = TimestampsActionOutput.open(build, query)) {
            result = reader.lines().collect(Collectors.toList());
        }
        return result;
    }
}
