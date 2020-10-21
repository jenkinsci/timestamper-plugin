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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.Util;
import hudson.plugins.timestamper.format.ElapsedTimestampFormat;
import hudson.plugins.timestamper.format.FormatParseException;
import hudson.plugins.timestamper.format.InvalidHtmlException;
import hudson.plugins.timestamper.format.SystemTimestampFormat;
import hudson.plugins.timestamper.format.TimestampFormat;
import hudson.plugins.timestamper.pipeline.GlobalDecorator;
import hudson.util.FormValidation;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import javax.servlet.ServletException;
import jenkins.YesNoMaybe;
import jenkins.model.GlobalConfiguration;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.QueryParameter;

/**
 * Global configuration for the Timestamper plug-in, as shown on the Jenkins Configure System page.
 *
 * @author Frederik Fromm
 */
@Extension(dynamicLoadable = YesNoMaybe.YES)
@Symbol({"timestamper", "timestamperConfig"})
public final class TimestamperConfig extends GlobalConfiguration {

  /**
   * Get the current Timestamper global configuration.
   *
   * @return the Timestamper configuration, or {@code null} if Jenkins has been shut down
   */
  public static TimestamperConfig get() {
    return ExtensionList.lookupSingleton(TimestamperConfig.class);
  }

  /** The default {@link #timestampFormat}. */
  private static final String DEFAULT_TIMESTAMP_FORMAT = "'<b>'HH:mm:ss'</b> '";

  /** The default {@link #elapsedTimeFormat}. */
  private static final String DEFAULT_ELAPSED_TIME_FORMAT = "'<b>'HH:mm:ss.S'</b> '";

  /**
   * The chosen format for displaying the system clock time, as recognised by {@link
   * SimpleDateFormat}.
   */
  @CheckForNull private String timestampFormat;

  /**
   * The chosen format for displaying the elapsed time, as recognised by {@link
   * DurationFormatUtils}.
   */
  @CheckForNull private String elapsedTimeFormat;

  /**
   * Whether to activate {@link GlobalDecorator}.
   */
  private boolean allPipelines;

  /** Constructor. */
  public TimestamperConfig() {
    load();
  }

  /**
   * Get the format for displaying the system clock time.
   *
   * @return the system clock time format
   */
  public String getSystemTimeFormat() {
    return timestampFormat == null ? DEFAULT_TIMESTAMP_FORMAT : timestampFormat;
  }

  /**
   * Set the format for displaying the system clock time.
   *
   * @param timestampFormat the system clock time format in {@link SimpleDateFormat} pattern
   */
  public void setSystemTimeFormat(@CheckForNull String timestampFormat) {
    this.timestampFormat = Util.fixEmptyAndTrim(timestampFormat);
    save();
  }

  public FormValidation doCheckSystemTimeFormat(@QueryParameter String systemTimeFormat)
      throws IOException, ServletException {
    if (Util.fixEmptyAndTrim(systemTimeFormat) == null) {
      return FormValidation.ok();
    }

    return validateFormat(
        () -> new SystemTimestampFormat(systemTimeFormat, Optional.empty(), Locale.getDefault()));
  }

  /**
   * Get the format for displaying the elapsed time.
   *
   * @return the elapsed time format
   */
  public String getElapsedTimeFormat() {
    return elapsedTimeFormat == null ? DEFAULT_ELAPSED_TIME_FORMAT : elapsedTimeFormat;
  }

  /**
   * Set the format for displaying the elapsed time.
   *
   * @param elapsedTimeFormat the elapsed time format in {@link DurationFormatUtils} pattern
   */
  public void setElapsedTimeFormat(@CheckForNull String elapsedTimeFormat) {
    this.elapsedTimeFormat = Util.fixEmptyAndTrim(elapsedTimeFormat);
    save();
  }

  public FormValidation doCheckElapsedTimeFormat(@QueryParameter String elapsedTimeFormat)
      throws IOException, ServletException {
    if (Util.fixEmptyAndTrim(elapsedTimeFormat) == null) {
      return FormValidation.ok();
    }

    return validateFormat(() -> new ElapsedTimestampFormat(elapsedTimeFormat));
  }

  /** Validates the given input using the given {@link TimestampFormat}. */
  private static FormValidation validateFormat(@NonNull Supplier<TimestampFormat> formatSupplier) {
    try {
      TimestampFormat format = formatSupplier.get();
      format.validate();
      return FormValidation.ok();
    } catch (FormatParseException e) {
      return FormValidation.error("Error parsing format");
    } catch (InvalidHtmlException e) {
      return FormValidation.error("Invalid HTML");
    }
  }

  public boolean isAllPipelines() {
    return allPipelines;
  }

  public void setAllPipelines(boolean allPipelines) {
    this.allPipelines = allPipelines;
    save();
  }

}
