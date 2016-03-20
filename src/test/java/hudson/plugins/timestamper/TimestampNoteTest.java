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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import hudson.MarkupText;
import hudson.model.Run;
import hudson.plugins.timestamper.format.TimestampFormat;
import hudson.plugins.timestamper.format.TimestampFormatProvider;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import com.google.common.base.Supplier;

/**
 * Unit test for the {@link TimestampNote} class.
 * 
 * @author Steven G. Brown
 */
@SuppressWarnings("boxing")
public class TimestampNoteTest {

  private Run<?, ?> build;

  private TimestampNote note;

  private TimestampFormat format;

  private MarkupText text;

  private final long BUILD_START = 1;

  private final long NOTE_ELAPSED = 4;

  private final long TIME = 3;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    build = mock(Run.class);
    Whitebox.setInternalState(build, "timestamp", BUILD_START);

    note = new TimestampNote(NOTE_ELAPSED, TIME);

    format = mock(TimestampFormat.class);
    Whitebox.setInternalState(TimestampFormatProvider.class,
        new Supplier<TimestampFormat>() {
          @Override
          public TimestampFormat get() {
            return format;
          }
        });

    text = new MarkupText("");
  }

  /**
   */
  @Test
  public void testGetTimestamp() {
    assertThat(note.getTimestamp(build), is(new Timestamp(TIME - BUILD_START,
        TIME)));
  }

  /**
   */
  @Test
  public void testGetTimestamp_afterSerialization() {
    note = (TimestampNote) SerializationUtils.clone(note);
    testGetTimestamp();
  }

  /**
   */
  @Test
  public void testAnnotate() {
    note.annotate(build, text, 0);
    verify(format).markup(text, new Timestamp(TIME - BUILD_START, TIME));
  }

  /**
   */
  @Test
  public void testAnnotate_afterSerialization() {
    note = (TimestampNote) SerializationUtils.clone(note);
    testAnnotate();
  }

  /**
   */
  @Test
  public void testAnnotate_unrecognisedContext() {
    note.annotate(new Object(), text, 0);
    verify(format).markup(text, new Timestamp(NOTE_ELAPSED, TIME));
  }

  /**
   */
  @Test
  public void testAnnotate_UnrecognisedContext_afterSerialization() {
    note = (TimestampNote) SerializationUtils.clone(note);
    testAnnotate_unrecognisedContext();
  }

  /**
   */
  @Test
  public void testAnnotate_unrecognisedContext_buildStartNotRecorded() {
    Whitebox.setInternalState(note, "elapsedMillis", (Object) null);
    note.annotate(new Object(), text, 0);
    verify(format).markup(text, new Timestamp(null, TIME));
  }

  /**
   */
  @Test
  public void testAnnotate_UnrecognisedContext_buildStartNotRecorded_afterSerialization() {
    note = (TimestampNote) SerializationUtils.clone(note);
    testAnnotate_unrecognisedContext_buildStartNotRecorded();
  }
}
