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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for the {@link Timestamp} class.
 * 
 * @author Steven G. Brown
 */
@SuppressWarnings("boxing")
public class TimestampTest {

  private static final String FORMAT_ONE = "HH:mm:ss";

  private static final String FORMAT_TWO = "HHmmss";

  private static TimeZone systemDefaultTimeZone;

  /**
   */
  @BeforeClass
  public static void setUpBeforeClass() {
    systemDefaultTimeZone = TimeZone.getDefault();
    // Set the time zone to get consistent results.
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
  }

  /**
   */
  @AfterClass
  public static void tearDownAfterClass() {
    TimeZone.setDefault(systemDefaultTimeZone);
  }

  private String timestampFormat;

  /**
   */
  @Before
  public void setUp() {
    timestampFormat = FORMAT_ONE;
  }

  /**
   */
  @Test
  public void testConstructor() {
    Timestamp timestamp = new Timestamp(1, 2);
    assertThat(
        Arrays.asList(timestamp.elapsedMillis, timestamp.millisSinceEpoch),
        is(Arrays.asList(1l, 2l)));
  }

  /**
   */
  @Test
  public void testTimestampNote_DefaultFormat() {
    assertThat(markup("line", 0).toString(true), is(timestamp(0) + "line"));
  }

  /**
   */
  @Test
  public void testTimestampNote_NonDefaultFormat() {
    timestampFormat = FORMAT_TWO;
    assertThat(markup("line", 0).toString(true), is(timestamp(0) + "line"));
  }

  /**
   */
  @Test
  public void testTimestampThenAntTargetNote() {
    assertThat(
        annotate(markup("target:", 0), new AntTargetNote()).toString(true),
        is(timestamp(0) + "<b class=ant-target>target</b>:"));
  }

  /**
   */
  @Test
  public void testAntTargetNoteThenTimestamp() {
    assertThat(
        markup(annotate("target:", new AntTargetNote()), 0).toString(true),
        is(timestamp(0) + "<b class=ant-target>target</b>:"));
  }

  /**
   */
  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Timestamp.class).verify();
  }

  private MarkupText markup(String text, long millisSinceEpoch) {
    MarkupText markupText = new MarkupText(text);
    return markup(markupText, millisSinceEpoch);
  }

  private MarkupText markup(MarkupText markupText, long millisSinceEpoch) {
    new Timestamp(0, millisSinceEpoch).markup(markupText, timestampFormat);
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

  private String timestamp(long millisSinceEpoch) {
    return new SimpleDateFormat(timestampFormat).format(new Date(
        millisSinceEpoch));
  }
}
