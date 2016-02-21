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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import hudson.plugins.timestamper.Timestamp;

import java.util.TimeZone;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;

/**
 * Unit test for the {@link SystemTimestampFormat} class.
 * 
 * @author Steven G. Brown
 */
public class SystemTimestampFormatTest {

  private TimeZone systemDefaultTimeZone;

  /**
   */
  @Before
  public void setUp() {
    systemDefaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    System.clearProperty(SystemTimestampFormat.TIME_ZONE_PROPERTY);
  }

  /**
   */
  @After
  public void tearDown() {
    TimeZone.setDefault(systemDefaultTimeZone);
    System.clearProperty(SystemTimestampFormat.TIME_ZONE_PROPERTY);
  }

  /**
   */
  @Test
  public void testApply() {
    String systemTimeFormat = "HH:mm:ss";
    Timestamp timestamp = new Timestamp(123, 42000);
    assertThat(
        new SystemTimestampFormat(systemTimeFormat, Optional.<String> absent())
            .apply(timestamp),
        is("00:00:42"));
  }

  /**
   */
  @Test
  public void testApply_withDifferentTimeZone() {
    TimeZone.setDefault(TimeZone.getTimeZone("GMT+1"));

    String systemTimeFormat = "HH:mm:ss";
    Timestamp timestamp = new Timestamp(123, 42000);
    assertThat(
        new SystemTimestampFormat(systemTimeFormat, Optional.<String> absent())
            .apply(timestamp),
        is("01:00:42"));
  }

  /**
   */
  @Test
  public void testApply_withSystemProperty() {
    System.setProperty(SystemTimestampFormat.TIME_ZONE_PROPERTY, "GMT+2");

    String systemTimeFormat = "HH:mm:ss";
    Timestamp timestamp = new Timestamp(123, 42000);
    assertThat(
        new SystemTimestampFormat(systemTimeFormat, Optional.<String> absent())
            .apply(timestamp),
        is("02:00:42"));
  }

  /**
   */
  @Test
  public void testApply_withProvidedTimeZone() {
    String systemTimeFormat = "HH:mm:ss";
    Timestamp timestamp = new Timestamp(123, 42000);
    assertThat(
        new SystemTimestampFormat(systemTimeFormat, Optional.of("GMT+3"))
            .apply(timestamp),
        is("03:00:42"));
  }

  /**
   */
  @Test
  public void testApply_withSystemPropertyAndProvidedTimeZone() {
    System.setProperty(SystemTimestampFormat.TIME_ZONE_PROPERTY, "GMT+2");

    String systemTimeFormat = "HH:mm:ss";
    Timestamp timestamp = new Timestamp(123, 42000);
    assertThat(
        new SystemTimestampFormat(systemTimeFormat, Optional.of("GMT+3"))
            .apply(timestamp),
        is("03:00:42"));
  }

  /**
   */
  @Test
  public void testGetPlainTextUrl() {
    SystemTimestampFormat format = new SystemTimestampFormat(
        "'<b>'HH:mm:ss'</b> '", Optional.<String> absent());
    assertThat(format.getPlainTextUrl(),
        is("timestamps?time=HH:mm:ss&appendLog"));
  }

  /**
   */
  @Test
  public void testGetPlainTextUrl_excessWhitespace() {
    SystemTimestampFormat format = new SystemTimestampFormat(
        " ' <b> ' HH:mm:ss ' </b> ' ", Optional.<String> absent());
    assertThat(format.getPlainTextUrl(),
        is("timestamps?time=HH:mm:ss&appendLog"));
  }

  /**
   */
  @Test
  public void testGetPlainTextUrl_withTimeZone() {
    SystemTimestampFormat format = new SystemTimestampFormat(
        "'<b>'HH:mm:ss'</b> '", Optional.of("GMT+1"));
    assertThat(format.getPlainTextUrl(),
        is("timestamps?time=HH:mm:ss&timeZone=GMT+1&appendLog"));
  }

  /**
   */
  @Test
  public void testEqualsAndHashCode() {
    EqualsVerifier.forClass(SystemTimestampFormat.class)
        .suppress(Warning.NULL_FIELDS).verify();
  }
}
