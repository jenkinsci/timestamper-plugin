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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import hudson.model.Run;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Optional;
import javax.annotation.CheckForNull;

/**
 * Write the time-stamps for a build to disk.
 *
 * @author Steven G. Brown
 */
public class TimestampsWriter implements Closeable {

  private static final int BUFFER_SIZE = 1024;

  private final File timestampsFile;

  private final Optional<MessageDigest> timestampsDigest;

  @CheckForNull private OutputStream timestampsOutput;

  /** Buffer that is used to store Varints prior to writing to a file. */
  private final byte[] buffer = new byte[BUFFER_SIZE];

  private long previousCurrentTimeMillis;

  /**
   * Create a time-stamps writer for the given build.
   *
   * @param build
   * @throws IOException
   */
  public TimestampsWriter(Run<?, ?> build) throws IOException {
    this(build, Optional.empty());
  }

  /**
   * Create a time-stamps writer for the given build.
   *
   * @param build
   * @param digest (optional)
   * @throws IOException
   */
  public TimestampsWriter(Run<?, ?> build, Optional<MessageDigest> digest) throws IOException {
    this(TimestamperPaths.timestampsFile(build), build.getStartTimeInMillis(), digest);
  }

  public TimestampsWriter(File timestampsFile, long buildStartTime, Optional<MessageDigest> digest)
      throws IOException {
    this.timestampsFile = timestampsFile;
    this.previousCurrentTimeMillis = buildStartTime;
    this.timestampsDigest = checkNotNull(digest);

    Files.createParentDirs(timestampsFile);
    boolean fileCreated = timestampsFile.createNewFile();
    if (!fileCreated) {
      throw new IOException("File already exists: " + timestampsFile);
    }
  }

  /**
   * Write a time-stamp for a line of the console log.
   *
   * @param currentTimeMillis {@link System#currentTimeMillis()}
   * @param times the number of times to write the time-stamp
   * @throws IOException
   */
  public void write(long currentTimeMillis, int times) throws IOException {
    if (times < 1) {
      return;
    }
    long elapsedMillis = currentTimeMillis - previousCurrentTimeMillis;
    previousCurrentTimeMillis = currentTimeMillis;

    // Write to the time-stamps file.
    if (timestampsOutput == null) {
      timestampsOutput = openTimestampsStream();
    }
    writeVarintsTo(timestampsOutput, elapsedMillis);
    if (times > 1) {
      writeZerosTo(timestampsOutput, times - 1);
    }
  }

  /**
   * Open an output stream for writing to the time-stamps file.
   *
   * @return the output stream
   * @throws FileNotFoundException
   */
  private OutputStream openTimestampsStream() throws FileNotFoundException {
    OutputStream outputStream = new FileOutputStream(timestampsFile);
    if (timestampsDigest.isPresent()) {
      outputStream = new DigestOutputStream(outputStream, timestampsDigest.get());
    }
    return outputStream;
  }

  /**
   * Write each value to the given output stream as a Base 128 Varint.
   *
   * @param outputStream
   * @param values
   * @throws IOException
   */
  private void writeVarintsTo(OutputStream outputStream, long... values) throws IOException {
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
  private void writeZerosTo(OutputStream outputStream, int n) throws IOException {
    Arrays.fill(buffer, (byte) 0);
    while (n > 0) {
      int bytesToWrite = Math.min(n, buffer.length);
      n -= bytesToWrite;
      outputStream.write(buffer, 0, bytesToWrite);
      outputStream.flush();
    }
  }

  /**
   * Write a time-stamps digest file for the build.
   *
   * @throws IOException
   */
  public void writeDigest() throws IOException {
    if (timestampsDigest.isPresent()) {
      writeDigest(timestampsDigest.get());
    }
  }

  private void writeDigest(MessageDigest timestampsDigest) throws IOException {
    StringBuilder hash = new StringBuilder();
    for (byte b : timestampsDigest.digest()) {
      hash.append(String.format("%02x", b));
    }
    hash.append("\n");
    File digestFile =
        new File(
            timestampsFile.getParent(),
            timestampsFile.getName() + "." + timestampsDigest.getAlgorithm());
    Files.write(hash.toString(), digestFile, Charsets.US_ASCII);
  }

  /** {@inheritDoc} */
  @Override
  public void close() throws IOException {
    if (timestampsOutput != null) {
      timestampsOutput.close();
    }
  }
}
