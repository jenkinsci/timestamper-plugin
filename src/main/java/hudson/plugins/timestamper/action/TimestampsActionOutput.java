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
package hudson.plugins.timestamper.action;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.io.LogFileReader;
import hudson.plugins.timestamper.io.LogFileReader.Line;
import hudson.plugins.timestamper.io.TimestampsReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.apache.commons.lang.time.DurationFormatUtils;

/**
 * Generate a page of time-stamps on behalf of {@link TimestampsAction}.
 *
 * <p>Each line contains time-stamps for the equivalent line in the console log, and optionally
 * includes the console log text.
 *
 * <p>By default, the elapsed time will be displayed in seconds, with three places after the decimal
 * point. The output can be configured by providing the following query parameters. The output can
 * include multiple time-stamp formats by providing multiple parameters.
 *
 * <ul>
 *   <li>"precision": Display the elapsed time in seconds, with a certain number of places after the
 *       decimal point. Accepts a number of decimal places or values such as "seconds" and
 *       "milliseconds".
 *   <li>"time": Display the system clock time. Accepts the {@link SimpleDateFormat} format.
 *   <li>"timeZone": Time zone used when displaying the system clock time. Accepts the {@link
 *       TimeZone} ID format.
 *   <li>"elapsed": Display the elapsed time since the start of the build. Accepts the {@link
 *       DurationFormatUtils} format.
 *   <li>"appendLog": Display the console log line after the time-stamp.
 *   <li>"startLine": Display the time-stamps starting from a certain line. Accepts a positive
 *       integer to start at that line, or a negative integer to start that many lines back from the
 *       end.
 *   <li>"endLine": Display the time-stamps ending at a certain line. Accepts a positive integer to
 *       finish at that line, or a negative integer to finish that many lines back from the end.
 *   <li>"locale": Select the locale to use when displaying the system clock time.
 *   <li>"currentTime": Display the current time instead of reading time-stamps from the build.
 *   <li>
 * </ul>
 *
 * @author Steven G. Brown
 */
public class TimestampsActionOutput {

  /**
   * Open a reader which provides the page of time-stamps.
   *
   * @param build
   * @param query
   * @return a {@link BufferedReader}
   */
  public static BufferedReader open(Run<?, ?> build, TimestampsActionQuery query) {
    TimestampsReader timestampsReader = new TimestampsReader(build);
    LogFileReader logFileReader = new LogFileReader(build);

    long buildStartTime = build.getStartTimeInMillis();
    long millisSinceEpoch = System.currentTimeMillis();
    Timestamp currentTimestamp = new Timestamp(millisSinceEpoch - buildStartTime, millisSinceEpoch);

    return open(timestampsReader, logFileReader, query, currentTimestamp);
  }

  static BufferedReader open(
      final TimestampsReader timestampsReader,
      final LogFileReader logFileReader,
      final TimestampsActionQuery query,
      Timestamp currentTimestamp) {
    if (query.currentTime) {
      List<String> parts = new ArrayList<String>();
      for (Function<Timestamp, String> format : query.timestampFormats) {
        parts.add(format.apply(currentTimestamp));
      }
      String result = Joiner.on(' ').join(parts) + "\n";
      return new BufferedReader(new StringReader(result));
    }

    final StringBuilder buffer = new StringBuilder();

    Reader reader =
        new Reader() {
          int linesRead;
          Optional<Integer> endLine = Optional.absent();
          boolean started;

          @Override
          public int read(char[] cbuf, int off, int len) throws IOException {
            if (!started) {
              LineCountSupplier lineCount = new LineCountSupplier(logFileReader);
              linesRead = readToStartLine(query, lineCount);
              endLine = resolveEndLine(query, lineCount);
              started = true;
            }
            while (buffer.length() < len) {
              Optional<String> nextLine = readNextLine(query);
              if (!nextLine.isPresent()) {
                break;
              }
              linesRead++;
              if (endLine.isPresent() && linesRead > endLine.get()) {
                break;
              }
              buffer.append(nextLine.get());
              buffer.append("\n");
            }
            int numRead = new StringReader(buffer.toString()).read(cbuf, off, len);
            buffer.delete(0, (numRead >= 0 ? numRead : buffer.length()));
            return numRead;
          }

          private int readToStartLine(TimestampsActionQuery query, LineCountSupplier lineCount)
              throws IOException {
            int linesToSkip = Math.max(query.startLine - 1, 0);
            if (query.startLine < 0) {
              linesToSkip = lineCount.get() + query.startLine;
            }

            for (int line = 0; line < linesToSkip; line++) {
              timestampsReader.read();
              logFileReader.nextLine();
            }
            return linesToSkip;
          }

          private Optional<Integer> resolveEndLine(
              TimestampsActionQuery query, LineCountSupplier lineCount) throws IOException {
            if (query.endLine.isPresent() && query.endLine.get() < 0) {
              return Optional.of(lineCount.get() + query.endLine.get() + 1);
            }
            return query.endLine;
          }

          private Optional<String> readNextLine(TimestampsActionQuery query) throws IOException {

            Optional<Timestamp> timestamp = timestampsReader.read();
            Optional<Line> logFileLine = logFileReader.nextLine();
            if (logFileLine.isPresent() && !timestamp.isPresent()) {
              timestamp = logFileLine.get().readTimestamp();
            }

            String result = "";
            if (timestamp.isPresent()) {
              List<String> parts = new ArrayList<String>();
              for (Function<Timestamp, String> format : query.timestampFormats) {
                parts.add(format.apply(timestamp.get()));
              }
              result = Joiner.on(' ').join(parts);
            }
            if (query.appendLogLine) {
              result += "  ";
              if (logFileLine.isPresent()) {
                result += logFileLine.get().getText();
              }
            }

            if (!timestamp.isPresent() && !logFileLine.isPresent()) {
              return Optional.absent();
            }
            return Optional.of(result);
          }

          @Override
          public void close() throws IOException {
            timestampsReader.close();
            logFileReader.close();
          }
        };

    return new BufferedReader(reader);
  }

  private static class LineCountSupplier {

    private final LogFileReader logFileReader;

    private Optional<Integer> lineCount = Optional.absent();

    LineCountSupplier(LogFileReader logFileReader) {
      this.logFileReader = checkNotNull(logFileReader);
    }

    int get() throws IOException {
      if (!lineCount.isPresent()) {
        lineCount = Optional.of(logFileReader.lineCount());
      }
      return lineCount.get();
    }
  }

  private TimestampsActionOutput() {}
}
