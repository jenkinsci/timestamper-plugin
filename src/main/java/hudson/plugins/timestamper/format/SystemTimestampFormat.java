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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.plugins.timestamper.Timestamp;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.time.FastDateFormat;

/**
 * Converts a time-stamp to the system clock time format.
 *
 * @author Steven G. Brown
 */
public final class SystemTimestampFormat extends TimestampFormat {

    /**
     * This System property is used to configure the time zone. See the "Change time zone" Jenkins
     * wiki page.
     */
    private static final String TIME_ZONE_PROPERTY = "org.apache.commons.jelly.tags.fmt.timeZone";

    private final FastDateFormat format;

    private final Optional<String> timeZoneId;

    public SystemTimestampFormat(String systemTimeFormat, Optional<String> timeZoneId, Locale locale) {
        TimeZone timeZone = null;
        if (timeZoneId.isPresent()) {
            timeZone = TimeZone.getTimeZone(timeZoneId.get());
        } else {
            String timeZoneProperty = System.getProperty(TIME_ZONE_PROPERTY);
            if (timeZoneProperty != null) {
                timeZone = TimeZone.getTimeZone(timeZoneProperty);
            }
        }
        try {
            this.format = FastDateFormat.getInstance(systemTimeFormat, timeZone, locale);
        } catch (IllegalArgumentException e) {
            throw new FormatParseException(e);
        }
        this.timeZoneId = timeZoneId;
    }

    /** {@inheritDoc} */
    @Override
    public String apply(@NonNull Timestamp timestamp) {
        String result = format.format(new Date(timestamp.millisSinceEpoch));
        return TimestampFormatUtils.sanitize(result);
    }

    @Override
    public void validate() throws FormatParseException, InvalidHtmlException {
        String result;
        try {
            result = format.format(new Date(0L));
        } catch (IllegalArgumentException e) {
            throw new FormatParseException(e);
        }

        String sanitized = TimestampFormatUtils.sanitize(result);
        if (!sanitized.equals(result)) {
            throw new InvalidHtmlException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getPlainTextUrl() {
        String timeParamValue = format.getPattern();
        timeParamValue = FormatStringUtils.stripHtmlTags(timeParamValue);
        timeParamValue = FormatStringUtils.trim(timeParamValue);

        return "timestamps/?time="
                + timeParamValue
                + timeZoneId.map(s -> "&timeZone=" + s).orElse("")
                + "&appendLog"
                + "&locale="
                + format.getLocale();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(format, timeZoneId);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SystemTimestampFormat) {
            SystemTimestampFormat other = (SystemTimestampFormat) obj;
            return format.equals(other.format) && timeZoneId.equals(other.timeZoneId);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("format", format.getPattern())
                .append("timeZoneId", timeZoneId)
                .append("locale", format.getLocale())
                .toString();
    }
}
