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

import java.io.Serializable;
import java.util.Date;
import java.util.TimeZone;

import javax.annotation.concurrent.Immutable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.lang.time.FastDateFormat;

import com.google.common.base.Function;
import com.google.common.base.Optional;

/**
 * Formats a time-stamp to be displayed in the console log page, according to
 * the current settings.
 * 
 * @author Steven G. Brown
 */
@Immutable
public final class TimestampFormatterImpl implements TimestampFormatter {

  private static final long serialVersionUID = 1L;

  /**
   * Function that converts a time-stamp to a formatted string representation of
   * that time-stamp.
   */
  private final Function<Timestamp, String> formatTimestamp;

  /**
   * Create a new {@link TimestampFormatterImpl}.
   * 
   * @param systemTimeFormat
   *          the system clock time format
   * @param elapsedTimeFormat
   *          the elapsed time format
   * @param timeZoneId
   *          the configured time zone identifier
   * @param request
   *          the current HTTP request
   */
  public TimestampFormatterImpl(String systemTimeFormat,
      String elapsedTimeFormat, Optional<String> timeZoneId,
      HttpServletRequest request) {

    String cookieValue = null;
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if ("jenkins-timestamper".equals(cookie.getName())) {
          cookieValue = cookie.getValue();
          break;
        }
      }
    }

    if ("elapsed".equalsIgnoreCase(cookieValue)) {
      formatTimestamp = new ElapsedTimeFormatFunction(elapsedTimeFormat);
    } else if ("none".equalsIgnoreCase(cookieValue)) {
      formatTimestamp = new EmptyFormatFunction();
    } else {
      // "system", no cookie, or unrecognised cookie
      TimeZone timeZone = null;
      if (timeZoneId.isPresent()) {
        timeZone = TimeZone.getTimeZone(timeZoneId.get());
      }
      FastDateFormat format = FastDateFormat.getInstance(systemTimeFormat,
          timeZone);
      formatTimestamp = new SystemTimeFormatFunction(format);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
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
      Function<Timestamp, String>, Serializable {

    private static final long serialVersionUID = 1L;

    private final FastDateFormat format;

    SystemTimeFormatFunction(FastDateFormat format) {
      this.format = checkNotNull(format);
    }

    @Override
    public String apply(Timestamp timestamp) {
      return format.format(new Date(timestamp.millisSinceEpoch));
    }
  }

  /**
   * Function that converts a time-stamp to the elapsed time format.
   */
  private static class ElapsedTimeFormatFunction implements
      Function<Timestamp, String>, Serializable {

    private static final long serialVersionUID = 1L;

    private final String elapsedTimeFormat;

    ElapsedTimeFormatFunction(String elapsedTimeFormat) {
      this.elapsedTimeFormat = checkNotNull(elapsedTimeFormat);
    }

    @Override
    public String apply(Timestamp timestamp) {
      return DurationFormatUtils.formatDuration(timestamp.elapsedMillis,
          elapsedTimeFormat);
    }
  }

  /**
   * Function that converts any time-stamp to an empty string.
   */
  private static class EmptyFormatFunction implements
      Function<Timestamp, String>, Serializable {

    private static final long serialVersionUID = 1L;

    EmptyFormatFunction() {
    }

    @Override
    public String apply(Timestamp timestamp) {
      return "";
    }
  }
}
