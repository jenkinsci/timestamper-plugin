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

import com.google.common.io.ByteStreams;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Optional;
import org.apache.commons.io.input.CountingInputStream;

/**
 * Read the time-stamps for a build from disk.
 *
 * @author Steven G. Brown
 */
public class TimestampsReader implements Serializable, Closeable {

  private static final long serialVersionUID = 1L;

  private final File timestampsFile;

  private long filePointer;

  private long elapsedMillis;

  private long millisSinceEpoch;

  private long entry;

  private final TimeShiftsReader timeShiftsReader;

  @CheckForNull private transient InputStream inputStream;

  /** Create a time-stamps reader for the given build. */
  public TimestampsReader(Run<?, ?> build) {
    this.timestampsFile = TimestamperPaths.timestampsFile(build).toFile();
    this.timeShiftsReader = new TimeShiftsReader(build);
    this.millisSinceEpoch = build.getStartTimeInMillis();
  }

  /**
   * Skip past the given number of time-stamp entries.
   *
   * @param count the number of time-stamp entries to skip
   */
  public void skip(int count) throws IOException {
    for (int i = 0; i < count; i++) {
      Optional<Timestamp> timestamp = read();
      if (!timestamp.isPresent()) {
        return;
      }
    }
  }

  /**
   * Convert negative line number that was calculated from end of file to absolute line number (from
   * head)
   *
   * @param lineNumber line number (should be negative)
   * @return absolute line
   */
  public int getAbs(int lineNumber) throws IOException {
    skip(-lineNumber);

    int numberOfTimestampsFromStart = 0;
    while (true) {
      Optional<Timestamp> timestamp = read();
      if (!timestamp.isPresent()) {
        return numberOfTimestampsFromStart;
      }
      numberOfTimestampsFromStart++;
    }
  }

  /**
   * Read the next time-stamp.
   *
   * @return the next time-stamp, or {@link Optional#empty()} if there are no more to read
   */
  public Optional<Timestamp> read() throws IOException {
    if (inputStream == null) {
      if (!Files.isRegularFile(timestampsFile.toPath())) {
        return Optional.empty();
      }
      inputStream = Files.newInputStream(timestampsFile.toPath());
      ByteStreams.skipFully(inputStream, filePointer);
      inputStream = new BufferedInputStream(inputStream);
    }
    Optional<Timestamp> timestamp = Optional.empty();
    if (filePointer < Files.size(timestampsFile.toPath())) {
      timestamp = Optional.of(readNext(inputStream));
    }
    return timestamp;
  }

  /** Close this reader. */
  @Override
  public void close() {
    try {
      if (inputStream != null) {
        inputStream.close();
      }
    } catch (IOException e) {
      // ignore
    }
    inputStream = null;
  }

  /**
   * Read the next time-stamp from the given input stream.
   *
   * @return the next time-stamp
   */
  private Timestamp readNext(InputStream inputStream) throws IOException {
    CountingInputStream countingInputStream = new CountingInputStream(inputStream);
    long elapsedMillisDiff = Varint.read(countingInputStream);

    elapsedMillis += elapsedMillisDiff;
    millisSinceEpoch = timeShiftsReader.getTime(entry).orElse(millisSinceEpoch + elapsedMillisDiff);
    filePointer += countingInputStream.getCount();
    entry++;
    return new Timestamp(elapsedMillis, millisSinceEpoch);
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    try {
      if (inputStream != null) {
        inputStream.close();
      }
    } catch (IOException e) {
      // ignore
    }
  }
}
