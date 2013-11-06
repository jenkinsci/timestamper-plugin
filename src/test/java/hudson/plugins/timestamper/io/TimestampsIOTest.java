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
package hudson.plugins.timestamper.io;

import static hudson.plugins.timestamper.TimestamperTestAssistant.readAllTimestamps;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;

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
import org.powermock.reflect.Whitebox;

import com.google.common.base.Function;
import com.google.common.base.Functions;

/**
 * Unit test for the {@link TimestampsReader} and {@link TimestampsWriterImpl}
 * classes.
 * 
 * @author Steven G. Brown
 */
public class TimestampsIOTest {

  // start
  private static final Timestamp timestampOne = new Timestamp(0, 0);

  // time shift into future
  private static final Timestamp timestampTwo = new Timestamp(500, 10000);

  // no time shift
  private static final Timestamp timestampThree = new Timestamp(1500, 11000);

  // duplicate of time-stamp three
  private static final Timestamp timestampFour = timestampThree;

  // time shift into past
  private static final Timestamp timestampFive = new Timestamp(2000, 10000);

  private static final List<Timestamp> timestamps = Collections
      .unmodifiableList(Arrays.asList(timestampOne, timestampTwo,
          timestampThree, timestampFour, timestampFive));

  private static final Function<TimestampsReader, TimestampsReader> serializeReader = new Function<TimestampsReader, TimestampsReader>() {
    public TimestampsReader apply(TimestampsReader reader) {
      return (TimestampsReader) SerializationUtils.clone(reader);
    }
  };

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
    assertThat(readAllTimestamps(build, serializeReader), is(timestamps));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testReadFromStartWhileWriting() throws Exception {
    testReadFromStartWhileWriting(Functions.<TimestampsReader> identity());
  }

  /**
   * @throws Exception
   */
  @Test
  public void testReadFromStartWhileWritingWithSerialization() throws Exception {
    testReadFromStartWhileWriting(serializeReader);
  }

  private void testReadFromStartWhileWriting(
      Function<TimestampsReader, TimestampsReader> readerTransformer)
      throws Exception {
    TimestampsWriter writer = new TimestampsWriterImpl(build);
    TimestampsReader reader = new TimestampsReader(build);
    try {
      writeTimestamp(timestampOne, 1, writer);
      reader = readerTransformer.apply(reader);
      assertThat(reader.next(), is(timestampOne));
      writeTimestamp(timestampTwo, 1, writer);
      reader = readerTransformer.apply(reader);
      assertThat(reader.next(), is(timestampTwo));
      writeTimestamp(timestampThree, 2, writer);
      reader = readerTransformer.apply(reader);
      assertThat(reader.next(), is(timestampThree));
      assertThat(reader.next(), is(timestampFour));
      writeTimestamp(timestampFive, 1, writer);
      reader = readerTransformer.apply(reader);
      assertThat(reader.next(), is(timestampFive));
    } finally {
      writer.close();
    }
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWriteSameTimestampManyTimes() throws Exception {
    int bufferSize = Whitebox.getField(TimestampsWriterImpl.class,
        "BUFFER_SIZE").getInt(null);
    int numberOfTimestamps = bufferSize + 1000; // larger than the buffer
    Timestamp timestamp = new Timestamp(10000, 10000);
    TimestampsWriter writer = new TimestampsWriterImpl(build);
    try {
      writeTimestamp(timestamp, numberOfTimestamps, writer);
    } finally {
      writer.close();
    }
    List<Timestamp> readTimestamps = readAllTimestamps(build);
    assertThat(readTimestamps, everyItem(equalTo(new Timestamp(0, 10000))));
    assertThat(readTimestamps, hasSize(numberOfTimestamps));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSkipZero() throws Exception {
    writeTimestamps();
    testSkip(0);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSkipOne() throws Exception {
    writeTimestamps();
    testSkip(1);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSkipTwo() throws Exception {
    writeTimestamps();
    testSkip(2);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSkipPastEnd() throws Exception {
    writeTimestamps();
    TimestampsReader reader = new TimestampsReader(build);
    reader.skip(timestamps.size());
    assertThat(reader.next(), is(nullValue()));
  }

  private void testSkip(int count) throws Exception {
    TimestampsReader reader = new TimestampsReader(build);
    List<Timestamp> timestampsRead = new ArrayList<Timestamp>();
    reader.skip(count);
    for (int i = 0; i < timestamps.size() - count; i++) {
      timestampsRead.add(reader.next());
    }
    assertThat(timestampsRead, is(timestamps.subList(count, timestamps.size())));
    assertThat(reader.next(), is(nullValue()));
  }

  private void writeTimestamps() throws Exception {
    TimestampsWriter writer = new TimestampsWriterImpl(build);
    try {
      writeTimestamp(timestampOne, 1, writer);
      writeTimestamp(timestampTwo, 1, writer);
      writeTimestamp(timestampThree, 2, writer);
      writeTimestamp(timestampFive, 1, writer);
    } finally {
      writer.close();
    }
  }

  private void writeTimestamp(Timestamp timestamp, int times,
      TimestampsWriter writer) throws Exception {
    long startNanos = 100;
    long nanoTime = TimeUnit.MILLISECONDS.toNanos(timestamp.elapsedMillis)
        + startNanos;
    writer.write(nanoTime, timestamp.millisSinceEpoch, times);
  }
}
