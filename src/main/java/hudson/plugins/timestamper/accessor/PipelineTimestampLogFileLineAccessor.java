package hudson.plugins.timestamper.accessor;

import static com.google.common.base.Preconditions.checkNotNull;

import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.io.LogFileReader;
import hudson.plugins.timestamper.pipeline.GlobalAnnotator;
import hudson.plugins.timestamper.pipeline.GlobalDecorator;
import java.io.IOException;
import java.util.Optional;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Implementation of {@link TimestampLogFileLineAccessor} for Pipeline jobs. In contrast to
 * Freestyle jobs, Pipeline jobs do not use a separate timestamps file. Rather, timestamps are
 * embedded into the main log file in {@link GlobalDecorator} and annotated dynamically when the
 * console log is viewed in {@link GlobalAnnotator}.
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
        // Singleton arrays for use with the below lambda expression
        Timestamp[] timestampRef = new Timestamp[1];
        String[] logFileLineRef = new String[1];

        logFileReader
                .nextLine()
                .ifPresent(
                        logFileLine -> {
                            Optional<Timestamp> timestamp =
                                    GlobalAnnotator.parseTimestamp(
                                            logFileLine, 0, build.getStartTimeInMillis());
                            if (timestamp.isPresent()) {
                                timestampRef[0] = timestamp.get();
                                logFileLineRef[0] = logFileLine.substring(27);
                            } else {
                                logFileLineRef[0] = logFileLine;
                            }
                        });

        return new TimestampLogFileLine(
                Optional.ofNullable(timestampRef[0]), Optional.ofNullable(logFileLineRef[0]));
    }

    @Override
    public void close() {
        logFileReader.close();
    }
}
