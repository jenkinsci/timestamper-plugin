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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.io.Closeables;
import com.google.common.io.Files;

/**
 * Write the time-stamps for a build to disk.
 * 
 * @author Steven G. Brown
 */
public class TimestampsWriter {

  private static final Logger LOGGER = Logger.getLogger(TimestampsWriter.class
      .getName());

  private static final int BUFFER_SIZE = 1024;

  static File timestamperDir(Run<?, ?> build) {
    return new File(build.getRootDir(), "timestamper");
  }

  static File timestampsFile(File timestamperDir) {
    return new File(timestamperDir, "timestamps");
  }

  static File timeShiftsFile(File timestamperDir) {
    return new File(timestamperDir, "timeshifts");
  }

  private final FileOutputStream timestampsOutput;

  private final File timeShiftsFile;

  private FileOutputStream timeShiftsOutput;

  private long entry;

  /**
   * Buffer that is used to store Varints prior to writing to a file.
   */
  private final byte[] buffer = new byte[BUFFER_SIZE];

  private long startNanos;

  private long previousElapsedMillis;

  private long previousCurrentTimeMillis;

  /**
   * Create a time-stamps writer for the given build.
   * 
   * @param build
   * @throws IOException
   */
  public TimestampsWriter(Run<?, ?> build) throws IOException {
    File timestamperDir = timestamperDir(build);
    File timestampsFile = timestampsFile(timestamperDir);
    Files.createParentDirs(timestampsFile);
    boolean fileCreated = timestampsFile.createNewFile();
    if (!fileCreated) {
      throw new IOException("File already exists: " + timestampsFile);
    }
    this.timestampsOutput = new FileOutputStream(timestampsFile);

    this.timeShiftsFile = timeShiftsFile(timestamperDir);
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
    writeVarintsTo(timestampsOutput, elapsedMillisDiff);
    if (times > 1) {
      writeZerosTo(timestampsOutput, times - 1);
    }
    previousElapsedMillis = elapsedMillis;

    // Write to time shifts file.
    long expectedTimeMillis = previousCurrentTimeMillis + elapsedMillisDiff;
    long timeMillisDiff = currentTimeMillis - expectedTimeMillis;
    if (Math.abs(timeMillisDiff) > 1000) {
      LOGGER.log(Level.FINE, "Time shift: " + timeMillisDiff);
      if (timeShiftsOutput == null) {
        timeShiftsOutput = new FileOutputStream(timeShiftsFile);
      }
      writeVarintsTo(timeShiftsOutput, entry, currentTimeMillis);
      previousCurrentTimeMillis = currentTimeMillis;
    } else {
      previousCurrentTimeMillis = expectedTimeMillis;
    }

    entry += times;
  }

  /**
   * Write each value to the given output stream as a Base 128 Varint.
   * 
   * @param outputStream
   * @param values
   * @throws IOException
   */
  private void writeVarintsTo(FileOutputStream outputStream, long... values)
      throws IOException {
    int offset = 0;
    for (long value : values) {
      offset = Varint.write(value, buffer, offset);
    }
    outputStream.write(buffer, 0, offset);
    outputStream.flush();
  }

  /**
   * Write n bytes of 0 to the given output stream.
   * 
   * @param outputStream
   * @param n
   */
  private void writeZerosTo(FileOutputStream outputStream, int n)
      throws IOException {
    Arrays.fill(buffer, (byte) 0);
    while (n > 0) {
      int bytesToWrite = Math.min(n, buffer.length);
      n -= bytesToWrite;
      outputStream.write(buffer, 0, bytesToWrite);
      outputStream.flush();
    }
  }

  /**
   * Close this writer.
   */
  public void close() {
    Closeables.closeQuietly(timestampsOutput);
    Closeables.closeQuietly(timeShiftsOutput);
  }
}