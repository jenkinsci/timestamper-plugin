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

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.OutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import hudson.plugins.timestamper.io.TimestampsWriter;

/**
 * Unit test for the TimestamperOutputStream class.
 * 
 * @author Steven G. Brown
 */
public class TimestamperOutputStreamTest {

  private static final char NEWLINE = 0x0A;

  private OutputStream delegate;

  private TimestampsWriter writer;

  private OutputStream timestamperOutputStream;

  private byte[] data;

  private byte[] dataTwoLines;

  /**
   */
  @Before
  public void setUp() {
    delegate = mock(OutputStream.class);
    writer = mock(TimestampsWriter.class);
    timestamperOutputStream = new TimestamperOutputStream(delegate, writer);
    data = new byte[] { 'a', (byte) NEWLINE };
    dataTwoLines = new byte[] { 'a', (byte) NEWLINE, 'b', (byte) NEWLINE };
  }

  /**
   */
  @After
  public void tearDown() throws Exception {
    timestamperOutputStream.close();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testPassThroughWriteByteArray() throws Exception {
    timestamperOutputStream.write(data);
    verify(delegate).write(data);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testPassThroughWriteByteArrayWithOffset() throws Exception {
    timestamperOutputStream.write(data, 0, 1);
    verify(delegate).write(data, 0, 1);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testPassThroughWriteByte() throws Exception {
    timestamperOutputStream.write(42);
    verify(delegate).write(42);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testPassThroughFlush() throws Exception {
    timestamperOutputStream.flush();
    verify(delegate).flush();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testPassThroughClose() throws Exception {
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

  /**
   * @throws Exception
   */
  @Test
  public void testNoWritesAfterError() throws Exception {
    doThrow(new IOException()).when(writer).write(anyLong(), anyInt());
    timestamperOutputStream.write(data);
    timestamperOutputStream.write(data);
    verify(writer, times(1)).write(anyLong(), anyInt());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteDigest() throws Exception {
    timestamperOutputStream.close();
    verify(writer).writeDigest();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testNoDigestAfterWriteError() throws Exception {
    doThrow(new IOException()).when(writer).write(anyLong(), anyInt());
    timestamperOutputStream.write(data);
    timestamperOutputStream.close();
    verify(writer, never()).writeDigest();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testNoDigestAfterCloseError() throws Exception {
    doThrow(new IOException()).when(writer).close();
    timestamperOutputStream.close();
    verify(writer, never()).writeDigest();
  }
}
