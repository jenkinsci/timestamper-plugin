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

  private TimeZone systemDefaultTimeZone;
  private String expectedFormattedTimestamp;

  /**
   */
  @Override
  protected void setUp() throws Exception {
    systemDefaultTimeZone = TimeZone.getDefault();
    // Set the time zone to get consistent results.
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    SimpleDateFormat smf = new SimpleDateFormat(TimestamperConfig.DEFAULT_TIMESTAMP_FORMAT);
    expectedFormattedTimestamp = smf.format(new Date(0));
    super.setUp();
  }

  /**
   */
  @Override
  protected void tearDown() throws Exception  {
    TimeZone.setDefault(systemDefaultTimeZone);
    super.tearDown();
  }

  /**
   */
  public void testTimestampNote() {
    assertThat(annotate("line", new TimestampNote(0)),
        is(expectedFormattedTimestamp + "line"));
  }

  /**
   */
  public void testSerialization() {
    assertThat(annotate("line", serialize(new TimestampNote(0))),
        is(expectedFormattedTimestamp + "line"));
  }

  /**
   */
  public void testTimestampThenAntTargetNote() {
    assertThat(annotate("target:", new TimestampNote(0), new AntTargetNote()),
        is(expectedFormattedTimestamp + "<b class=ant-target>target</b>:"));
  }

  /**
   */
  public void testAntTargetNoteThenTimestamp() {
    assertThat(annotate("target:", new AntTargetNote(), new TimestampNote(0)),
        is(expectedFormattedTimestamp + "<b class=ant-target>target</b>:"));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
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
}
