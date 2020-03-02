package hudson.plugins.timestamper.accessor;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.io.CountingInputStream;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.console.ConsoleNote;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.TimestampNote;
import hudson.plugins.timestamper.action.TimestampsActionOutput;
import hudson.plugins.timestamper.io.TimestampsReader;
import hudson.plugins.timestamper.pipeline.GlobalAnnotator;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Abstraction for retrieving timestamps and log file lines from completed builds. Timestamp records
 * can be stored in different files and formats for different types of builds. Consumers that wish
 * to retrieve such records should use this interface rather than directly opening the corresponding
 * log file.
 */
@Restricted(NoExternalUse.class)
@SuppressFBWarnings(
        value = "RV_DONT_JUST_NULL_CHECK_READLINE",
        justification = "Line count lambda is just counting lines, not reading them")
public class TimestampLogFileLineAccessor implements Closeable {

    /** The build whose timestamps and log file lines we are accessing. */
    private final Run<?, ?> build;

    /**
     * A {@link BufferedReader} for the log file. Note that this may be backed by an external stream
     * as of JEP 210.
     */
    private final BufferedReader logFileReader;

    /**
     * A reader for the timestamps file. Note that the timestamps file is only present for Freestyle
     * builds of version 1.4 or later where the "timestamper-consolenotes" system property is
     * <em>not</em> set.
     */
    private final TimestampsReader timestampsReader;

    /**
     * A memoizing {@link Supplier} of the line count of the log file. Counting the lines of an
     * entire log file may be an expensive operation, especially if it is backed by an external
     * stream. Furthermore, in the common case (where negative numbers are not supplied as input to
     * startLine and endLine in {@link TimestampsActionOutput}, counting lines isn't even necessary
     * at all. Therefore, we only do this operation if we need to and cache the result once it is
     * done.
     */
    private final Supplier<Integer> lineCount;

    public TimestampLogFileLineAccessor(Run<?, ?> build) throws IOException {
        this.build = checkNotNull(build);
        this.logFileReader = new BufferedReader(build.getLogReader());
        this.timestampsReader = new TimestampsReader(build);
        this.lineCount =
                Suppliers.memoize(
                        () -> {
                            int lineCount = 0;
                            try (BufferedReader lineCountReader =
                                    new BufferedReader(build.getLogReader())) {
                                while (lineCountReader.readLine() != null) {
                                    lineCount++;
                                }
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                            return lineCount;
                        });
    }

    /** Skip forward one line in the associated record file(s). */
    public void skipLine() throws IOException {
        timestampsReader.read();
        logFileReader.readLine();
    }

    /**
     * Retrieve a log file line and its associated timestamp. While typically both a timestamp and a
     * log file line will be present, this API is resilient to edge cases in which one or the other
     * is not present. In such cases, consumers have a choice as to whether to discard the record or
     * return incomplete information to the user. When neither a timestamp nor a log file line are
     * present, EOF has been reached and callers should stop retrieving further records.
     */
    public TimestampLogFileLine readLine() throws IOException {
        String logFileLine = logFileReader.readLine();

        // Attempt to read the timestamp from the timestamps file, if present. This covers Freestyle
        // builds of version 1.4 or later where the "timestamper-consolenotes" system property was
        // _not_ set.
        Timestamp timestamp = timestampsReader.read().orElse(null);
        if (timestamp == null && logFileLine != null) {
            // If a timestamps file is not present, attempt to read the timestamp from the log file.
            // The log file is decorated with GlobalDecorator for Pipeline builds of version 1.9 or
            // later.
            timestamp =
                    GlobalAnnotator.parseTimestamp(logFileLine, build.getStartTimeInMillis())
                            .orElse(null);
            if (timestamp != null) {
                // If we succeeded, then the log file was decorated by GlobalDecorator. Strip the
                // timestamp decoration from the front of the line.
                logFileLine = logFileLine.substring(27);
            } else {
                // Attempt to read the timestamp from TimestampNotes embedded in the log file.
                // Such TimestampNotes are present for Pipeline builds prior to version 1.9 as well
                // as Freestyle builds prior to version 1.4 or where the
                // "timestamper-consolenotes" system property was set.
                timestamp = readTimestamp(logFileLine).orElse(null);
            }
        }

        return new TimestampLogFileLine(timestamp, logFileLine);
    }

    /**
     * Read the time-stamp from the {@link ConsoleNote} in this line, if present.
     *
     * @return the time-stamp
     */
    private Optional<Timestamp> readTimestamp(String line) {
        byte[] bytes = line.getBytes(build.getCharset());
        int length = bytes.length;

        int index = 0;
        while (true) {
            index = ConsoleNote.findPreamble(bytes, index, length - index);
            if (index == -1) {
                return Optional.empty();
            }
            CountingInputStream inputStream =
                    new CountingInputStream(new ByteArrayInputStream(bytes, index, length - index));

            try {
                ConsoleNote<?> consoleNote = ConsoleNote.readFrom(new DataInputStream(inputStream));
                if (consoleNote instanceof TimestampNote) {
                    TimestampNote timestampNote = (TimestampNote) consoleNote;
                    Timestamp timestamp = timestampNote.getTimestamp(build);
                    return Optional.of(timestamp);
                }
            } catch (IOException e) {
                // Error reading console note, e.g. end of stream. Ignore.
            } catch (ClassNotFoundException e) {
                // Unknown console note. Ignore.
            }

            // Advance at least one character to avoid an infinite loop.
            index += Math.max(inputStream.getCount(), 1);
        }
    }

    public int getLineCount() {
        return lineCount.get();
    }

    @Override
    public void close() throws IOException {
        timestampsReader.close();
        logFileReader.close();
    }
}
