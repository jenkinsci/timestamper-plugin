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
package hudson.plugins.timestamper.annotator;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.SerializationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;

import com.google.common.base.Supplier;

import hudson.MarkupText;
import hudson.console.ConsoleAnnotator;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.format.TimestampFormat;
import hudson.plugins.timestamper.format.TimestampFormatProvider;
import hudson.plugins.timestamper.io.TimestampsWriter;

/**
 * Unit test for the {@link TimestampAnnotator} class.
 *
 * @author Steven G. Brown
 */
@RunWith(Parameterized.class)
public class TimestampAnnotatorTest {

  /** @return parameterised test data */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[] {false}, new Object[] {true});
  }

  /** */
  @Parameter public boolean serialize;

  /** */
  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private Run<?, ?> build;

  private static ConsoleLogParser.Result logPosition;

  private static List<Timestamp> capturedTimestamps;

  private TimestampsWriter writer;

  /** @throws Exception */
  @Before
  public void setUp() throws Exception {
    build = mock(Run.class);
    when(build.getRootDir()).thenReturn(folder.getRoot());

    logPosition = new ConsoleLogParser.Result();
    capturedTimestamps = new ArrayList<Timestamp>();

    writer = new TimestampsWriter(build);
  }

  /** @throws Exception */
  @After
  public void tearDown() throws Exception {
    writer.close();
  }

  /** @throws Exception */
  @Test
  public void testStartOfLogFile() throws Exception {
    List<Timestamp> timestamps = writeTimestamps(2);
    logPosition.lineNumber = 0;
    logPosition.atNewLine = true;
    assertThat(annotate(), is(timestamps));
  }

  /** @throws Exception */
  @Test
  public void testStartOfLogFile_negativeLineNumber() throws Exception {
    List<Timestamp> timestamps = writeTimestamps(2);
    logPosition.lineNumber = -2;
    logPosition.atNewLine = true;
    assertThat(annotate(), is(timestamps));
  }

  /** @throws Exception */
  @Test
  public void testWithinFirstLine() throws Exception {
    List<Timestamp> timestamps = writeTimestamps(2);
    logPosition.lineNumber = 0;
    logPosition.atNewLine = false;
    assertThat(annotate(), is(timestamps.subList(1, 2)));
  }

  /** @throws Exception */
  @Test
  public void testWithinFirstLine_negativeLineNumber() throws Exception {
    List<Timestamp> timestamps = writeTimestamps(2);
    logPosition.lineNumber = -2;
    logPosition.atNewLine = false;
    assertThat(annotate(), is(timestamps.subList(1, 2)));
  }

  /** @throws Exception */
  @Test
  public void testNextLine() throws Exception {
    List<Timestamp> timestamps = writeTimestamps(2);
    logPosition.lineNumber = 1;
    logPosition.atNewLine = true;
    assertThat(annotate(), is(timestamps.subList(1, 2)));
  }

  /** @throws Exception */
  @Test
  public void testNextLine_negativeLineNumber() throws Exception {
    List<Timestamp> timestamps = writeTimestamps(2);
    logPosition.lineNumber = -1;
    logPosition.atNewLine = true;
    assertThat(annotate(), is(timestamps.subList(1, 2)));
  }

  /** @throws Exception */
  @Test
  public void testEndOfLogFile() throws Exception {
    logPosition.endOfFile = true;
    assertThat(annotate(), is(Collections.<Timestamp>emptyList()));
  }

  private List<Timestamp> writeTimestamps(int count) throws Exception {
    List<Timestamp> timestamps = new ArrayList<Timestamp>();
    for (int i = 0; i < count; i++) {
      writer.write(i, 1);
      timestamps.add(new Timestamp(i, i));
    }
    return timestamps;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private List<Timestamp> annotate() throws Exception {
    ConsoleLogParser logParser = new MockConsoleLogParser();
    ConsoleAnnotator annotator = new TimestampAnnotator(logParser);
    Supplier<TimestampFormat> originalSupplier =
        Whitebox.getInternalState(TimestampFormatProvider.class, Supplier.class);

    captureFormattedTimestamps();
    try {
      int iterations = 0;
      while (annotator != null) {
        if (serialize) {
          annotator = (ConsoleAnnotator) SerializationUtils.clone(annotator);
        }
        annotator = annotator.annotate(build, mock(MarkupText.class));
        iterations++;
        if (iterations > 100) {
          throw new AssertionError("annotator is not terminating");
        }
      }
    } finally {
      Whitebox.setInternalState(TimestampFormatProvider.class, Supplier.class, originalSupplier);
    }
    return capturedTimestamps;
  }

  private void captureFormattedTimestamps() {
    final TimestampFormat format = mock(TimestampFormat.class);
    doAnswer(
            new Answer<Void>() {

              @Override
              public Void answer(InvocationOnMock invocation) throws Throwable {
                Timestamp timestamp = (Timestamp) invocation.getArguments()[1];
                capturedTimestamps.add(timestamp);
                return null;
              }
            })
        .when(format)
        .markup(any(MarkupText.class), any(Timestamp.class));
    Whitebox.setInternalState(
        TimestampFormatProvider.class,
        Supplier.class,
        new Supplier<TimestampFormat>() {
          @Override
          public TimestampFormat get() {
            return format;
          }
        });
  }

  private static class MockConsoleLogParser extends ConsoleLogParser {

    private static final long serialVersionUID = 1L;

    MockConsoleLogParser() {
      super(0);
    }

    @Override
    public Result seek(Run<?, ?> build) throws IOException {
      return logPosition;
    }
  }
}
