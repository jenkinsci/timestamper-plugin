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
import jakarta.servlet.http.Cookie;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest2;

/**
 * Provides a {@link TimestampFormat} based on the current settings.
 *
 * @author Steven G. Brown
 */
public class TimestampFormatProvider {

    private static Supplier<TimestampFormat> SUPPLIER = () -> {
        TimestamperConfig config = TimestamperConfig.get();
        StaplerRequest2 request = Stapler.getCurrentRequest2();
        if (request == null) {
            return EmptyTimestampFormat.INSTANCE;
        }
        return TimestampFormatProvider.get(
                config.getSystemTimeFormat(), config.getElapsedTimeFormat(), request, Locale.getDefault());
    };

    /**
     * Get the currently selected time-stamp format.
     *
     * @return the time-stamp format
     */
    public static TimestampFormat get() {
        return SUPPLIER.get();
    }

    static TimestampFormat get(
            String systemTimeFormat, String elapsedTimeFormat, StaplerRequest2 request, Locale locale) {

        String mode = null;
        Boolean local = null;
        String offset = null;

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (mode == null && "jenkins-timestamper".equals(cookie.getName())) {
                    mode = cookie.getValue();
                }

                if (local == null && "jenkins-timestamper-local".equals(cookie.getName())) {
                    local = Boolean.valueOf(cookie.getValue());
                }

                if (offset == null && "jenkins-timestamper-offset".equals(cookie.getName())) {
                    offset = cookie.getValue();
                }
            }
        }

        if ("elapsed".equalsIgnoreCase(mode)) {
            return new ElapsedTimestampFormat(elapsedTimeFormat);
        } else if ("none".equalsIgnoreCase(mode)) {
            return EmptyTimestampFormat.INSTANCE;
        } else {
            // "system", no mode cookie, or unrecognised mode cookie
            Optional<String> timeZoneId = Optional.empty();
            if (local != null && local) {
                try {
                    String localTimeZoneId = convertOffsetToTimeZoneId(offset);
                    timeZoneId = Optional.of(localTimeZoneId);
                } catch (NumberFormatException e) {
                    return EmptyTimestampFormat.INSTANCE;
                }
            }
            return new SystemTimestampFormat(systemTimeFormat, timeZoneId, locale);
        }
    }

    private static String convertOffsetToTimeZoneId(String offset) {
        // Reverse sign due to return value of the Date.getTimezoneOffset function.
        long offsetInMillis = -Integer.parseInt(offset);
        return TimeZoneUtils.getTimeZoneId(offsetInMillis);
    }
}
