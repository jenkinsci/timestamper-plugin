/*
 * The MIT License
 * 
 * Copyright (c) 2013 Steven G. Brown
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
package hudson.plugins.timestamper.format;

import static com.google.common.base.Preconditions.checkNotNull;
import hudson.MarkupText;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.TimestamperConfig;

import java.util.Date;
import java.util.TimeZone;

import javax.annotation.Nonnull;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import jenkins.model.GlobalConfiguration;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;

/**
 * Formats a time-stamp to be displayed in the console log page, according to
 * the current settings.
 * 
 * @author Steven G. Brown
 */
public class TimestampFormatter {

  /**
   * This System property is used to configure the time zone. See the
   * "Change time zone" Jenkins wiki page.
   */
  private static final String TIME_ZONE_PROPERTY = "org.apache.commons.jelly.tags.fmt.timeZone";

  private static Supplier<TimestampFormatter> SUPPLIER = new Supplier<TimestampFormatter>() {
    @Override
    public TimestampFormatter get() {
      TimestamperConfig config = GlobalConfiguration.all().get(
          TimestamperConfig.class);
      // JENKINS-16778: The request can be null when the slave goes off-line.
      Optional<StaplerRequest> request = Optional.fromNullable(Stapler
          .getCurrentRequest());
      Optional<String> timeZoneId = Optional.fromNullable(System
          .getProperty(TIME_ZONE_PROPERTY));
      return new TimestampFormatter(config.getSystemTimeFormat(),
          config.getElapsedTimeFormat(), request, timeZoneId);
    }
  };

  public static TimestampFormatter get() {
    return SUPPLIER.get();
  }

  /**
   * Function that converts a time-stamp to a formatted string representation of
   * that time-stamp.
   */
  private final Function<Timestamp, String> formatTimestamp;

  /**
   * Create a new {@link TimestampFormatter}.
   * 
   * @param systemTimeFormat
   *          the system clock time format
   * @param elapsedTimeFormat
   *          the elapsed time format
   * @param request
   *          the current HTTP request
   * @param timeZoneId
   *          the currently configured time zone
   */
  TimestampFormatter(String systemTimeFormat, String elapsedTimeFormat,
      Optional<? extends HttpServletRequest> request,
      Optional<String> timeZoneId) {

    String cookieValue = null;
    String offset = null;
    if (request.isPresent()) {
      Cookie[] cookies = request.get().getCookies();
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if ("jenkins-timestamper".equals(cookie.getName())) {
            cookieValue = cookie.getValue();
            break;
          }
        }

        for (Cookie cookie : cookies) {
          if ("jenkins-timestamper-offset".equals(cookie.getName())) {
            offset = cookie.getValue();
            break;
          }
        }
      }
    }

    if ("elapsed".equalsIgnoreCase(cookieValue)) {
      formatTimestamp = new ElapsedTimeFormatFunction(elapsedTimeFormat);
    } else if ("none".equalsIgnoreCase(cookieValue)) {
      formatTimestamp = new EmptyFormatFunction();
    } else if ("local".equalsIgnoreCase(cookieValue)) {
      if (offset == null) {
        offset = "0";
      }
      Optional<String> localTimeZoneId = adaptTimeZoneId(offset);
      formatTimestamp = new SystemTimeFormatFunction(systemTimeFormat,
          localTimeZoneId);
    } else {
      formatTimestamp = new SystemTimeFormatFunction(systemTimeFormat,
          timeZoneId);
    }
  }

  private Optional<String> adaptTimeZoneId(String offset) {
    String[] timeZones = TimeZone.getAvailableIDs(Integer.parseInt(offset)
        * (-1));
    Optional<String> timeZoneId = Optional.of(timeZones[0]);
    return timeZoneId;
  }

  /**
   * Format the given time-stamp and add it to the mark-up text.
   * 
   * @param text
   *          the mark-up text
   * @param timestamp
   *          the time-stamp to format
   */
  public void markup(MarkupText text, Timestamp timestamp) {
    String timestampString = formatTimestamp.apply(timestamp);
    // Wrap the time-stamp in a span element, which is used to detect the
    // time-stamp when inspecting the page with Javascript.
    String markup = "<span class=\"timestamp\">" + timestampString + "</span>";
    // Add as end tag, which will be inserted prior to tags added by other
    // console notes (e.g. AntTargetNote).
    text.addMarkup(0, 0, "", markup);
  }

  /**
   * Function that converts a time-stamp to the system clock time format.
   */
  private static class SystemTimeFormatFunction implements
      Function<Timestamp, String> {

    private final FastDateFormat format;

    SystemTimeFormatFunction(String systemTimeFormat,
        Optional<String> timeZoneId) {
      TimeZone timeZone = null;
      if (timeZoneId.isPresent()) {
        timeZone = TimeZone.getTimeZone(timeZoneId.get());
      }
      this.format = FastDateFormat.getInstance(systemTimeFormat, timeZone);
    }

    @Override
    public String apply(@Nonnull Timestamp timestamp) {
      return format.format(new Date(timestamp.millisSinceEpoch));
    }
  }

  /**
   * Function that converts a time-stamp to the elapsed time format.
   */
  private static class ElapsedTimeFormatFunction implements
      Function<Timestamp, String> {

    private final String elapsedTimeFormat;

    ElapsedTimeFormatFunction(String elapsedTimeFormat) {
      this.elapsedTimeFormat = checkNotNull(elapsedTimeFormat);
    }

    @Override
    public String apply(@Nonnull Timestamp timestamp) {
      return DurationFormatUtils.formatDuration(timestamp.elapsedMillis,
          elapsedTimeFormat);
    }
  }

  /**
   * Function that converts any time-stamp to an empty string.
   */
  private static class EmptyFormatFunction implements
      Function<Timestamp, String> {

    EmptyFormatFunction() {
    }

    @Override
    public String apply(@Nonnull Timestamp timestamp) {
      return "";
    }
  }
}
