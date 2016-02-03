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

import hudson.plugins.timestamper.TimestamperConfig;

import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import jenkins.model.GlobalConfiguration;

import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;

/**
 * Provides a {@link TimestampFormat} based on the current settings.
 * 
 * @author Steven G. Brown
 */
public class TimestampFormatProvider {

  private static Supplier<TimestampFormat> SUPPLIER = new Supplier<TimestampFormat>() {
    @Override
    public TimestampFormat get() {
      TimestamperConfig config = GlobalConfiguration.all().get(
          TimestamperConfig.class);
      // JENKINS-16778: The request can be null when the slave goes off-line.
      Optional<StaplerRequest> request = Optional.fromNullable(Stapler
          .getCurrentRequest());
      return TimestampFormatProvider.get(config.getSystemTimeFormat(),
          config.getElapsedTimeFormat(), request);
    }
  };

  /**
   * Get the currently selected time-stamp format.
   * 
   * @return the time-stamp format
   */
  public static TimestampFormat get() {
    return SUPPLIER.get();
  }

  static TimestampFormat get(String systemTimeFormat, String elapsedTimeFormat,
      Optional<? extends HttpServletRequest> request) {

    String mode = null;
    Boolean local = null;
    String offset = null;
    if (request.isPresent()) {
      Cookie[] cookies = request.get().getCookies();
      if (cookies != null) {
        for (Cookie cookie : cookies) {
          if (mode == null && "jenkins-timestamper".equals(cookie.getName())) {
            mode = cookie.getValue();
          }

          if (local == null
              && "jenkins-timestamper-local".equals(cookie.getName())) {
            local = Boolean.valueOf(cookie.getValue());
          }

          if (offset == null
              && "jenkins-timestamper-offset".equals(cookie.getName())) {
            offset = cookie.getValue();
          }
        }
      }
    }

    if ("elapsed".equalsIgnoreCase(mode)) {
      return new ElapsedTimestampFormat(elapsedTimeFormat);
    } else if ("none".equalsIgnoreCase(mode)) {
      return EmptyTimestampFormat.INSTANCE;
    } else {
      // "system", no mode cookie, or unrecognised mode cookie
      Optional<String> timeZoneId = Optional.absent();
      if (local != null && local.booleanValue()) {
        if (offset == null) {
          offset = "0";
        }
        String localTimeZoneId = convertOffsetToTimeZoneId(offset);
        timeZoneId = Optional.of(localTimeZoneId);
      }
      return new SystemTimestampFormat(systemTimeFormat, timeZoneId);
    }
  }

  private static String convertOffsetToTimeZoneId(String offset) {
    long minutes = TimeUnit.MILLISECONDS.toMinutes(Integer.parseInt(offset));
    // Reverse sign due to return value of the Date.getTimezoneOffset function.
    String sign = minutes > 0 ? "-" : "+";
    return String.format("GMT%s%02d:%02d", sign, Math.abs(minutes / 60),
        Math.abs(minutes % 60));
  }
}
