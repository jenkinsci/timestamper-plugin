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

import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.common.io.CountingInputStream;

/**
 * Read the time-stamps for a build from disk.
 * 
 * @author Steven G. Brown
 */
public final class TimestampsReader implements Serializable {

  private static final long serialVersionUID = 1L;

  private final File timestampsFile;

  private long filePointer;

  private long elapsedMillis;

  private long millisSinceEpoch;

  private long entry;

  private final TimeShiftsReader timeShiftsReader;

  /**
   * Create a time-stamps reader for the given build.
   * 
   * @param build
   */
  public TimestampsReader(Run<?, ?> build) {
    this.timestampsFile = TimestamperPaths.timestampsFile(build);
    this.timeShiftsReader = new TimeShiftsReader(build);
    this.millisSinceEpoch = build.getTimeInMillis();
  }

  /**
   * Skip past the given number of time-stamp entries.
   * 
   * @param count
   *          the number of time-stamp entries to skip
   * @throws IOException
   */
  public void skip(int count) throws IOException {
    read(count, Optional.<List<Timestamp>> absent());
  }

  /**
   * Read the next time-stamp.
   * 
   * @return the next time-stamp, or {@link Optional#absent()} if there are no
   *         more to read
   * @throws IOException
   */
  public Optional<Timestamp> read() throws IOException {
    List<Timestamp> timestamps = new ArrayList<Timestamp>();
    read(1, Optional.of(timestamps));
    return Optional.fromNullable(Iterators.getOnlyElement(
        timestamps.iterator(), null));
  }

  /**
   * Read several time-stamps.
   * 
   * @param count
   *          the number of time-stamps to read
   * @return a list containing {@code count} time-stamps, or fewer if there are
   *         no more to read
   * @throws IOException
   */
  public List<Timestamp> read(int count) throws IOException {
    List<Timestamp> timestamps = new ArrayList<Timestamp>();
    read(count, Optional.of(timestamps));
    return ImmutableList.copyOf(timestamps);
  }

  /**
   * Read up to {@code count} time-stamps and add them to the given list.
   * 
   * @param count
   *          the number of time-stamps to read
   * @param timestamps
   *          the list that will contain the time-stamps (optional)
   * @throws IOException
   */
  private void read(int count, Optional<List<Timestamp>> timestamps)
      throws IOException {
    if (count < 1 || !timestampsFile.isFile()) {
      return;
    }
    InputStream inputStream = new FileInputStream(timestampsFile);
    boolean threw = true;
    try {
      ByteStreams.skipFully(inputStream, filePointer);
      inputStream = new BufferedInputStream(inputStream);
      int i = 0;
      while (i < count && filePointer < timestampsFile.length()) {
        Timestamp timestamp = readNext(inputStream);
        if (timestamps.isPresent()) {
          timestamps.get().add(timestamp);
        }
        i++;
      }
      threw = false;
    } finally {
      Closeables.close(inputStream, threw);
    }
  }

  /**
   * Read the next time-stamp from the given input stream.
   * 
   * @param inputStream
   * @return the next time-stamp
   */
  private Timestamp readNext(InputStream inputStream) throws IOException {
    CountingInputStream countingInputStream = new CountingInputStream(
        inputStream);
    long elapsedMillisDiff = Varint.read(countingInputStream);

    elapsedMillis += elapsedMillisDiff;
    millisSinceEpoch = timeShiftsReader.getTime(entry).or(
        millisSinceEpoch + elapsedMillisDiff);
    filePointer += countingInputStream.getCount();
    entry++;
    return new Timestamp(elapsedMillis, millisSinceEpoch);
  }
}