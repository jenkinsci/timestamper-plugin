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

import hudson.model.Run;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.mutable.MutableLong;

import com.google.common.io.Closeables;
import com.google.common.io.Files;

/**
 * Allows the time-stamps for a build to be written to disk and read back again.
 * 
 * @author Steven G. Brown
 * @since 1.4
 */
public class TimestampsIO {

  private static final Logger LOGGER = Logger.getLogger(TimestampsIO.class
      .getName());

  private static File timestamperDir(Run<?, ?> build) {
    return new File(build.getRootDir(), "timestamper");
  }

  private static File timestampsFile(Run<?, ?> build) {
    return new File(timestamperDir(build), "timestamps");
  }

  private static File timeShiftsFile(Run<?, ?> build) {
    return new File(timestamperDir(build), "timeshifts");
  }

  private static final int BUFFER_SIZE = 1024;

  /**
   * Writer for time-stamps.
   */
  public static class Writer {

    private final File timestampsFile;

    private final File timeShiftsFile;

    private final Map<File, FileOutputStream> outputStreams = new HashMap<File, FileOutputStream>();

    private long entry;

    /**
     * Buffer that is used to store Varints prior to writing to a file.
     */
    private final byte[] buffer = new byte[BUFFER_SIZE];

    /**
     * Current offset into the {@link #buffer}.
     */
    private int bufferOffset;

    private long startNanos;

    private long previousElapsedMillis;

    private long previousCurrentTimeMillis;

    /**
     * Create a time-stamps writer for the given build.
     * 
     * @param build
     */
    public Writer(Run<?, ?> build) {
      this.timestampsFile = timestampsFile(build);
      this.timeShiftsFile = timeShiftsFile(build);
      this.previousCurrentTimeMillis = build.getTimeInMillis();
    }

    /**
     * Write a time-stamp for a line of the console log.
     * 
     * @param nanoTime
     *          {@link System#nanoTime()}
     * @param currentTimeMillis
     *          {@link System#currentTimeMillis()}
     * @param times
     *          the number of times to write the time-stamp
     * @throws IOException
     */
    public void write(long nanoTime, long currentTimeMillis, int times)
        throws IOException {
      if (times < 1) {
        return;
      }

      // Write to time-stamps file.
      if (entry == 0) {
        startNanos = nanoTime;
      }
      long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(nanoTime - startNanos);
      long elapsedMillisDiff = elapsedMillis - previousElapsedMillis;
      writeVarint(elapsedMillisDiff);
      writeBufferTo(timestampsFile);
      if (times > 1) {
        writeZero(times - 1);
      }
      previousElapsedMillis = elapsedMillis;

      // Write to time shifts file.
      long expectedTimeMillis = previousCurrentTimeMillis + elapsedMillisDiff;
      long timeMillisDiff = currentTimeMillis - expectedTimeMillis;
      if (Math.abs(timeMillisDiff) > 1000) {
        LOGGER.log(Level.FINE, "Time shift: " + timeMillisDiff);
        writeVarint(entry);
        writeVarint(currentTimeMillis);
        writeBufferTo(timeShiftsFile);
        previousCurrentTimeMillis = currentTimeMillis;
      } else {
        previousCurrentTimeMillis = expectedTimeMillis;
      }

      entry += times;
    }

    /**
     * Write n bytes of 0.
     */
    private void writeZero(int n) throws IOException {
      Arrays.fill(buffer, (byte) 0);
      while (n > 0) {
        bufferOffset = Math.min(n, buffer.length);
        n -= bufferOffset;
        writeBufferTo(timestampsFile);
      }
    }

    /**
     * Write a value to the {@link #buffer} as a Base 128 Varint. See:
     * https://developers.google.com/protocol-buffers/docs/encoding#varints
     * 
     * @param value
     * @throws IOException
     */
    private void writeVarint(long value) throws IOException {
      while (true) {
        if ((value & ~0x7FL) == 0) {
          buffer[bufferOffset] = (byte) value;
          bufferOffset++;
          return;
        }
        buffer[bufferOffset] = (byte) (((int) value & 0x7F) | 0x80);
        bufferOffset++;
        value >>>= 7;
      }
    }

    /**
     * Write the contents of {@link #buffer} to the given file.
     * 
     * @param file
     * @throws IOException
     */
    private void writeBufferTo(File file) throws IOException {
      FileOutputStream outputStream = outputStreams.get(file);
      if (outputStream == null) {
        Files.createParentDirs(file);
        outputStream = new FileOutputStream(file);
        outputStreams.put(file, outputStream);
      }
      outputStream.write(buffer, 0, bufferOffset);
      outputStream.flush();
      bufferOffset = 0;
    }

    /**
     * Close this writer.
     */
    public void close() {
      for (FileOutputStream outputStream : outputStreams.values()) {
        Closeables.closeQuietly(outputStream);
      }
    }
  }

  /**
   * Reader for time-stamps.
   */
  public static class Reader implements Serializable {

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
     * Transient: after serializing, the {@link #timeShiftsFile} will be
     * re-read.
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
    public Reader(Run<?, ?> build) {
      this.timestampsFile = timestampsFile(build);
      this.timeShiftsFile = timeShiftsFile(build);
      this.millisSinceEpoch = build.getTimeInMillis();
    }

    /**
     * Skip ahead to the time-stamp for the given console log file pointer.
     * 
     * @param consoleFilePointerToFind
     *          the console log file pointer
     * @param build
     *          the build for the console log
     * @return the time-stamp found at that location
     * @throws IOException
     */
    public Timestamp find(long consoleFilePointerToFind, Run<?, ?> build)
        throws IOException {
      BufferedInputStream logInputStream = null;
      try {
        Timestamp found = null;
        boolean previousNewLine = true;
        byte[] buffer = new byte[1024];
        long bytesReadTotal = 0;
        logInputStream = new BufferedInputStream(build.getLogInputStream());
        while (true) {
          int bytesRead = logInputStream.read(buffer, 0, buffer.length);
          if (bytesRead == -1) {
            return null;
          }
          for (int i = 0; i < bytesRead; i++) {
            boolean newLine = buffer[i] == 0x0A;
            if (previousNewLine && !newLine) {
              found = next();
            } else {
              found = null;
            }
            previousNewLine = newLine;
            bytesReadTotal++;
            if (bytesReadTotal > consoleFilePointerToFind) {
              return found;
            }
          }
        }
      } finally {
        Closeables.closeQuietly(logInputStream);
      }
    }

    /**
     * Read the next time-stamp.
     * 
     * @return the next time-stamp
     * @throws IOException
     */
    public Timestamp next() throws IOException {
      if (!timestampsFile.isFile() || filePointer >= timestampsFile.length()) {
        return null;
      }
      final RandomAccessFile raf = new RandomAccessFile(timestampsFile, "r");
      ByteReader byteReader = new ByteReader() {
        public byte readByte() throws IOException {
          return raf.readByte();
        }
      };
      try {
        raf.seek(filePointer);

        long elapsedMillisDiff = readVarint(byteReader);
        elapsedMillis += elapsedMillisDiff;

        timeShifts = readTimeShifts();
        if (timeShifts.containsKey(Long.valueOf(entry))) {
          millisSinceEpoch = timeShifts.get(Long.valueOf(entry)).longValue();
        } else {
          millisSinceEpoch += elapsedMillisDiff;
        }

        filePointer = raf.getFilePointer();
        entry++;
        return new Timestamp(elapsedMillis, millisSinceEpoch);
      } finally {
        closeQuietly(raf);
      }
    }

    private Map<Long, Long> readTimeShifts() throws IOException {
      if (!timeShiftsFile.isFile()) {
        return Collections.emptyMap();
      }
      if (timeShiftsFile.length() == timeShiftsFileLength) {
        return timeShifts;
      }
      timeShiftsFileLength = timeShiftsFile.length();
      final BufferedInputStream inputStream = new BufferedInputStream(
          new FileInputStream(timeShiftsFile));
      final MutableLong bytesRead = new MutableLong();
      ByteReader byteReader = new ByteReader() {
        public byte readByte() throws IOException {
          int b = inputStream.read();
          if (b == -1) {
            throw new EOFException();
          }
          bytesRead.increment();
          return (byte) b;
        }
      };
      Map<Long, Long> timeShifts = new HashMap<Long, Long>();
      try {
        while (bytesRead.longValue() < timeShiftsFileLength) {
          long entry = readVarint(byteReader);
          long shift = readVarint(byteReader);
          timeShifts.put(Long.valueOf(entry), Long.valueOf(shift));
        }
      } finally {
        Closeables.closeQuietly(inputStream);
      }
      return timeShifts;
    }

    /**
     * Read a value as a Base 128 Varint. See:
     * https://developers.google.com/protocol-buffers/docs/encoding#varints
     * 
     * @param byteReader
     * @throws IOException
     */
    private long readVarint(ByteReader byteReader) throws IOException {
      int shift = 0;
      long result = 0;
      while (shift < 64) {
        final byte b = byteReader.readByte();
        result |= (long) (b & 0x7F) << shift;
        if ((b & 0x80) == 0) {
          return result;
        }
        shift += 7;
      }
      throw new IOException("Malformed varint");
    }

    /**
     * Unconditionally close a {@link RandomAccessFile}.
     * <p>
     * Equivalent to {@link RandomAccessFile#close()}, except any exceptions
     * will be ignored. This is typically used in finally blocks.
     * 
     * @param input
     *          the file to close, may be null or already closed
     */
    private static void closeQuietly(RandomAccessFile raf) {
      try {
        if (raf != null) {
          raf.close();
        }
      } catch (IOException ioe) {
        // ignore
      }
    }
  }

  private static interface ByteReader {
    byte readByte() throws IOException;
  }

  private TimestampsIO() {
  }
}
