/*
 * The MIT License
 * 
 * Copyright (c) 2016 Steven G. Brown
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
import hudson.console.ConsoleNote;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.TimestampNote;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import javax.annotation.CheckForNull;

import com.google.common.base.Optional;

/**
 * Read the time-stamp console notes for a build from the log file.
 * 
 * @author Steven G. Brown
 */
public class TimestampNotesReader implements TimestampsReader {

  private final Run<?, ?> build;

  @CheckForNull
  private DataInputStream dataInputStream;

  /**
   * Create a time-stamp notes reader for the given build.
   * 
   * @param build
   */
  public TimestampNotesReader(Run<?, ?> build) {
    this.build = checkNotNull(build);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<Timestamp> read() throws IOException {
    if (dataInputStream == null) {
      dataInputStream = new DataInputStream(new BufferedInputStream(
          build.getLogInputStream()));
    }
    while (true) {
      dataInputStream.mark(1);
      int currentByte = dataInputStream.read();
      if (currentByte == -1) {
        return Optional.absent();
      }
      if (currentByte == ConsoleNote.PREAMBLE[0]) {
        dataInputStream.reset();
        ConsoleNote<?> consoleNote;
        try {
          consoleNote = ConsoleNote.readFrom(dataInputStream);
        } catch (ClassNotFoundException ex) {
          // Unknown console note. Ignore.
          continue;
        }
        if (consoleNote instanceof TimestampNote) {
          TimestampNote timestampNote = (TimestampNote) consoleNote;
          Timestamp timestamp = timestampNote.getTimestamp(build);
          return Optional.of(timestamp);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    Closeables.closeQuietly(dataInputStream);
  }
}
