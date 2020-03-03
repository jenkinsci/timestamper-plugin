package hudson.plugins.timestamper.format;

/** Exception indicating that a given timestamp format string contained invalid HTML. */
public class InvalidHtmlException extends RuntimeException {

    public InvalidHtmlException() {}

    public InvalidHtmlException(String message) {
        super(message);
    }

    public InvalidHtmlException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidHtmlException(Throwable cause) {
        super(cause);
    }
}
