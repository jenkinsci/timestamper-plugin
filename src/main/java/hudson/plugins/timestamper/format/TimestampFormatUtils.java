package hudson.plugins.timestamper.format;

import edu.umd.cs.findbugs.annotations.NonNull;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.owasp.html.HtmlSanitizer;
import org.owasp.html.HtmlStreamRenderer;
import org.owasp.html.Sanitizers;

import java.util.logging.Level;
import java.util.logging.Logger;

@Restricted(NoExternalUse.class)
public class TimestampFormatUtils {

    private static final Logger LOGGER = Logger.getLogger(TimestampFormatUtils.class.getName());

    public static String sanitize(@NonNull String input) {
        StringBuilder sb = new StringBuilder();
        HtmlStreamRenderer renderer = HtmlStreamRenderer.create(sb, TimestampFormatUtils::handle);
        HtmlSanitizer.sanitize(input, Sanitizers.FORMATTING.apply(renderer));
        return sb.toString();
    }

    private static void handle(@NonNull String html) {
        LOGGER.log(Level.WARNING, "Invalid HTML: " + html);
    }
}
