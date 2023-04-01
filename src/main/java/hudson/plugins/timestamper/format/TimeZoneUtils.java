/*
 * The MIT License
 *
 * Copyright (c) 2016 Steven G. Brown
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

import java.util.concurrent.TimeUnit;

/**
 * Utility class for handling time zones.
 *
 * @author Steven G. Brown
 */
class TimeZoneUtils {

    /**
     * Get a time zone ID for the given offset.
     *
     * @param offset the offset in milliseconds
     * @return the time zone ID
     */
    static String getTimeZoneId(long offset) {
        long totalOffsetInMinutes = TimeUnit.MILLISECONDS.toMinutes(offset);
        String sign = totalOffsetInMinutes > 0 ? "+" : "-";
        long hours = Math.abs(totalOffsetInMinutes / 60);
        long minutes = Math.abs(totalOffsetInMinutes % 60);

        String timeZoneId = "GMT";
        if (hours > 0 || minutes > 0) {
            timeZoneId += sign + hours;
            if (minutes > 0) {
                timeZoneId += ":" + minutes;
            }
        }
        return timeZoneId;
    }

    private TimeZoneUtils() {}
}
