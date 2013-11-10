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
package hudson.plugins.timestamper.io;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.Run;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.powermock.reflect.Whitebox;

import com.google.common.io.Files;

/**
 * Unit test for the {@link TimestampsWriterImpl} class.
 * 
 * @author Steven G. Brown
 */
public class TimestampsWriterImplTest {

  /**
   */
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private Run<?, ?> build;

  private TimestampsWriterImpl timestampsWriter;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    build = mock(Run.class);
    when(build.getRootDir()).thenReturn(folder.getRoot());
    timestampsWriter = new TimestampsWriterImpl(build);
  }

  /**
   * @throws Exception
   */
  @After
  public void tearDown() throws Exception {
    timestampsWriter.close();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteIncreasing() throws Exception {
    timestampsWriter.write(1, 1);
    timestampsWriter.write(2, 1);
    timestampsWriter.write(3, 1);
    assertThat(writtenTimestampData(), is(Arrays.asList(1, 1, 1)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteDecreasing() throws Exception {
    timestampsWriter.write(3, 1);
    timestampsWriter.write(2, 1);
    timestampsWriter.write(1, 1);
    assertThat(writtenTimestampData(), is(Arrays.asList(3, -1, -1)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteZeroTimes() throws Exception {
    timestampsWriter.write(0, 0);
    assertThat(writtenTimestampData(), is(Collections.<Integer> emptyList()));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteSeveralTimes() throws Exception {
    timestampsWriter.write(1, 1);
    timestampsWriter.write(6, 4);
    timestampsWriter.write(7, 1);
    assertThat(writtenTimestampData(), is(Arrays.asList(1, 5, 0, 0, 0, 1)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteSameTimestampManyTimes() throws Exception {
    int bufferSize = Whitebox.getField(TimestampsWriterImpl.class,
        "BUFFER_SIZE").getInt(null);
    int times = bufferSize + 1000; // larger than the buffer
    timestampsWriter.write(10000, times);
    List<Integer> writtenTimestampData = writtenTimestampData();
    assertThat(writtenTimestampData.get(0), is(10000));
    assertThat(writtenTimestampData.subList(1, writtenTimestampData.size()),
        everyItem(equalTo(0)));
    assertThat(writtenTimestampData, hasSize(times));
  }

  private List<Integer> writtenTimestampData() throws Exception {
    File timestamperDir = TimestampsWriterImpl.timestamperDir(build);
    File timestampsFile = TimestampsWriterImpl.timestampsFile(timestamperDir);
    byte[] fileContents = Files.toByteArray(timestampsFile);
    TimestampsReader.InputStreamByteReader byteReader = new TimestampsReader.InputStreamByteReader(
        new ByteArrayInputStream(fileContents));
    List<Integer> timestampData = new ArrayList<Integer>();
    while (byteReader.bytesRead < fileContents.length) {
      timestampData.add((int) Varint.read(byteReader));
    }
    return timestampData;
  }
}
