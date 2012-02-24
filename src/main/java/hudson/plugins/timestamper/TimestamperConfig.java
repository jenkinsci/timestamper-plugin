package hudson.plugins.timestamper;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Represents the config class to manage timestamp format and line prefix in Jenkins Configure System page.
 *
 * @author Frederik Fromm
 */
@Extension
public class TimestamperConfig extends GlobalConfiguration {

    /**
     * timestamp format in SimpleDateFormat pattern.
     */
    private String timestampFormat;

    /**
     * line prefix containing one {0} that gets the formatted timestamp.
     */
    private String linePrefix;

    /**
     * default for timestamp format
     */
    public static final String DEFAULT_TIMESTAMP_FORMAT="yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * default for line prefix
     */
    public static final String DEFAULT_LINE_PREFIX="{0} | ";

    /**
     * Constructor
     */
    public TimestamperConfig() {
        // unit test has no Jenkins in this case
        if(Jenkins.getInstance() != null) {
            load();
        }
    }

    /**
     * Returns the timestamp format.
     * @return the timestamp format
     */
    public String getTimestampFormat() {
        return timestampFormat;
    }

    /**
     * Sets the timestamp format. If null, the default timestamp format is used.
     * @param timestampFormat the timestamp format in SimpleDateFormat pattern.
     */
    public void setTimestampFormat(String timestampFormat) {
        this.timestampFormat = StringUtils.isEmpty(timestampFormat) ? DEFAULT_TIMESTAMP_FORMAT : timestampFormat;

    }

    /**
     * Returns the line prefix containing {0} to render the formatted timestamp.
     * @return the line prefix
     */
    public String getLinePrefix() {
        return linePrefix;
    }

    /**
     * Sets the line prefix. One {0} must be used to render the formatted timestamp.
     * @param linePrefix the line prefix containing {0}
     */
    public void setLinePrefix(String linePrefix) {
        this.linePrefix = StringUtils.isEmpty(linePrefix) ? DEFAULT_LINE_PREFIX : linePrefix;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this,json);
        save();
        return true;
    }

    /**
     * Returns the Timestamper config instance if Jenkins is present. For UnitTests a MOCK is returned.
     * @return the Timestamper config instance.
     */
    public static TimestamperConfig get() {
        if(Jenkins.getInstance() != null && GlobalConfiguration.all() != null) {
            return GlobalConfiguration.all().get(TimestamperConfig.class);
        }

        // MOCK for testing...
        TimestamperConfig config = new TimestamperConfig();
        config.setLinePrefix(DEFAULT_LINE_PREFIX);
        config.setTimestampFormat(DEFAULT_TIMESTAMP_FORMAT);

        return config;
    }
}
