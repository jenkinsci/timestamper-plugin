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

import java.io.BufferedReader;
import java.io.IOException;

import com.google.common.base.Optional;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Reader for the build log file which skips over the console notes.
 * 
 * @author Steven G. Brown
 */
public class LogFileReader {

  private final Run<?, ?> build;

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
  public Optional<String> nextLine() throws IOException {
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
    return Optional.of(ConsoleNote.removeNotes(line));
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
    BufferedReader reader = new BufferedReader(build.getLogReader());
    try {
      while (reader.readLine() != null) {
        lineCount++;
      }
    } finally {
      Closeables.closeQuietly(reader);
    }
    return lineCount;
  }

  /**
   * Close this reader.
   */
  public void close() {
    Closeables.closeQuietly(reader);
  }
}
