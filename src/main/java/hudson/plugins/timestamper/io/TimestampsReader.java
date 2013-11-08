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
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

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

  private final File timeShiftsFile;

  /**
   * Last known length of the {@link #timeShiftsFile}. This value is used to
   * detect whether the file has changed. This is sufficient because the file
   * never shrinks; new data is always appended to the end of the file.
   * <p>
   * Transient: after serializing, the {@link #timeShiftsFile} will be re-read.
   */
  private transient long timeShiftsFileLength;

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
    File timestamperDir = TimestampsWriterImpl.timestamperDir(build);
    this.timestampsFile = TimestampsWriterImpl.timestampsFile(timestamperDir);
    this.timeShiftsFile = TimestampsWriterImpl.timeShiftsFile(timestamperDir);
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
    RandomAccessFile raf = openTimestampsFile();
    boolean threw = true;
    try {
      for (int i = 0; i < count; i++) {
        next(raf);
      }
      threw = false;
    } finally {
      Closeables.close(raf, threw);
    }
  }

  /**
   * Read the next time-stamp.
   * 
   * @return the next time-stamp
   * @throws IOException
   */
  public Timestamp next() throws IOException {
    RandomAccessFile raf = openTimestampsFile();
    Timestamp timestamp;
    boolean threw = true;
    try {
      timestamp = next(raf);
      threw = false;
    } finally {
      Closeables.close(raf, threw);
    }
    return timestamp;
  }

  /**
   * Read the next time-stamp by using an existing {@link RandomAccessFile}.
   */
  private Timestamp next(final RandomAccessFile raf) throws IOException {
    if (raf == null) {
      return null;
    }
    Varint.ByteReader byteReader = new Varint.ByteReader() {
      @Override
      public byte readByte() throws IOException {
        return raf.readByte();
      }
    };

    raf.seek(filePointer);

    long elapsedMillisDiff = Varint.read(byteReader);
    elapsedMillis += elapsedMillisDiff;

    timeShifts = readTimeShifts();
    if (timeShifts.containsKey(entry)) {
      millisSinceEpoch = timeShifts.get(entry);
    } else {
      millisSinceEpoch += elapsedMillisDiff;
    }

    filePointer = raf.getFilePointer();
    entry++;
    return new Timestamp(elapsedMillis, millisSinceEpoch);
  }

  private RandomAccessFile openTimestampsFile() throws FileNotFoundException {
    if (!timestampsFile.isFile() || filePointer >= timestampsFile.length()) {
      return null;
    }
    return new RandomAccessFile(timestampsFile, "r");
  }

  private Map<Long, Long> readTimeShifts() throws IOException {
    if (!timeShiftsFile.isFile()) {
      return Collections.emptyMap();
    }
    if (timeShiftsFile.length() == timeShiftsFileLength) {
      return Objects.firstNonNull(timeShifts,
          Collections.<Long, Long> emptyMap());
    }
    timeShiftsFileLength = timeShiftsFile.length();
    Map<Long, Long> timeShifts = new HashMap<Long, Long>();
    BufferedInputStream inputStream = null;
    boolean threw = true;
    try {
      inputStream = new BufferedInputStream(new FileInputStream(timeShiftsFile));
      InputStreamByteReader byteReader = new InputStreamByteReader(inputStream);
      while (byteReader.bytesRead < timeShiftsFileLength) {
        long entry = Varint.read(byteReader);
        long shift = Varint.read(byteReader);
        timeShifts.put(entry, shift);
      }
      threw = false;
    } finally {
      Closeables.close(inputStream, threw);
    }
    return timeShifts;
  }

  private static class InputStreamByteReader implements Varint.ByteReader {
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

  /**
   * Read the values from the timestamper directory path given by the
   * command-line arguments and output these values to the console. This is
   * intended only for debugging. It is not invoked by Jenkins.
   * 
   * @param args
   *          the command-line arguments, expected to contain a timestamper
   *          directory path
   * @throws IOException
   */
  public static void main(String... args) throws IOException {
    if (args.length == 0) {
      throw new IllegalArgumentException("no command-line arguments");
    }
    File timestamperDir = new File(Joiner.on(' ').join(args));
    System.out.println("timestamps");
    dump(TimestampsWriterImpl.timestampsFile(timestamperDir), 1, System.out);
    File timeShiftsFile = TimestampsWriterImpl.timeShiftsFile(timestamperDir);
    if (timeShiftsFile.isFile()) {
      System.out.println("timeshifts");
      dump(timeShiftsFile, 2, System.out);
    }
  }

  private static void dump(File file, int columns, PrintStream output)
      throws IOException {
    final byte[] fileContents = Files.toByteArray(file);
    InputStreamByteReader byteReader = new InputStreamByteReader(
        new ByteArrayInputStream(fileContents));
    List<Long> values = new ArrayList<Long>();
    while (byteReader.bytesRead < fileContents.length) {
      values.add(Varint.read(byteReader));
      if (values.size() == columns) {
        output.println(Joiner.on('\t').join(values));
        values.clear();
      }
    }
    if (!values.isEmpty()) {
      output.println(Joiner.on('\t').join(values));
    }
    output.println();
  }
}