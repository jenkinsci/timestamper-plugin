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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Objects;

import javax.annotation.CheckForNull;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.google.common.base.Optional;
import com.google.common.io.CountingInputStream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.console.ConsoleNote;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.TimestampNote;

/**
 * Reader for the build log file which skips over the console notes.
 * 
 * @author Steven G. Brown
 */
public class LogFileReader implements Closeable {

  /**
   * A line read from the log file of a build.
   */
  public static class Line {

    private final String line;

    private final Run<?, ?> build;

    private Line(String line, Run<?, ?> build) {
      this.line = checkNotNull(line);
      this.build = checkNotNull(build);
    }

    /**
     * Get the text from this line, without the console notes.
     * 
     * @return the text
     */
    public String getText() {
      return ConsoleNote.removeNotes(line);
    }

    /**
     * Read the time-stamp from this line, if it has one.
     * 
     * @return the time-stamp
     */
    public Optional<Timestamp> readTimestamp() {
      byte[] bytes = line.getBytes(build.getCharset());
      int length = bytes.length;

      int index = 0;
      while (true) {
        index = ConsoleNote.findPreamble(bytes, index, length - index);
        if (index == -1) {
          return Optional.absent();
        }
        CountingInputStream inputStream = new CountingInputStream(
            new ByteArrayInputStream(bytes, index, length - index));

        try {
          ConsoleNote<?> consoleNote = ConsoleNote.readFrom(new DataInputStream(inputStream));
          if (consoleNote instanceof TimestampNote) {
            TimestampNote timestampNote = (TimestampNote) consoleNote;
            Timestamp timestamp = timestampNote.getTimestamp(build);
            return Optional.of(timestamp);
          }
        } catch (IOException e) {
          // Error reading console note, e.g. end of stream. Ignore.
        } catch (ClassNotFoundException e) {
          // Unknown console note. Ignore.
        }

        // Advance at least one character to avoid an infinite loop.
        index += Math.max(inputStream.getCount(), 1);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return Objects.hash(line, build);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Line) {
        Line other = (Line) obj;
        return line.equals(other.line) && build.equals(other.build);
      }
      return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return new ToStringBuilder(this).append("line", line).append("build", build).toString();
    }
  }

  private final Run<?, ?> build;

  @CheckForNull
  private BufferedReader reader;

  /**
   * Create a log file reader for the given build.
   * 
   * @param build
   */
  public LogFileReader(Run<?, ?> build) {
    this.build = checkNotNull(build);
  }

  /**
   * Read the next line from the log file.
   * 
   * @return the next line, or {@link Optional#absent()} if there are no more to
   *         read
   * @throws IOException
   */
  public Optional<Line> nextLine() throws IOException {
    if (!build.getLogFile().exists()) {
      return Optional.absent();
    }
    if (reader == null) {
      reader = new BufferedReader(build.getLogReader());
    }
    String line = reader.readLine();
    if (line == null) {
      return Optional.absent();
    }
    return Optional.of(new Line(line, build));
  }

  /**
   * Get the number of lines that can be read from the log file.
   * 
   * @return the line count
   * @throws IOException
   */
  @SuppressFBWarnings("RV_DONT_JUST_NULL_CHECK_READLINE")
  public int lineCount() throws IOException {
    if (!build.getLogFile().exists()) {
      return 0;
    }
    int lineCount = 0;
    try (BufferedReader reader = new BufferedReader(build.getLogReader())) {
      while (reader.readLine() != null) {
        lineCount++;
      }
    }
    return lineCount;
  }

  /**
   * Close this reader.
   */
  @Override
  public void close() {
    IOUtils.closeQuietly(reader);
  }
}
