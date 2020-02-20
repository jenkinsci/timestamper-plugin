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
package hudson.plugins.timestamper.annotator;

import com.google.common.io.ByteStreams;
import hudson.model.Run;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Parser that is able to find a position in the console log file of a build.
 *
 * @author Steven G. Brown
 */
@Immutable
class ConsoleLogParser implements Serializable {

  private static final long serialVersionUID = 1L;

  private final long pos;

  /**
   * Create a new {@link ConsoleLogParser}.
   *
   * @param pos the position to find in the console log file. A non-negative position is from the
   *     start of the file, and a negative position is back from the end of the file.
   */
  ConsoleLogParser(long pos) {
    this.pos = pos;
  }

  /**
   * Skip to a position in the console log file.
   *
   * @param build the build to inspect
   * @return the result
   */
  public ConsoleLogParser.Result seek(Run<?, ?> build) throws IOException {
    long logLength = build.getLogText().length();
    if (pos == 0 || logLength + pos <= 0) {
      ConsoleLogParser.Result result = new ConsoleLogParser.Result();
      result.atNewLine = true;
      return result;
    }

    try (InputStream inputStream = new BufferedInputStream(build.getLogInputStream())) {
      if (build.isBuilding() || pos > 0) {
        long posFromStart = pos;
        if (pos < 0) {
          posFromStart = logLength + pos;
        }
        return parseFromStart(inputStream, posFromStart);
      } else {
        ByteStreams.skipFully(inputStream, logLength + pos - 1);
        return parseFromFinish(new BoundedInputStream(inputStream, -pos));
      }
    }
  }

  private ConsoleLogParser.Result parseFromStart(InputStream inputStream, long posFromStart)
      throws IOException {
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();

    for (long i = 0; i < posFromStart; i++) {
      int value = inputStream.read();
      if (value == -1) {
        result.endOfFile = true;
        break;
      }
      result.atNewLine = isNewLine(value);
      if (result.atNewLine) {
        result.lineNumber++;
      }
    }

    return result;
  }

  private ConsoleLogParser.Result parseFromFinish(InputStream inputStream) throws IOException {
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = -1;

    int value = inputStream.read();
    result.atNewLine = isNewLine(value);

    while ((value = inputStream.read()) != -1) {
      if (isNewLine(value)) {
        result.lineNumber--;
      }
    }

    return result;
  }

  private boolean isNewLine(int character) {
    return character == 0x0A;
  }

  static final class Result {

    /** The current line number, starting at line zero. */
    int lineNumber;

    /** Whether the last-read character was a new line. */
    boolean atNewLine;

    /** Whether the position is past the end of the file. */
    boolean endOfFile;

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      return Objects.hash(lineNumber, atNewLine, endOfFile);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof ConsoleLogParser.Result) {
        ConsoleLogParser.Result other = (ConsoleLogParser.Result) obj;
        return lineNumber == other.lineNumber
            && atNewLine == other.atNewLine
            && endOfFile == other.endOfFile;
      }
      return false;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return new ToStringBuilder(this)
          .append("lineNumber", lineNumber)
          .append("atNewLine", atNewLine)
          .append("endOfFile", endOfFile)
          .toString();
    }
  }
}
