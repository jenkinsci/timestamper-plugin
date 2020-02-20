package hudson.plugins.timestamper.accessor;

import static com.google.common.base.Preconditions.checkNotNull;

import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.action.TimestampsActionOutput;
import hudson.plugins.timestamper.io.LogFileReader;
import java.io.Closeable;
import java.io.IOException;
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
public interface TimestampLogFileLineAccessor extends Closeable {

    /**
     * Value class representing a single record in the abstract, regardless of how the storage of
     * that record is implemented.
     */
    final class TimestampLogFileLine {
        private final Optional<Timestamp> timestamp;
        private final Optional<String> logFileLine;

        public TimestampLogFileLine(Optional<Timestamp> timestamp, Optional<String> logFileLine) {
            this.timestamp = checkNotNull(timestamp);
            this.logFileLine = checkNotNull(logFileLine);
        }

        /** Return the timestamp associated with the record, if present. */
        public Optional<Timestamp> getTimestamp() {
            return timestamp;
        }

        /**
         * Return the log file line associated with the record, if present. This log file line may
         * contain console notes. It is the caller's responsibility to remove any console notes if
         * desired.
         */
        public Optional<String> getLogFileLine() {
            return logFileLine;
        }
    }

    /**
     * Retrieve the {@link LogFileReader} associated with this accessor, likely for use with {@code
     * TimestampsActionOutput.LineCountSupplier}.
     */
    LogFileReader getLogFileReader();

    /** Skip forward one line in the associated record file(s). */
    void skipLine() throws IOException;

    /**
     * Retrieve a log file line and its associated timestamp. While typically both a timestamp and a
     * log file line will be present, this API is resilient to edge cases in which one or the other
     * is not present. When neither a timestamp nor a log file line are present, EOF has been
     * reached and callers should stop retrieving further records.
     */
    TimestampLogFileLine readLine() throws IOException;
}
