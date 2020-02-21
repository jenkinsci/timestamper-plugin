package hudson.plugins.timestamper.accessor;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.io.CountingInputStream;
import hudson.console.ConsoleNote;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.TimestampNote;
import hudson.plugins.timestamper.io.LogFileReader;
import hudson.plugins.timestamper.pipeline.GlobalAnnotator;
import hudson.plugins.timestamper.pipeline.GlobalDecorator;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Implementation of {@link TimestampLogFileLineAccessor} for Pipeline jobs as well as Freestyle
 * jobs that were annotated with the "timestamper-consolenotes" property set (or Freestyle jobs
 * prior to version 1.4 of this plugin). In contrast to Freestyle jobs, Pipeline jobs do not use a
 * separate timestamps file. Rather, timestamps are embedded into the main log file in {@link
 * GlobalDecorator} and annotated dynamically when the console log is viewed in {@link
 * GlobalAnnotator}.
 */
@Restricted(NoExternalUse.class)
public class PipelineTimestampLogFileLineAccessor implements TimestampLogFileLineAccessor {

    private final LogFileReader logFileReader;
    private final Run<?, ?> build;

    public PipelineTimestampLogFileLineAccessor(LogFileReader logFileReader, Run<?, ?> build) {
        this.logFileReader = checkNotNull(logFileReader);
        this.build = checkNotNull(build);
    }

    @Override
    public LogFileReader getLogFileReader() {
        return logFileReader;
    }

    @Override
    public void skipLine() throws IOException {
        logFileReader.nextLine();
    }

    @Override
    public TimestampLogFileLine readLine() throws IOException {
        // AtomicReference for use with the below lambda expression
        AtomicReference<Timestamp> timestampRef = new AtomicReference<>();
        AtomicReference<String> logFileLineRef = new AtomicReference<>();

        logFileReader
                .nextLine()
                .ifPresent(
                        logFileLine -> {
                            Optional<Timestamp> timestamp =
                                    GlobalAnnotator.parseTimestamp(
                                            logFileLine, 0, build.getStartTimeInMillis());
                            if (timestamp.isPresent()) {
                                timestampRef.set(timestamp.get());
                                logFileLineRef.set(logFileLine.substring(27));
                            } else {
                                // Pipeline jobs prior to version 1.9 of this plugin (as well as
                                // Freestyle jobs prior to version 1.4 of this plugin, or when the
                                // "timestamper-consolenotes" system property was enabled) used
                                // TimestampNote rather than GlobalDecorator.
                                readTimestamp(logFileLine).ifPresent(timestampRef::set);
                                logFileLineRef.set(logFileLine);
                            }
                        });

        return new TimestampLogFileLine(
                Optional.ofNullable(timestampRef.get()), Optional.ofNullable(logFileLineRef.get()));
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
        logFileReader.close();
    }
}
