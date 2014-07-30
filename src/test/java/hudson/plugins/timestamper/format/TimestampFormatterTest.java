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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.MarkupText;
import hudson.console.ConsoleNote;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.tasks._ant.AntTargetNote;

import java.util.Arrays;
import java.util.Collection;
import java.util.TimeZone;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.base.Optional;

/**
 * Unit test for the {@link TimestampFormatter} class.
 * 
 * @author Steven G. Brown
 */
@RunWith(Parameterized.class)
public class TimestampFormatterTest {

  /**
   * @return parameterised test data
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        { request("system"), span("00:00:42 "), span("08:00:42 ") },
        { request("elapsed"), span("00.123 "), span("00.123 ") },
        { request("none"), span(""), span("") },
        { request(), span("00:00:42 "), span("08:00:42 ") },
        { request((String[]) null), span("00:00:42 "), span("08:00:42 ") },
        { null, span("00:00:42 "), span("08:00:42 ") } });
  }

  private static HttpServletRequest request(String... cookieValues) {
    HttpServletRequest request = mock(HttpServletRequest.class);
    Cookie[] cookies = null;
    if (cookieValues != null) {
      cookies = new Cookie[cookieValues.length];
      for (int i = 0; i < cookieValues.length; i++) {
        cookies[i] = new Cookie("jenkins-timestamper", cookieValues[i]);
      }
    }
    when(request.getCookies()).thenReturn(cookies);
    return request;
  }

  private static String span(String timestampString) {
    return "<span class=\"timestamp\">" + timestampString + "</span>";
  }

  /**
   */
  @Parameter(0)
  public HttpServletRequest request;

  /**
   */
  @Parameter(1)
  public String prefix;

  /**
   */
  @Parameter(2)
  public String prefixInDifferentTimezone;

  private TimeZone systemDefaultTimeZone;

  /**
   */
  @Before
  public void setUp() {
    systemDefaultTimeZone = TimeZone.getDefault();
    // Set the time zone to get consistent results.
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
  }

  /**
   */
  @After
  public void tearDown() {
    TimeZone.setDefault(systemDefaultTimeZone);
  }

  /**
   */
  @Test
  public void testMarkup() {
    assertThat(markup("line").toString(true), is(prefix + "line"));
  }

  /**
   */
  @Test
  public void testMarkupElapsedTimeWithDifferentTimeZone() {
    TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
    assertThat(markup("line").toString(true), is(prefixInDifferentTimezone
        + "line"));
  }

  /**
   */
  @Test
  public void testMarkupElapsedTimeWithConfiguredTimeZone() {
    assertThat(markup("line", "GMT+8").toString(true),
        is(prefixInDifferentTimezone + "line"));
  }

  /**
   */
  @Test
  public void testMarkupThenAntTargetNote() {
    assertThat(annotate(markup("target:"), new AntTargetNote()).toString(true),
        is(prefix + "<b class=ant-target>target</b>:"));
  }

  /**
   */
  @Test
  public void testAntTargetNoteThenMarkup() {
    assertThat(markup(annotate("target:", new AntTargetNote())).toString(true),
        is(prefix + "<b class=ant-target>target</b>:"));
  }

  private MarkupText markup(String text) {
    return markup(text, null);
  }

  private MarkupText markup(String text, String timeZoneId) {
    return markup(new MarkupText(text), timeZoneId);
  }

  private MarkupText markup(MarkupText markupText) {
    return markup(markupText, null);
  }

  private MarkupText markup(MarkupText markupText, String timeZoneId) {
    TimestampFormatter formatter = new TimestampFormatter("HH:mm:ss ", "ss.S ",
        Optional.fromNullable(request), Optional.fromNullable(timeZoneId));
    formatter.markup(markupText, new Timestamp(123, 42000));
    return markupText;
  }

  @SuppressWarnings("rawtypes")
  private MarkupText annotate(String text, ConsoleNote... notes) {
    MarkupText markupText = new MarkupText(text);
    return annotate(markupText, notes);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private MarkupText annotate(MarkupText markupText, ConsoleNote... notes) {
    Object context = mock(Run.class);
    for (ConsoleNote note : notes) {
      note.annotate(context, markupText, 0);
    }
    return markupText;
  }
}
