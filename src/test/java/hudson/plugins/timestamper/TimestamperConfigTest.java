/*
 * The MIT License
 *
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import hudson.util.XStream2;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * Test for the {@link TimestamperConfig} class.
 *
 * @author Steven G. Brown
 */
@WithJenkins
class TimestamperConfigTest {

    private static final String customSystemTimeFormat = "HH:mm:ss " + TimestamperConfigTest.class.getSimpleName();

    private static final String customElapsedTimeFormat = "ss.S " + TimestamperConfigTest.class.getSimpleName();

    @Test
    void testDefaultSystemTimeFormat(JenkinsRule r) {
        assertThat(TimestamperConfig.get().getSystemTimeFormat(), containsString("HH:mm:ss"));
    }

    @Test
    void testDefaultElapsedTimeFormat(JenkinsRule r) {
        assertThat(TimestamperConfig.get().getElapsedTimeFormat(), containsString("HH:mm:ss.S"));
    }

    @Test
    void testSetSystemTimeFormat(JenkinsRule r) {
        TimestamperConfig config = TimestamperConfig.get();
        config.setSystemTimeFormat(customSystemTimeFormat);
        assertThat(config.getSystemTimeFormat(), is(customSystemTimeFormat));
    }

    @Test
    void testSetElapsedTimeFormat(JenkinsRule r) {
        TimestamperConfig config = TimestamperConfig.get();
        config.setElapsedTimeFormat(customElapsedTimeFormat);
        assertThat(config.getElapsedTimeFormat(), is(customElapsedTimeFormat));
    }

    @Test
    void testSetSystemTimeFormatEmpty(JenkinsRule r) {
        TimestamperConfig config = TimestamperConfig.get();
        config.setSystemTimeFormat("");
        assertThat(config.getSystemTimeFormat(), is(""));
    }

    @Test
    void testSetElapsedTimeFormatEmpty(JenkinsRule r) {
        TimestamperConfig config = TimestamperConfig.get();
        config.setElapsedTimeFormat("");
        assertThat(config.getElapsedTimeFormat(), is(""));
    }

    @Test
    void testSetSystemTimeFormatNull(JenkinsRule r) {
        TimestamperConfig config = TimestamperConfig.get();
        config.setSystemTimeFormat(null);
        assertThat(config.getSystemTimeFormat(), is("'<b>'HH:mm:ss'</b> '"));
    }

    @Test
    void testSetElapsedTimeFormatNull(JenkinsRule r) {
        TimestamperConfig config = TimestamperConfig.get();
        config.setElapsedTimeFormat(null);
        assertThat(config.getElapsedTimeFormat(), is("'<b>'HH:mm:ss.S'</b> '"));
    }

    @Test
    void testSetSystemTimeFormatTrimmed(JenkinsRule r) {
        TimestamperConfig config = TimestamperConfig.get();
        config.setSystemTimeFormat("'<b>'HH:mm:ss'</b> '");
        assertThat(config.getSystemTimeFormat(), is("'<b>'HH:mm:ss'</b> '"));
    }

    @Test
    void testSetElapsedTimeFormatTrimmed(JenkinsRule r) {
        TimestamperConfig config = TimestamperConfig.get();
        config.setElapsedTimeFormat("'<b>'HH:mm:ss.S'</b> '");
        assertThat(config.getElapsedTimeFormat(), is("'<b>'HH:mm:ss.S'</b> '"));
    }

    @Test
    void testSetSystemTimeFormatNotTrimmed(JenkinsRule r) {
        TimestamperConfig config = TimestamperConfig.get();
        config.setSystemTimeFormat("       '<b>'HH:mm:ss'</b> '           ");
        assertThat(config.getSystemTimeFormat(), is("'<b>'HH:mm:ss'</b> '"));
    }

    @Test
    void testSetElapsedTimeFormatNotTrimmed(JenkinsRule r) {
        TimestamperConfig config = TimestamperConfig.get();
        config.setElapsedTimeFormat("        '<b>'HH:mm:ss.S'</b> '              ");
        assertThat(config.getElapsedTimeFormat(), is("'<b>'HH:mm:ss.S'</b> '"));
    }

    @Test
    void testToXmlDefault(JenkinsRule r) {
        TimestamperConfig config = TimestamperConfig.get();
        assertThat(toXml(config), is(defaultXml()));
    }

    @Test
    void testToXmlCustomSystemTimeFormat(JenkinsRule r) {
        TimestamperConfig config = TimestamperConfig.get();
        config.setSystemTimeFormat(customSystemTimeFormat);
        assertThat(toXml(config), is(xml(customSystemTimeFormat, null)));
    }

    @Test
    void testToXmlCustomElapsedTimeFormat(JenkinsRule r) {
        TimestamperConfig config = TimestamperConfig.get();
        config.setElapsedTimeFormat(customElapsedTimeFormat);
        assertThat(toXml(config), is(xml(null, customElapsedTimeFormat)));
    }

    @Test
    void testFromXmlDefault(JenkinsRule r) {
        TimestamperConfig config = fromXml(defaultXml());
        TimestamperConfig defaultConfig = TimestamperConfig.get();
        assertThat(
                Arrays.asList(config.getSystemTimeFormat(), config.getElapsedTimeFormat()),
                is(Arrays.asList(defaultConfig.getSystemTimeFormat(), defaultConfig.getElapsedTimeFormat())));
    }

    @Test
    void testFromXmlCustomSystemTimeFormat(JenkinsRule r) {
        TimestamperConfig config = fromXml(xml(customSystemTimeFormat, null));
        assertThat(config.getSystemTimeFormat(), is(customSystemTimeFormat));
    }

    @Test
    void testFromXmlCustomElapsedTimeFormat(JenkinsRule r) {
        TimestamperConfig config = fromXml(xml(null, customElapsedTimeFormat));
        assertThat(config.getElapsedTimeFormat(), is(customElapsedTimeFormat));
    }

    @Test
    void testFromXmlEmptyFormat(JenkinsRule r) {
        TimestamperConfig config = fromXml(xml("", ""));
        assertThat(
                Arrays.asList(config.getSystemTimeFormat(), config.getElapsedTimeFormat()), is(Arrays.asList("", "")));
    }

    private String toXml(TimestamperConfig config) {
        XStream2 xStream2 = new XStream2();
        return xStream2.toXML(config);
    }

    private TimestamperConfig fromXml(String xml) {
        XStream2 xStream2 = new XStream2();
        return (TimestamperConfig) xStream2.fromXML(xml);
    }

    private String defaultXml() {
        return xml(null, null);
    }

    private String xml(String systemTimeFormat, String elapsedTimeFormat) {
        String xml = "<hudson.plugins.timestamper.TimestamperConfig>\n";
        if (systemTimeFormat != null) {
            xml += "  <timestampFormat>" + systemTimeFormat + "</timestampFormat>\n";
        }
        if (elapsedTimeFormat != null) {
            xml += "  <elapsedTimeFormat>" + elapsedTimeFormat + "</elapsedTimeFormat>\n";
        }
        xml += "  <allPipelines>false</allPipelines>\n";
        xml += "</hudson.plugins.timestamper.TimestamperConfig>";
        return xml;
    }
}
