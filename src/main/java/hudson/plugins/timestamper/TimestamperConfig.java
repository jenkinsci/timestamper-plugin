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

import org.kohsuke.stapler.StaplerRequest;

import com.google.common.base.Objects;
import com.google.common.base.Supplier;

/**
 * Global configuration for the Timestamper plug-in, as shown on the Jenkins
 * Configure System page.
 * 
 * @author Frederik Fromm
 * @since 1.3
 */
@Extension
public class TimestamperConfig extends GlobalConfiguration implements Settings {

  private static Supplier<Settings> settingsSupplier = new Supplier<Settings>() {

    public Settings get() {
      return GlobalConfiguration.all().get(TimestamperConfig.class);
    }
  };

  /**
   * The default time-stamp format.
   */
  private static final String DEFAULT_TIMESTAMP_FORMAT = "'<b>'HH:mm:ss'</b> '";

  /**
   * The chosen time-stamp format, as recognised by SimpleDateFormat.
   */
  private String timestampFormat;

  /**
   * Constructor.
   */
  public TimestamperConfig() {
    load();
  }

  /**
   * Get the time-stamp format.
   * 
   * @return the time-stamp format
   */
  public String getTimestampFormat() {
    return Objects.firstNonNull(timestampFormat, DEFAULT_TIMESTAMP_FORMAT);
  }

  /**
   * Set the time-stamp format.
   * 
   * @param timestampFormat
   *          the time-stamp format in SimpleDateFormat pattern
   */
  public void setTimestampFormat(String timestampFormat) {
    this.timestampFormat = timestampFormat;
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
   * @return the Timestamper settings
   */
  public static Settings settings() {
    return settingsSupplier.get();
  }
}
