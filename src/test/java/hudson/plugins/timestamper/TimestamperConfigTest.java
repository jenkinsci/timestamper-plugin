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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import hudson.util.XStream2;

import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Test for the {@link TimestamperConfig} class.
 * 
 * @author Steven G. Brown
 */
public class TimestamperConfigTest extends HudsonTestCase {

  private static final String customTimestampFormat = "HH:mm:ss "
      + TimestamperConfigTest.class.getSimpleName();

  /**
   */
  public void testDefaultTimestampFormat() {
    assertThat(new TimestamperConfig().getTimestampFormat(),
        containsString("HH:mm:ss"));
  }

  /**
   */
  public void testSetTimestampFormat() {
    TimestamperConfig config = new TimestamperConfig();
    config.setTimestampFormat(customTimestampFormat);
    assertThat(config.getTimestampFormat(), is(customTimestampFormat));
  }

  /**
   */
  public void testToXmlDefault() {
    TimestamperConfig config = new TimestamperConfig();
    assertThat(toXml(config), is(defaultXml()));
  }

  /**
   */
  public void testToXmlCustomFormat() {
    TimestamperConfig config = new TimestamperConfig();
    config.setTimestampFormat(customTimestampFormat);
    assertThat(toXml(config), is(xml(customTimestampFormat)));
  }

  /**
   */
  public void testFromXmlDefault() {
    TimestamperConfig config = fromXml(defaultXml());
    String defaultTimestampFormat = new TimestamperConfig()
        .getTimestampFormat();
    assertThat(config.getTimestampFormat(), is(defaultTimestampFormat));
  }

  /**
   */
  public void testFromXmlCustomFormat() {
    TimestamperConfig config = fromXml(xml(customTimestampFormat));
    assertThat(config.getTimestampFormat(), is(customTimestampFormat));
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
    return "<hudson.plugins.timestamper.TimestamperConfig>\n"
        + "  <helpRedirect/>\n"
        + "</hudson.plugins.timestamper.TimestamperConfig>";
  }

  private String xml(String timestampFormat) {
    return "<hudson.plugins.timestamper.TimestamperConfig>\n"
        + "  <helpRedirect/>\n" + "  <timestampFormat>" + timestampFormat
        + "</timestampFormat>\n"
        + "</hudson.plugins.timestamper.TimestamperConfig>";
  }
}
