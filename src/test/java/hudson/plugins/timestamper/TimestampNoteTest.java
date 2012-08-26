/*
 * The MIT License
 * 
 * Copyright (c) 2010 Steven G. Brown
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
import hudson.MarkupText;
import hudson.console.ConsoleNote;
import hudson.tasks._ant.AntTargetNote;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang.SerializationUtils;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Unit test for the {@link TimestampNote} class.
 */
public class TimestampNoteTest extends HudsonTestCase {

  private static final String OTHER_FORMAT = "HHmmss";

  private TimeZone systemDefaultTimeZone;

  /**
   */
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    systemDefaultTimeZone = TimeZone.getDefault();
    // Set the time zone to get consistent results.
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    TimestamperConfig.get().setTimestampFormat(
        TimestamperConfig.DEFAULT_TIMESTAMP_FORMAT);
  }

  /**
   */
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    TimeZone.setDefault(systemDefaultTimeZone);
  }

  /**
   */
  public void testTimestampNote_DefaultFormat() {
    assertThat(annotate("line", new TimestampNote(0)),
        is(timestamp(0) + "line"));
  }

  /**
   */
  public void testTimestampNote_NonDefaultFormat() {
    TimestamperConfig.get().setTimestampFormat(OTHER_FORMAT);
    assertThat(annotate("line", new TimestampNote(0)),
        is(timestamp(0) + "line"));
  }

  /**
   */
  public void testSerialization_DefaultFormat() {
    assertThat(annotate("line", serialize(new TimestampNote(0))),
        is(timestamp(0) + "line"));
  }

  /**
   */
  public void testSerialization_NonDefaultFormat() {
    TimestamperConfig.get().setTimestampFormat(OTHER_FORMAT);
    assertThat(annotate("line", serialize(new TimestampNote(0))),
        is(timestamp(0) + "line"));
  }

  /**
   */
  public void testTimestampThenAntTargetNote() {
    assertThat(annotate("target:", new TimestampNote(0), new AntTargetNote()),
        is(timestamp(0) + "<b class=ant-target>target</b>:"));
  }

  /**
   */
  public void testAntTargetNoteThenTimestamp() {
    assertThat(annotate("target:", new AntTargetNote(), new TimestampNote(0)),
        is(timestamp(0) + "<b class=ant-target>target</b>:"));
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private String annotate(String text, ConsoleNote... notes) {
    Object context = new Object();
    MarkupText markupText = new MarkupText(text);
    for (ConsoleNote note : notes) {
      note.annotate(context, markupText, 0);
    }
    return markupText.toString(true);
  }

  private TimestampNote serialize(TimestampNote note) {
    return (TimestampNote) SerializationUtils.clone(note);
  }

  private String timestamp(long millisSinceEpoch) {
    TimestamperConfig config = TimestamperConfig.get();
    return new SimpleDateFormat(config.getTimestampFormat()).format(new Date(
        millisSinceEpoch));
  }
}
