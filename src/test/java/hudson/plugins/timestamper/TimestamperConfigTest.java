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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.util.XStream2;
import jenkins.model.Jenkins;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

/**
 * Test for the {@link TimestamperConfig} class.
 * 
 * @author Steven G. Brown
 */
@PrepareForTest(Jenkins.class)
public class TimestamperConfigTest {

  private static final String customTimestampFormat = "HH:mm:ss "
      + TimestamperConfigTest.class.getSimpleName();

  /**
   */
  @Rule
  public PowerMockRule powerMockRule = new PowerMockRule();

  /**
   */
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  /**
   */
  @Before
  public void setUp() {
    Jenkins jenkins = mock(Jenkins.class);
    when(jenkins.getRootDir()).thenReturn(folder.getRoot());
    PowerMockito.mockStatic(Jenkins.class);
    when(Jenkins.getInstance()).thenReturn(jenkins);
  }

  /**
   */
  @Test
  public void testDefaultTimestampFormat() {
    assertThat(new TimestamperConfig().getTimestampFormat(),
        containsString("HH:mm:ss"));
  }

  /**
   */
  @Test
  public void testSetTimestampFormat() {
    TimestamperConfig config = new TimestamperConfig();
    config.setTimestampFormat(customTimestampFormat);
    assertThat(config.getTimestampFormat(), is(customTimestampFormat));
  }

  /**
   */
  @Test
  public void testSetTimestampFormatEmpty() {
    TimestamperConfig config = new TimestamperConfig();
    config.setTimestampFormat("");
    assertThat(config.getTimestampFormat(), is(""));
  }

  /**
   */
  @Test
  public void testToXmlDefault() {
    TimestamperConfig config = new TimestamperConfig();
    assertThat(toXml(config), is(defaultXml()));
  }

  /**
   */
  @Test
  public void testToXmlCustomFormat() {
    TimestamperConfig config = new TimestamperConfig();
    config.setTimestampFormat(customTimestampFormat);
    assertThat(toXml(config), is(xml(customTimestampFormat)));
  }

  /**
   */
  @Test
  public void testFromXmlDefault() {
    TimestamperConfig config = fromXml(defaultXml());
    String defaultTimestampFormat = new TimestamperConfig()
        .getTimestampFormat();
    assertThat(config.getTimestampFormat(), is(defaultTimestampFormat));
  }

  /**
   */
  @Test
  public void testFromXmlCustomFormat() {
    TimestamperConfig config = fromXml(xml(customTimestampFormat));
    assertThat(config.getTimestampFormat(), is(customTimestampFormat));
  }

  /**
   */
  @Test
  public void testFromXmlEmptyFormat() {
    TimestamperConfig config = fromXml(xml(""));
    assertThat(config.getTimestampFormat(), is(""));
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
