package hudson.plugins.timestamper.accessor;

import hudson.plugins.timestamper.Timestamp;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import java.util.Optional;

/**
 * Value class representing a single record in the abstract, regardless of how the storage of that
 * record is implemented.
 */
@Restricted(NoExternalUse.class)
public final class TimestampLogFileLine {
    private final Optional<Timestamp> timestamp;
    private final Optional<String> logFileLine;

    public TimestampLogFileLine(Timestamp timestamp, String logFileLine) {
        this.timestamp = Optional.ofNullable(timestamp);
        this.logFileLine = Optional.ofNullable(logFileLine);
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
