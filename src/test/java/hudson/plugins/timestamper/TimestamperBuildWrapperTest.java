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

import static hudson.plugins.timestamper.TimestamperTestAssistant.readAllTimestamps;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.model.AbstractBuild;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test for the {@link TimestamperBuildWrapper} class.
 * 
 * @author Steven G. Brown
 */
@SuppressWarnings("boxing")
public class TimestamperBuildWrapperTest {

  private static final char NEWLINE = 0x0A;

  /**
   */
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private TimestamperBuildWrapper buildWrapper;

  private AbstractBuild<?, ?> build;

  private OutputStream outputStream;

  private OutputStream decoratedOutputStream;

  private byte[] data;

  private byte[] dataTwoLines;

  /**
   */
  @Before
  public void setUp() {
    buildWrapper = new TimestamperBuildWrapper();
    build = mock(AbstractBuild.class);
    when(build.getRootDir()).thenReturn(folder.getRoot());
    outputStream = mock(OutputStream.class);
    decoratedOutputStream = buildWrapper.decorateLogger(build, outputStream);
    data = new byte[] { 'a', (byte) NEWLINE };
    dataTwoLines = new byte[] { 'a', (byte) NEWLINE, 'b', (byte) NEWLINE };
  }

  /**
   */
  @After
  public void tearDown() {
    System.clearProperty(TimestampNote.getSystemProperty());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testTimestampNoteSystemProperty() throws Exception {
    System.setProperty(TimestampNote.getSystemProperty(), "true");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    OutputStream decoratedOutputStream = buildWrapper.decorateLogger(build,
        outputStream);
    decoratedOutputStream.write(data);
    assertThat(ArrayUtils.toObject(outputStream.toByteArray()),
        is(arrayWithSize(greaterThan(data.length))));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testPassThrough() throws Exception {
    decoratedOutputStream.write(data);
    verify(outputStream).write(data);
    decoratedOutputStream.write(data, 0, 1);
    verify(outputStream).write(data, 0, 1);
    decoratedOutputStream.write(42);
    verify(outputStream).write(42);
    decoratedOutputStream.flush();
    verify(outputStream).flush();
    decoratedOutputStream.close();
    verify(outputStream).close();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteIntOneCharacter() throws Exception {
    decoratedOutputStream.write('a');
    assertThat(readAllTimestamps(build), hasSize(1));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteIntOneLine() throws Exception {
    decoratedOutputStream.write('a');
    decoratedOutputStream.write(NEWLINE);
    assertThat(readAllTimestamps(build), hasSize(1));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteIntTwoLines() throws Exception {
    decoratedOutputStream.write('a');
    decoratedOutputStream.write(NEWLINE);
    decoratedOutputStream.write('b');
    assertThat(readAllTimestamps(build), hasSize(2));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteByteArray() throws Exception {
    decoratedOutputStream.write(data);
    assertThat(readAllTimestamps(build), hasSize(1));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteByteArrayTwoLines() throws Exception {
    decoratedOutputStream.write(dataTwoLines);
    List<Timestamp> timestamps = readAllTimestamps(build);
    assertThat(timestamps, hasSize(2));
    assertThat(timestamps.get(0), is(timestamps.get(1)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteByteArraySegment() throws Exception {
    decoratedOutputStream.write(dataTwoLines, 0, data.length);
    assertThat(readAllTimestamps(build), hasSize(1));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteByteArraySegmentTwoLines() throws Exception {
    decoratedOutputStream.write(dataTwoLines, 0, dataTwoLines.length);
    List<Timestamp> timestamps = readAllTimestamps(build);
    assertThat(timestamps, hasSize(2));
    assertThat(timestamps.get(0), is(timestamps.get(1)));
  }
}
