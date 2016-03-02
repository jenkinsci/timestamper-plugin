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

import java.util.Arrays;

import jenkins.model.Jenkins;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Test for the {@link TimestamperConfig} class.
 * 
 * @author Steven G. Brown
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Jenkins.class)
public class TimestamperConfigTest {

  private static final String customSystemTimeFormat = "HH:mm:ss "
      + TimestamperConfigTest.class.getSimpleName();

  private static final String customElapsedTimeFormat = "ss.S "
      + TimestamperConfigTest.class.getSimpleName();

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
  public void testDefaultSystemTimeFormat() {
    assertThat(new TimestamperConfig().getSystemTimeFormat(),
        containsString("HH:mm:ss"));
  }

  /**
   */
  @Test
  public void testDefaultSystemTimeFormat_noJenkinsInstance() {
    when(Jenkins.getInstance()).thenReturn(null);
    TimestamperConfig config = TimestamperConfig.get();
    assertThat(config.getSystemTimeFormat(), containsString("HH:mm:ss"));
  }

  /**
   */
  @Test
  public void testDefaultElapsedTimeFormat() {
    assertThat(new TimestamperConfig().getElapsedTimeFormat(),
        containsString("HH:mm:ss.S"));
  }

  /**
   */
  @Test
  public void testDefaultElapsedTimeFormat_noJenkinsInstance() {
    when(Jenkins.getInstance()).thenReturn(null);
    TimestamperConfig config = TimestamperConfig.get();
    assertThat(config.getElapsedTimeFormat(), containsString("HH:mm:ss.S"));
  }

  /**
   */
  @Test
  public void testSetSystemTimeFormat() {
    TimestamperConfig config = new TimestamperConfig();
    config.setSystemTimeFormat(customSystemTimeFormat);
    assertThat(config.getSystemTimeFormat(), is(customSystemTimeFormat));
  }

  /**
   */
  @Test
  public void testSetElapsedTimeFormat() {
    TimestamperConfig config = new TimestamperConfig();
    config.setElapsedTimeFormat(customElapsedTimeFormat);
    assertThat(config.getElapsedTimeFormat(), is(customElapsedTimeFormat));
  }

  /**
   */
  @Test
  public void testSetSystemTimeFormatEmpty() {
    TimestamperConfig config = new TimestamperConfig();
    config.setSystemTimeFormat("");
    assertThat(config.getSystemTimeFormat(), is(""));
  }

  /**
   */
  @Test
  public void testSetElapsedTimeFormatEmpty() {
    TimestamperConfig config = new TimestamperConfig();
    config.setElapsedTimeFormat("");
    assertThat(config.getElapsedTimeFormat(), is(""));
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
  public void testToXmlCustomSystemTimeFormat() {
    TimestamperConfig config = new TimestamperConfig();
    config.setSystemTimeFormat(customSystemTimeFormat);
    assertThat(toXml(config), is(xml(customSystemTimeFormat, null)));
  }

  /**
   */
  @Test
  public void testToXmlCustomElapsedTimeFormat() {
    TimestamperConfig config = new TimestamperConfig();
    config.setElapsedTimeFormat(customElapsedTimeFormat);
    assertThat(toXml(config), is(xml(null, customElapsedTimeFormat)));
  }

  /**
   */
  @Test
  public void testFromXmlDefault() {
    TimestamperConfig config = fromXml(defaultXml());
    TimestamperConfig defaultConfig = new TimestamperConfig();
    assertThat(
        Arrays.asList(config.getSystemTimeFormat(),
            config.getElapsedTimeFormat()),
        is(Arrays.asList(defaultConfig.getSystemTimeFormat(),
            defaultConfig.getElapsedTimeFormat())));
  }

  /**
   */
  @Test
  public void testFromXmlCustomSystemTimeFormat() {
    TimestamperConfig config = fromXml(xml(customSystemTimeFormat, null));
    assertThat(config.getSystemTimeFormat(), is(customSystemTimeFormat));
  }

  /**
   */
  @Test
  public void testFromXmlCustomElapsedTimeFormat() {
    TimestamperConfig config = fromXml(xml(null, customElapsedTimeFormat));
    assertThat(config.getElapsedTimeFormat(), is(customElapsedTimeFormat));
  }

  /**
   */
  @Test
  public void testFromXmlEmptyFormat() {
    TimestamperConfig config = fromXml(xml("", ""));
    assertThat(
        Arrays.asList(config.getSystemTimeFormat(),
            config.getElapsedTimeFormat()), is(Arrays.asList("", "")));
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
    return "<hudson.plugins.timestamper.TimestamperConfig/>";
  }

  private String xml(String systemTimeFormat, String elapsedTimeFormat) {
    String xml = "<hudson.plugins.timestamper.TimestamperConfig>\n";
    if (systemTimeFormat != null) {
      xml += "  <timestampFormat>" + systemTimeFormat + "</timestampFormat>\n";
    }
    if (elapsedTimeFormat != null) {
      xml += "  <elapsedTimeFormat>" + elapsedTimeFormat
          + "</elapsedTimeFormat>\n";
    }
    xml += "</hudson.plugins.timestamper.TimestamperConfig>";
    return xml;
  }
}
