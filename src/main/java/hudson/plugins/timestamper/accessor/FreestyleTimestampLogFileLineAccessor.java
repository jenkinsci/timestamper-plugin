package hudson.plugins.timestamper.accessor;

import static com.google.common.base.Preconditions.checkNotNull;

import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.io.LogFileReader;
import hudson.plugins.timestamper.io.TimestampsReader;
import hudson.plugins.timestamper.io.TimestampsWriter;
import java.io.IOException;
import java.util.Optional;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Implementation of {@link TimestampLogFileLineAccessor} for Freestyle jobs. In contrast to
 * Pipeline jobs, Freestyle jobs do not embed the timestamps in the main log file. Rather, they use
 * a separate timestamps file in {@link TimestampsWriter}.
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
        return new TimestampLogFileLine(timestamp, logFileLine);
    }

    @Override
    public void close() {
        timestampsReader.close();
        logFileReader.close();
    }
}
