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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import hudson.plugins.timestamper.Timestamp;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test for the {@link SystemTimestampFormat} class.
 *
 * @author Steven G. Brown
 */
class SystemTimestampFormatTest {

    // cf. hudson.plugins.timestamper.format.SystemTimestampFormat.TIME_ZONE_PROPERTY
    private static final String TIME_ZONE_PROPERTY = "org.apache.commons.jelly.tags.fmt.timeZone";

    private TimeZone systemDefaultTimeZone;

    @BeforeEach
    void setUp() {
        systemDefaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        System.clearProperty(TIME_ZONE_PROPERTY);
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(systemDefaultTimeZone);
        System.clearProperty(TIME_ZONE_PROPERTY);
    }

    @Test
    void testApply() {
        String systemTimeFormat = "HH:mm:ss";
        Timestamp timestamp = new Timestamp(123, 42000);
        assertThat(
                new SystemTimestampFormat(systemTimeFormat, Optional.empty(), Locale.ENGLISH).apply(timestamp),
                is("00:00:42"));
    }

    @Test
    void testApply_withDifferentTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+1"));

        String systemTimeFormat = "HH:mm:ss";
        Timestamp timestamp = new Timestamp(123, 42000);
        assertThat(
                new SystemTimestampFormat(systemTimeFormat, Optional.empty(), Locale.ENGLISH).apply(timestamp),
                is("01:00:42"));
    }

    @Test
    void testApply_withSystemProperty() {
        System.setProperty(TIME_ZONE_PROPERTY, "GMT+2");

        String systemTimeFormat = "HH:mm:ss";
        Timestamp timestamp = new Timestamp(123, 42000);
        assertThat(
                new SystemTimestampFormat(systemTimeFormat, Optional.empty(), Locale.ENGLISH).apply(timestamp),
                is("02:00:42"));
    }

    @Test
    void testApply_withProvidedTimeZone() {
        String systemTimeFormat = "HH:mm:ss";
        Timestamp timestamp = new Timestamp(123, 42000);
        assertThat(
                new SystemTimestampFormat(systemTimeFormat, Optional.of("GMT+3"), Locale.ENGLISH).apply(timestamp),
                is("03:00:42"));
    }

    @Test
    void testApply_withSystemPropertyAndProvidedTimeZone() {
        System.setProperty(TIME_ZONE_PROPERTY, "GMT+2");

        String systemTimeFormat = "HH:mm:ss";
        Timestamp timestamp = new Timestamp(123, 42000);
        assertThat(
                new SystemTimestampFormat(systemTimeFormat, Optional.of("GMT+3"), Locale.ENGLISH).apply(timestamp),
                is("03:00:42"));
    }

    @Test
    void testApply_englishLocale() {
        String systemTimeFormat = "EEEE, d MMMM";
        Timestamp timestamp = new Timestamp(123, 42000);
        assertThat(
                new SystemTimestampFormat(systemTimeFormat, Optional.empty(), Locale.ENGLISH).apply(timestamp),
                is("Thursday, 1 January"));
    }

    @Test
    void testApply_germanLocale() {
        String systemTimeFormat = "EEEE, d MMMM";
        Timestamp timestamp = new Timestamp(123, 42000);
        assertThat(
                new SystemTimestampFormat(systemTimeFormat, Optional.empty(), Locale.GERMAN).apply(timestamp),
                is("Donnerstag, 1 Januar"));
    }

    @Test
    void testApply_withInvalidHtml() {
        String systemTimeFormat = "'<b>'HH:mm:ss'</b><script>console.log(\"foo\")</script>'";
        Timestamp timestamp = new Timestamp(123, 42000);
        assertThat(
                new SystemTimestampFormat(systemTimeFormat, Optional.empty(), Locale.ENGLISH).apply(timestamp),
                is("<b>00:00:42</b>"));
    }

    @Test
    void testValidate() {
        String systemTimeFormat = "'<b>'HH:mm:ss'</b>'";
        new SystemTimestampFormat(systemTimeFormat, Optional.empty(), Locale.ENGLISH).validate();
    }

    @Test
    void testValidate_withFormatParseException() {
        String systemTimeFormat = "'<b>'pHH:mm:ss'</b>'";
        assertThrows(
                FormatParseException.class,
                () -> new SystemTimestampFormat(systemTimeFormat, Optional.empty(), Locale.ENGLISH).validate());
    }

    @Test
    void testValidate_withInvalidHtml() {
        String systemTimeFormat = "'<b>'HH:mm:ss'</b><script>console.log(\"foo\")</script>'";
        assertThrows(
                InvalidHtmlException.class,
                () -> new SystemTimestampFormat(systemTimeFormat, Optional.empty(), Locale.ENGLISH).validate());
    }

    @Test
    void testGetPlainTextUrl() {
        SystemTimestampFormat format =
                new SystemTimestampFormat("'<b>'HH:mm:ss'</b> '", Optional.empty(), Locale.ENGLISH);
        assertThat(format.getPlainTextUrl(), is("timestamps/?time=HH:mm:ss&appendLog&locale=en"));
    }

    @Test
    void testGetPlainTextUrl_excessWhitespace() {
        SystemTimestampFormat format =
                new SystemTimestampFormat(" ' <b> ' HH:mm:ss ' </b> ' ", Optional.empty(), Locale.ENGLISH);
        assertThat(format.getPlainTextUrl(), is("timestamps/?time=HH:mm:ss&appendLog&locale=en"));
    }

    @Test
    void testGetPlainTextUrl_withTimeZone() {
        SystemTimestampFormat format =
                new SystemTimestampFormat("'<b>'HH:mm:ss'</b> '", Optional.of("GMT+1"), Locale.ENGLISH);
        assertThat(format.getPlainTextUrl(), is("timestamps/?time=HH:mm:ss&timeZone=GMT+1&appendLog&locale=en"));
    }

    @Test
    void testEqualsAndHashCode() {
        EqualsVerifier.forClass(SystemTimestampFormat.class)
                .suppress(Warning.NULL_FIELDS)
                .verify();
    }
}
