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
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;

/**
 * Read the time-stamps for a build from disk.
 * 
 * @author Steven G. Brown
 */
public final class TimestampsReader implements Serializable {

  private static final long serialVersionUID = 1L;

  static File timeShiftsFile(File timestamperDir) {
    return new File(timestamperDir, "timeshifts");
  }

  private final File timestampsFile;

  private long filePointer;

  private long elapsedMillis;

  private long millisSinceEpoch;

  private long entry;

  private final File timeShiftsFile;

  /**
   * Cache of the time shifts for each entry.
   * <p>
   * Transient: derived from the contents of {@link #timeShiftsFile}.
   */
  private transient Map<Long, Long> timeShifts;

  /**
   * Create a time-stamps reader for the given build.
   * 
   * @param build
   */
  public TimestampsReader(Run<?, ?> build) {
    File timestamperDir = TimestampsWriter.timestamperDir(build);
    this.timestampsFile = TimestampsWriter.timestampsFile(timestamperDir);
    this.timeShiftsFile = timeShiftsFile(timestamperDir);
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
    InputStream inputStream = null;
    boolean threw = true;
    try {
      inputStream = openTimestampsStream();
      for (int i = 0; i < count; i++) {
        next(inputStream);
      }
      threw = false;
    } finally {
      Closeables.close(inputStream, threw);
    }
  }

  /**
   * Read the next time-stamp.
   * 
   * @return the next time-stamp
   * @throws IOException
   */
  public Timestamp next() throws IOException {
    InputStream inputStream = null;
    Timestamp timestamp;
    boolean threw = true;
    try {
      inputStream = openTimestampsStream();
      timestamp = next(inputStream);
      threw = false;
    } finally {
      Closeables.close(inputStream, threw);
    }
    return timestamp;
  }

  /**
   * Read the next time-stamp by using an existing {@link InputStream}.
   */
  private Timestamp next(InputStream inputStream) throws IOException {
    if (inputStream == null || filePointer >= timestampsFile.length()) {
      return null;
    }
    InputStreamByteReader byteReader = new InputStreamByteReader(inputStream);
    long elapsedMillisDiff = Varint.read(byteReader);

    elapsedMillis += elapsedMillisDiff;
    millisSinceEpoch += elapsedMillisDiff;
    applyTimeShift();
    filePointer += byteReader.bytesRead;
    entry++;
    return new Timestamp(elapsedMillis, millisSinceEpoch);
  }

  private InputStream openTimestampsStream() throws IOException {
    if (!timestampsFile.isFile()) {
      return null;
    }
    InputStream inputStream = new FileInputStream(timestampsFile);
    ByteStreams.skipFully(inputStream, filePointer);
    return inputStream;
  }

  private void applyTimeShift() throws IOException {
    if (timeShifts == null) {
      timeShifts = readTimeShifts();
    }
    millisSinceEpoch = Objects.firstNonNull(timeShifts.get(entry),
        millisSinceEpoch);
  }

  private Map<Long, Long> readTimeShifts() throws IOException {
    if (!timeShiftsFile.isFile()) {
      return Collections.emptyMap();
    }
    Map<Long, Long> timeShifts = new HashMap<Long, Long>();
    BufferedInputStream inputStream = null;
    boolean threw = true;
    try {
      inputStream = new BufferedInputStream(new FileInputStream(timeShiftsFile));
      InputStreamByteReader byteReader = new InputStreamByteReader(inputStream);
      while (byteReader.bytesRead < timeShiftsFile.length()) {
        long entry = Varint.read(byteReader);
        long shift = Varint.read(byteReader);
        timeShifts.put(entry, shift);
      }
      threw = false;
    } finally {
      Closeables.close(inputStream, threw);
    }
    return ImmutableMap.copyOf(timeShifts);
  }

  static class InputStreamByteReader implements Varint.ByteReader {
    InputStream inputStream;
    long bytesRead;

    InputStreamByteReader(InputStream inputStream) {
      this.inputStream = inputStream;
    }

    @Override
    public byte readByte() throws IOException {
      int b = inputStream.read();
      if (b == -1) {
        throw new EOFException();
      }
      bytesRead++;
      return (byte) b;
    }
  }
}