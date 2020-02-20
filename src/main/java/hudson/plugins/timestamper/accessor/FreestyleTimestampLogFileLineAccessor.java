package hudson.plugins.timestamper.accessor;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.io.CountingInputStream;
import hudson.console.ConsoleNote;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.TimestampNote;
import hudson.plugins.timestamper.io.LogFileReader;
import hudson.plugins.timestamper.io.TimestampsReader;
import hudson.plugins.timestamper.io.TimestampsWriter;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Optional;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Implementation of {@link TimestampLogFileLineAccessor} for Freestyle jobs. In contrast to
 * Pipeline jobs, Freestyle jobs do not embed the timestamps in the main log file. Rather, they use
 * a separate timestamps file in {@link TimestampsWriter}, optionally adding a {@link ConsoleNote}
 * based on the settings in {@link TimestampNote}.
 */
@Restricted(NoExternalUse.class)
public class FreestyleTimestampLogFileLineAccessor implements TimestampLogFileLineAccessor {

    private final TimestampsReader timestampsReader;
    private final LogFileReader logFileReader;
    private final Run<?, ?> build;

    public FreestyleTimestampLogFileLineAccessor(
            TimestampsReader timestampsReader, LogFileReader logFileReader, Run<?, ?> build) {
        this.timestampsReader = checkNotNull(timestampsReader);
        this.logFileReader = checkNotNull(logFileReader);
        this.build = checkNotNull(build);
    }

    @Override
    public LogFileReader getLogFileReader() {
        return logFileReader;
    }

    @Override
    public void skipLine() throws IOException {
        timestampsReader.read();
        logFileReader.nextLine();
    }

    @Override
    public TimestampLogFileLine readLine() throws IOException {
        Optional<Timestamp> timestamp = timestampsReader.read();
        Optional<String> logFileLine = logFileReader.nextLine();

        // This implementation relies primarily on the timestamps embedded in the timstamps file,
        // falling back to looking for ConsoleNotes only if necessary.
        if (logFileLine.isPresent() && !timestamp.isPresent()) {
            timestamp = readTimestamp(logFileLine.get());
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

    @Override
    public void close() {
        timestampsReader.close();
        logFileReader.close();
    }
}
