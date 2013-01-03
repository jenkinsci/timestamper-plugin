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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import hudson.MarkupText;
import hudson.console.ConsoleNote;
import hudson.model.Run;
import hudson.tasks._ant.AntTargetNote;

import java.util.Arrays;
import java.util.TimeZone;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for the {@link Timestamp} class.
 * 
 * @author Steven G. Brown
 */
@SuppressWarnings("boxing")
public class TimestampTest {

  private Timestamp timestamp;

  private String systemTimeFormat;

  private String elapsedTimeFormat;

  private TimeZone systemDefaultTimeZone;

  /**
   */
  @Before
  public void setUp() {
    timestamp = new Timestamp(123, 42000);
    systemTimeFormat = "HH:mm:ss ";
    elapsedTimeFormat = "ss.S ";

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
  public void testConstructor() {
    assertThat(
        Arrays.asList(timestamp.elapsedMillis, timestamp.millisSinceEpoch),
        is(Arrays.asList(123l, 42000l)));
  }

  /**
   */
  @Test
  public void testMarkupSystemTime() {
    assertThat(markupSystemTime("line").toString(true), is("00:00:42 line"));
  }

  /**
   */
  @Test
  public void testMarkupElapsedTime() {
    assertThat(markupElapsedTime("line").toString(true), is("00.123 line"));
  }

  /**
   */
  @Test
  public void testMarkupElapsedTimeWithDifferentTimeZone() {
    TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
    // unaffected by time zone
    assertThat(markupElapsedTime("line").toString(true), is("00.123 line"));
  }

  /**
   */
  @Test
  public void testMarkupSystemTimeThenAntTargetNote() {
    assertThat(annotate(markupSystemTime("target:"), new AntTargetNote())
        .toString(true), is("00:00:42 <b class=ant-target>target</b>:"));
  }

  /**
   */
  @Test
  public void testMarkupElapsedTimeThenAntTargetNote() {
    assertThat(annotate(markupElapsedTime("target:"), new AntTargetNote())
        .toString(true), is("00.123 <b class=ant-target>target</b>:"));
  }

  /**
   */
  @Test
  public void testAntTargetNoteThenMarkupSystemTime() {
    assertThat(markupSystemTime(annotate("target:", new AntTargetNote()))
        .toString(true), is("00:00:42 <b class=ant-target>target</b>:"));
  }

  /**
   */
  @Test
  public void testAntTargetNoteThenMarkupElapsedTime() {
    assertThat(markupElapsedTime(annotate("target:", new AntTargetNote()))
        .toString(true), is("00.123 <b class=ant-target>target</b>:"));
  }

  /**
   */
  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Timestamp.class)
        .suppress(Warning.STRICT_INHERITANCE).verify();
  }

  private MarkupText markupSystemTime(String text) {
    return markupSystemTime(new MarkupText(text));
  }

  private MarkupText markupSystemTime(MarkupText markupText) {
    timestamp.markupSystemTime(markupText, systemTimeFormat);
    return markupText;
  }

  private MarkupText markupElapsedTime(String text) {
    return markupElapsedTime(new MarkupText(text));
  }

  private MarkupText markupElapsedTime(MarkupText markupText) {
    timestamp.markupElapsedTime(markupText, elapsedTimeFormat);
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
