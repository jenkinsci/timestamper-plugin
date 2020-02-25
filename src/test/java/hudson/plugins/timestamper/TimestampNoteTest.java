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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hudson.MarkupText;
import hudson.model.Run;
import hudson.plugins.timestamper.format.TimestampFormat;
import hudson.plugins.timestamper.format.TimestampFormatProvider;
import java.util.Arrays;
import java.util.function.Supplier;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.reflect.Whitebox;

/**
 * Unit test for the {@link TimestampNote} class.
 *
 * @author Steven G. Brown
 */
@RunWith(Parameterized.class)
public class TimestampNoteTest {

  private static final long BUILD_START = 1;

  private static final long ELAPSED = 4;

  private static final long TIME = 3;

  private Supplier<TimestampFormat> originalSupplier;

  /** @return the test cases */
  @Parameters(name = "{0}, {1}")
  public static Iterable<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          //
          {build(), note(ELAPSED, TIME), new Timestamp(ELAPSED, TIME)},
          //
          {build(), note(null, TIME), new Timestamp(TIME - BUILD_START, TIME)},
          //
          {new Object(), note(ELAPSED, TIME), new Timestamp(ELAPSED, TIME)},
          //
          {new Object(), note(null, TIME), new Timestamp(null, TIME)}
        });
  }

  private static Run<?, ?> build() {
    Run<?, ?> build = mock(Run.class);
    Whitebox.setInternalState(build, "timestamp", BUILD_START);
    when(build.toString())
        .thenReturn(new ToStringBuilder("Run").append("startTime", BUILD_START).toString());
    return build;
  }

  private static TimestampNote note(Long elapsedMillis, long millisSinceEpoch) {
    TimestampNote note =
        new TimestampNote(elapsedMillis == null ? 0L : elapsedMillis, millisSinceEpoch);
    if (elapsedMillis == null) {
      Whitebox.setInternalState(note, "elapsedMillis", (Object) null);
    }
    return note;
  }

  @Parameter(0)
  public Object context;

  @Parameter(1)
  public TimestampNote note;

  @Parameter(2)
  public Timestamp expectedTimestamp;

  @Mock private TimestampFormat format;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    originalSupplier = Whitebox.getInternalState(TimestampFormatProvider.class, Supplier.class);
    Whitebox.setInternalState(
        TimestampFormatProvider.class, (Supplier<TimestampFormat>) () -> format);
  }

  @After
  public void tearDown() {
    Whitebox.setInternalState(TimestampFormatProvider.class, Supplier.class, originalSupplier);
  }

  @Test
  public void testGetTimestamp() {
    assertThat(note.getTimestamp(context), is(expectedTimestamp));
  }

  @Test
  public void testGetTimestamp_afterSerialization() {
    note = (TimestampNote) SerializationUtils.clone(note);
    testGetTimestamp();
  }

  @Test
  public void testAnnotate() {
    MarkupText text = new MarkupText("");
    note.annotate(context, text, 0);
    verify(format).markup(text, expectedTimestamp);
  }

  @Test
  public void testAnnotate_afterSerialization() {
    note = (TimestampNote) SerializationUtils.clone(note);
    testAnnotate();
  }
}
