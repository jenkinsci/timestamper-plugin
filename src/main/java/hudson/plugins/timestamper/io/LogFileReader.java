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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.console.ConsoleNote;
import hudson.model.Run;
import java.io.Closeable;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Scanner;
import javax.annotation.CheckForNull;
import org.apache.commons.io.IOUtils;

/**
 * Reader for the build log file. Lines returned by this class may or may not have timestamps
 * embedded in them, and timestamps may or may not be represented as {@link ConsoleNote}s. There may
 * or may not be other {@link ConsoleNote}s. The only thing consumers of this class can rely on is
 * that the number of lines returned can be used for line counting purposes.
 *
 * @author Steven G. Brown
 */
public class LogFileReader implements Closeable {

  private final Run<?, ?> build;

  @CheckForNull private Scanner reader;

  /**
   * Create a log file reader for the given build.
   */
  public LogFileReader(Run<?, ?> build) {
    this.build = checkNotNull(build);
  }

  /**
   * Read the next line from the log file.
   *
   * @return the next line, or {@link Optional#empty()} if there are no more to read
   */
  public Optional<String> nextLine() throws IOException {
    if (!build.getLogFile().exists()) { // TODO JENKINS-54128 rather use getLogText
      return Optional.empty();
    }
    if (reader == null) {
      reader = new Scanner(build.getLogReader());
      reader.useDelimiter("\n");
    }
    String line;
    try {
      line = reader.next();
    } catch (NoSuchElementException e) {
      return Optional.empty();
    }
    return Optional.of(line);
  }

  /**
   * Get the number of lines that can be read from the log file.
   *
   * @return the line count
   */
  @SuppressFBWarnings("RV_DONT_JUST_NULL_CHECK_READLINE")
  public int lineCount() throws IOException {
    if (!build.getLogFile().exists()) {
      return 0;
    }
    int lineCount = 0;
    try (Scanner reader = new Scanner(build.getLogReader())) {
      reader.useDelimiter("\n");
      while (reader.hasNext()) {
        reader.next();
        lineCount++;
      }
    }
    return lineCount;
  }

  /** Close this reader. */
  @Override
  public void close() {
    IOUtils.closeQuietly(reader);
  }
}
