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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.Run;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Function;

/**
 * Unit test for the {@link TimestampsIO} class.
 * 
 * @author Steven G. Brown
 */
public class TimestampsIOTest {

  // start
  private static final Timestamp timestampOne = new Timestamp(0, 0);

  // time shift
  private static final Timestamp timestampTwo = new Timestamp(500, 10000);

  // no time shift
  private static final Timestamp timestampThree = new Timestamp(1500, 11000);

  private static final List<Timestamp> timestamps = Collections
      .unmodifiableList(Arrays.asList(timestampOne, timestampTwo,
          timestampThree));

  /**
   */
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private Run<?, ?> build;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    build = mock(Run.class);
    when(build.getRootDir()).thenReturn(folder.getRoot());
    byte[] consoleLog = new byte[] { 0x61, 0x0A, 0x61, 0x0A, 0x61, 0x0A };
    when(build.getLogInputStream()).thenReturn(
        new ByteArrayInputStream(consoleLog));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testReadFromStart() throws Exception {
    writeTimestamps();
    assertThat(readAllTimestamps(build), is(timestamps));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testReadFromStartWithSerialization() throws Exception {
    writeTimestamps();
    Function<TimestampsIO.Reader, TimestampsIO.Reader> transformer = new Function<TimestampsIO.Reader, TimestampsIO.Reader>() {
      public TimestampsIO.Reader apply(TimestampsIO.Reader reader) {
        return (TimestampsIO.Reader) SerializationUtils.clone(reader);
      }
    };
    assertThat(readAllTimestamps(build, transformer), is(timestamps));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFindTimestampOne() throws Exception {
    writeTimestamps();
    testFind(0);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFindTimestampTwo() throws Exception {
    writeTimestamps();
    testFind(2);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFindTimestampThree() throws Exception {
    writeTimestamps();
    testFind(4);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFindWithinTimestampTwo() throws Exception {
    writeTimestamps();
    TimestampsIO.Reader reader = new TimestampsIO.Reader(build);
    assertThat(reader.find(3, build), is(nullValue()));
    assertThat(reader.next(), is(timestampThree));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testFindPastEnd() throws Exception {
    writeTimestamps();
    TimestampsIO.Reader reader = new TimestampsIO.Reader(build);
    assertThat(reader.find(5, build), is(nullValue()));
    assertThat(reader.next(), is(nullValue()));
  }

  private void testFind(long consoleFilePointer) throws Exception {
    TimestampsIO.Reader reader = new TimestampsIO.Reader(build);
    List<Timestamp> timestampsRead = new ArrayList<Timestamp>();
    timestampsRead.add(reader.find(consoleFilePointer, build));
    for (int i = 0; i < timestamps.size() - consoleFilePointer / 2 - 1; i++) {
      timestampsRead.add(reader.next());
    }
    assertThat(timestampsRead,
        is(timestamps.subList((int) consoleFilePointer / 2, timestamps.size())));
    assertThat(reader.next(), is(nullValue()));
  }

  private void writeTimestamps() throws Exception {
    TimestampsIO.Writer writer = new TimestampsIO.Writer(build);
    long startNanos = 100;
    try {
      for (Timestamp timestamp : timestamps) {
        long nanoTime = TimeUnit.MILLISECONDS.toNanos(timestamp.elapsedMillis)
            + startNanos;
        writer.write(nanoTime, timestamp.millisSinceEpoch, 1);
      }
    } finally {
      writer.close();
    }
  }
}
