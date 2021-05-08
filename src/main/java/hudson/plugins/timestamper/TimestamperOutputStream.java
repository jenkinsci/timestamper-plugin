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
package hudson.plugins.timestamper;

import hudson.plugins.timestamper.io.TimestampsWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Output stream that records time-stamps into a separate file while inspecting the delegate output
 * stream.
 *
 * @author Steven G. Brown
 */
final class TimestamperOutputStream extends OutputStream {

  private static final Logger LOGGER = Logger.getLogger(TimestamperOutputStream.class.getName());

  /** The delegate output stream. */
  private final OutputStream delegate;

  /** Writer for the time-stamps. */
  private final TimestampsWriter timestampsWriter;

  /** Byte array that is re-used each time the {@link #write(int)} method is called. */
  private final byte[] oneElementByteArray = new byte[1];

  /** The last processed character, or {@code -1} for the start of the stream. */
  private int previousCharacter = -1;

  /** Set to {@code true} when an error occurs while writing the time-stamps. */
  private boolean writeError;

  /**
   * Create a new {@link TimestamperOutputStream}.
   *
   * @param delegate the delegate output stream
   * @param timestampsWriter will be used by this output stream to write the time-stamps and closed
   *     when the {@link #close()} method is called
   */
  TimestamperOutputStream(OutputStream delegate, TimestampsWriter timestampsWriter) {
    this.delegate = Objects.requireNonNull(delegate);
    this.timestampsWriter = Objects.requireNonNull(timestampsWriter);
  }

  /** {@inheritDoc} */
  @Override
  public void write(int b) throws IOException {
    oneElementByteArray[0] = (byte) b;
    writeTimestamps(oneElementByteArray, 0, 1);
    delegate.write(b);
  }

  /** {@inheritDoc} */
  @Override
  public void write(byte[] b) throws IOException {
    writeTimestamps(b, 0, b.length);
    delegate.write(b);
  }

  /** {@inheritDoc} */
  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    writeTimestamps(b, off, len);
    delegate.write(b, off, len);
  }

  private void writeTimestamps(byte[] b, int off, int len) {
    byte newlineCharacter = (byte) 0x0A;
    int lineStartCount = 0;
    for (int i = off; i < off + len; i++) {
      if (previousCharacter == -1 || previousCharacter == newlineCharacter) {
        lineStartCount++;
      }
      previousCharacter = b[i];
    }

    if (lineStartCount > 0 && !writeError) {
      long currentTimeMillis = System.currentTimeMillis();
      try {
        timestampsWriter.write(currentTimeMillis, lineStartCount);
      } catch (IOException ex) {
        writeError = true;
        LOGGER.log(Level.WARNING, "Error writing timestamps", ex);
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void flush() throws IOException {
    delegate.flush();
  }

  /** {@inheritDoc} */
  @Override
  public void close() throws IOException {
    try {
      timestampsWriter.close();

      if (!writeError) {
        timestampsWriter.writeDigest();
      }
    } catch (IOException ex) {
      LOGGER.log(Level.WARNING, ex.getMessage(), ex);
    }

    delegate.close();
  }
}
