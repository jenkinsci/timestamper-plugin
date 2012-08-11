package hudson.plugins.timestamper;

import hudson.Extension;
import hudson.model.Descriptor;
import jenkins.model.GlobalConfiguration;
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
     * default for timestamp format
     */
    public static final String DEFAULT_TIMESTAMP_FORMAT="'<b>'HH:mm:ss'</b> '";

    /**
     * Constructor
     */
    public TimestamperConfig() {
        load();
    }

    /**
     * Returns the timestamp format.
     * @return the timestamp format
     */
    public String getTimestampFormat() {
        return StringUtils.isEmpty(timestampFormat) ? DEFAULT_TIMESTAMP_FORMAT : this.timestampFormat;
    }

    /**
     * Sets the timestamp format. If null, the default timestamp format is used.
     * @param timestampFormat the timestamp format in SimpleDateFormat pattern.
     */
    public void setTimestampFormat(String timestampFormat) {
        this.timestampFormat = StringUtils.isEmpty(timestampFormat) ? DEFAULT_TIMESTAMP_FORMAT : timestampFormat;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws Descriptor.FormException {
        req.bindJSON(this,json);
        save();
        return true;
    }

    /**
     * Get the currently configured global Timestamper settings.
     * 
     * @return the Timestamper global config
     */
    public static TimestamperConfig get() {
        return GlobalConfiguration.all().get(TimestamperConfig.class);
    }
}
