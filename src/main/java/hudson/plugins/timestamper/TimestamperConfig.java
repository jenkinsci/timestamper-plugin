/*
 * The MIT License
 * 
 * Copyright (c) 2012 Frederik Fromm
 * Copyright (c) 2012 Steven G. Brown
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.timestamper;

import hudson.Extension;
import hudson.model.Descriptor;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Global configuration for the Timestamper plugin, as shown on the Jenkins
 * Configure System page.
 * 
 * @author Frederik Fromm
 */
@Extension
public class TimestamperConfig extends GlobalConfiguration {

  /**
   * The default time-stamp format.
   */
  private static final String DEFAULT_TIMESTAMP_FORMAT = "'<b>'HH:mm:ss'</b> '";

  /**
   * The chosen time-stamp format, as recognised by SimpleDateFormat.
   */
  private String timestampFormat;

  /**
   * Constructor
   */
  public TimestamperConfig() {
    load();
  }

  /**
   * Returns the timestamp format.
   * 
   * @return the timestamp format
   */
  public String getTimestampFormat() {
    return StringUtils.isEmpty(timestampFormat) ? DEFAULT_TIMESTAMP_FORMAT
        : this.timestampFormat;
  }

  /**
   * Sets the timestamp format. If null, the default timestamp format is used.
   * 
   * @param timestampFormat
   *          the timestamp format in SimpleDateFormat pattern.
   */
  public void setTimestampFormat(String timestampFormat) {
    this.timestampFormat = StringUtils.isEmpty(timestampFormat) ? DEFAULT_TIMESTAMP_FORMAT
        : timestampFormat;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean configure(StaplerRequest req, JSONObject json)
      throws Descriptor.FormException {
    req.bindJSON(this, json);
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
