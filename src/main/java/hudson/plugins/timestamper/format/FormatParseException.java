package hudson.plugins.timestamper.format;

/** Exception indicating that a given timestamp format string could not be parsed. */
public class FormatParseException extends RuntimeException {

    public FormatParseException() {}

    public FormatParseException(String message) {
        super(message);
    }

    public FormatParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public FormatParseException(Throwable cause) {
        super(cause);
    }
}
