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
package hudson.plugins.timestamper;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.model.AbstractBuild;
import hudson.plugins.timestamper.io.TimestampsWriter;

import java.io.OutputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit test for the TimestamperOutputStream class.
 * 
 * @author Steven G. Brown
 */
public class TimestamperOutputStreamTest {

  private static final char NEWLINE = 0x0A;

  /**
   */
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private AbstractBuild<?, ?> build;

  private OutputStream delegate;

  private TimestampsWriter writer;

  private OutputStream timestamperOutputStream;

  private byte[] data;

  private byte[] dataTwoLines;

  /**
   */
  @Before
  public void setUp() {
    build = mock(AbstractBuild.class);
    when(build.getRootDir()).thenReturn(folder.getRoot());
    delegate = mock(OutputStream.class);
    writer = mock(TimestampsWriter.class);
    timestamperOutputStream = new TimestamperOutputStream(build, delegate,
        writer);
    data = new byte[] { 'a', (byte) NEWLINE };
    dataTwoLines = new byte[] { 'a', (byte) NEWLINE, 'b', (byte) NEWLINE };
  }

  /**
   * @throws Exception
   */
  @Test
  public void testPassThrough() throws Exception {
    timestamperOutputStream.write(data);
    verify(delegate).write(data);
    timestamperOutputStream.write(data, 0, 1);
    verify(delegate).write(data, 0, 1);
    timestamperOutputStream.write(42);
    verify(delegate).write(42);
    timestamperOutputStream.flush();
    verify(delegate).flush();
    timestamperOutputStream.close();
    verify(delegate).close();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteIntOneCharacter() throws Exception {
    timestamperOutputStream.write('a');
    verify(writer).write(anyLong(), eq(1));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteIntOneLine() throws Exception {
    timestamperOutputStream.write('a');
    timestamperOutputStream.write(NEWLINE);
    verify(writer).write(anyLong(), eq(1));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteIntTwoLines() throws Exception {
    timestamperOutputStream.write('a');
    timestamperOutputStream.write(NEWLINE);
    timestamperOutputStream.write('b');
    verify(writer, times(2)).write(anyLong(), eq(1));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteByteArray() throws Exception {
    timestamperOutputStream.write(data);
    verify(writer).write(anyLong(), eq(1));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteByteArrayTwoLines() throws Exception {
    timestamperOutputStream.write(dataTwoLines);
    verify(writer).write(anyLong(), eq(2));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteByteArraySegment() throws Exception {
    timestamperOutputStream.write(dataTwoLines, 0, data.length);
    verify(writer).write(anyLong(), eq(1));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteByteArraySegmentTwoLines() throws Exception {
    timestamperOutputStream.write(dataTwoLines, 0, dataTwoLines.length);
    verify(writer).write(anyLong(), eq(2));
  }
}
