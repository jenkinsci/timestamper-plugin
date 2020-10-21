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
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Test for the {@link TimestamperConfig} class.
 *
 * @author Steven G. Brown
 */
public class TimestamperConfigTest {

  private static final String customSystemTimeFormat =
      "HH:mm:ss " + TimestamperConfigTest.class.getSimpleName();

  private static final String customElapsedTimeFormat =
      "ss.S " + TimestamperConfigTest.class.getSimpleName();

  @Rule public JenkinsRule r = new JenkinsRule();

  @Test
  public void testDefaultSystemTimeFormat() {
    assertThat(TimestamperConfig.get().getSystemTimeFormat(), containsString("HH:mm:ss"));
  }

  @Test
  public void testDefaultElapsedTimeFormat() {
    assertThat(TimestamperConfig.get().getElapsedTimeFormat(), containsString("HH:mm:ss.S"));
  }

  @Test
  public void testSetSystemTimeFormat() {
    TimestamperConfig config = TimestamperConfig.get();
    config.setSystemTimeFormat(customSystemTimeFormat);
    assertThat(config.getSystemTimeFormat(), is(customSystemTimeFormat));
  }

  @Test
  public void testSetElapsedTimeFormat() {
    TimestamperConfig config = TimestamperConfig.get();
    config.setElapsedTimeFormat(customElapsedTimeFormat);
    assertThat(config.getElapsedTimeFormat(), is(customElapsedTimeFormat));
  }

  @Test
  public void testSetSystemTimeFormatEmpty() {
    TimestamperConfig config = TimestamperConfig.get();
    config.setSystemTimeFormat("");
    assertThat(config.getSystemTimeFormat(), is(""));
  }

  @Test
  public void testSetElapsedTimeFormatEmpty() {
    TimestamperConfig config = TimestamperConfig.get();
    config.setElapsedTimeFormat("");
    assertThat(config.getElapsedTimeFormat(), is(""));
  }

  @Test
  public void testSetSystemTimeFormatNull() {
    TimestamperConfig config = TimestamperConfig.get();
    config.setSystemTimeFormat(null);
    assertThat(config.getSystemTimeFormat(), is("'<b>'HH:mm:ss'</b> '"));
  }

  @Test
  public void testSetElapsedTimeFormatNull() {
    TimestamperConfig config = TimestamperConfig.get();
    config.setElapsedTimeFormat(null);
    assertThat(config.getElapsedTimeFormat(), is("'<b>'HH:mm:ss.S'</b> '"));
  }

  @Test
  public void testSetSystemTimeFormatTrimmed() {
    TimestamperConfig config = TimestamperConfig.get();
    config.setSystemTimeFormat("'<b>'HH:mm:ss'</b> '");
    assertThat(config.getSystemTimeFormat(), is("'<b>'HH:mm:ss'</b> '"));
  }

  @Test
  public void testSetElapsedTimeFormatTrimmed() {
    TimestamperConfig config = TimestamperConfig.get();
    config.setElapsedTimeFormat("'<b>'HH:mm:ss.S'</b> '");
    assertThat(config.getElapsedTimeFormat(), is("'<b>'HH:mm:ss.S'</b> '"));
  }

  @Test
  public void testSetSystemTimeFormatNotTrimmed() {
    TimestamperConfig config = TimestamperConfig.get();
    config.setSystemTimeFormat("       '<b>'HH:mm:ss'</b> '           ");
    assertThat(config.getSystemTimeFormat(), is("'<b>'HH:mm:ss'</b> '"));
  }

  @Test
  public void testSetElapsedTimeFormatNotTrimmed() {
    TimestamperConfig config = TimestamperConfig.get();
    config.setElapsedTimeFormat("        '<b>'HH:mm:ss.S'</b> '              ");
    assertThat(config.getElapsedTimeFormat(), is("'<b>'HH:mm:ss.S'</b> '"));
  }

  @Test
  public void testToXmlDefault() {
    TimestamperConfig config = TimestamperConfig.get();
    assertThat(toXml(config), is(defaultXml()));
  }

  @Test
  public void testToXmlCustomSystemTimeFormat() {
    TimestamperConfig config = TimestamperConfig.get();
    config.setSystemTimeFormat(customSystemTimeFormat);
    assertThat(toXml(config), is(xml(customSystemTimeFormat, null)));
  }

  @Test
  public void testToXmlCustomElapsedTimeFormat() {
    TimestamperConfig config = TimestamperConfig.get();
    config.setElapsedTimeFormat(customElapsedTimeFormat);
    assertThat(toXml(config), is(xml(null, customElapsedTimeFormat)));
  }

  @Test
  public void testFromXmlDefault() {
    TimestamperConfig config = fromXml(defaultXml());
    TimestamperConfig defaultConfig = TimestamperConfig.get();
    assertThat(
        Arrays.asList(config.getSystemTimeFormat(), config.getElapsedTimeFormat()),
        is(
            Arrays.asList(
                defaultConfig.getSystemTimeFormat(), defaultConfig.getElapsedTimeFormat())));
  }

  @Test
  public void testFromXmlCustomSystemTimeFormat() {
    TimestamperConfig config = fromXml(xml(customSystemTimeFormat, null));
    assertThat(config.getSystemTimeFormat(), is(customSystemTimeFormat));
  }

  @Test
  public void testFromXmlCustomElapsedTimeFormat() {
    TimestamperConfig config = fromXml(xml(null, customElapsedTimeFormat));
    assertThat(config.getElapsedTimeFormat(), is(customElapsedTimeFormat));
  }

  @Test
  public void testFromXmlEmptyFormat() {
    TimestamperConfig config = fromXml(xml("", ""));
    assertThat(
        Arrays.asList(config.getSystemTimeFormat(), config.getElapsedTimeFormat()),
        is(Arrays.asList("", "")));
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
