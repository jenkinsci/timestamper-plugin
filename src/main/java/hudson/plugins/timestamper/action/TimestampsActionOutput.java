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
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.format.ElapsedTimestampFormat;
import hudson.plugins.timestamper.format.SystemTimestampFormat;
import hudson.plugins.timestamper.io.LogFileReader;
import hudson.plugins.timestamper.io.TimestampsReader;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.time.DurationFormatUtils;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

/**
 * Generate a page of time-stamps on behalf of {@link TimestampsAction}.
 * <p>
 * Each line contains time-stamps for the equivalent line in the console log,
 * and optionally includes the console log text.
 * <p>
 * By default, the elapsed time will be displayed in seconds, with three places
 * after the decimal point. The output can be configured by providing the
 * following query parameters. The output can include multiple time-stamp
 * formats by providing multiple parameters.
 * <ul>
 * <li>"precision": Display the elapsed time in seconds, with a certain number
 * of places after the decimal point. Accepts a number of decimal places or
 * values such as "seconds" and "milliseconds".</li>
 * <li>"time": Display the system clock time. Accepts the
 * {@link SimpleDateFormat} format.</li>
 * <li>"timeZone": Time zone used when displaying the system clock time. Accepts
 * the {@link TimeZone} ID format.</li>
 * <li>"elapsed": Display the elapsed time since the start of the build. Accepts
 * the {@link DurationFormatUtils} format.</li>
 * <li>"appendLog": Display the console log line after the time-stamp.</li>
 * <li>"startLine": Display the time-stamps starting from a certain line.
 * Accepts a positive integer to start at that line, or a negative integer to
 * start that many lines back from the end.</li>
 * <li>"endLine": Display the time-stamps ending at a certain line. Accepts a
 * positive integer to finish at that line, or a negative integer to finish that
 * many lines back from the end.</li>
 * <li>"locale": Select the locale to use when displaying the system clock time.
 * </li>
 * </ul>
 * 
 * @author Steven G. Brown
 */
public class TimestampsActionOutput {

  private int startLine;

  private Optional<Integer> endLine;

  private final List<Function<Timestamp, String>> timestampFormats = new ArrayList<Function<Timestamp, String>>();

  private boolean appendLogLine;

  private int linesRead;

  @CheckForNull
  private Integer cachedLineCount;

  public TimestampsActionOutput() {
    setQuery(null);
  }

  /**
   * Set the query string.
   * 
   * @param query
   *          the query string
   */
  public void setQuery(String query) {
    startLine = 0;
    endLine = Optional.absent();
    timestampFormats.clear();
    appendLogLine = false;

    List<QueryParameter> queryParameters = readQueryString(query);

    Optional<String> timeZoneId = Optional.absent();
    Locale locale = Locale.getDefault();
    for (QueryParameter parameter : queryParameters) {
      if (parameter.name.equalsIgnoreCase("timeZone")) {
        // '+' was replaced with ' ' by URL decoding, so put it back.
        timeZoneId = Optional.of(parameter.value.replace("GMT ", "GMT+"));
      } else if (parameter.name.equalsIgnoreCase("locale")) {
        locale = LocaleUtils.toLocale(parameter.value);
      }
    }

    for (QueryParameter parameter : queryParameters) {
      if (parameter.name.equalsIgnoreCase("time")) {
        timestampFormats.add(new SystemTimestampFormat(parameter.value,
            timeZoneId, locale));
      } else if (parameter.name.equalsIgnoreCase("elapsed")) {
        timestampFormats.add(new ElapsedTimestampFormat(parameter.value));
      } else if (parameter.name.equalsIgnoreCase("precision")) {
        int precision = readPrecision(parameter.value);
        timestampFormats.add(new PrecisionTimestampFormat(precision));
      } else if (parameter.name.equalsIgnoreCase("appendLog")) {
        appendLogLine = (parameter.value.isEmpty() || Boolean
            .parseBoolean(parameter.value));
      } else if (parameter.name.equalsIgnoreCase("startLine")) {
        startLine = Integer.parseInt(parameter.value);
      } else if (parameter.name.equalsIgnoreCase("endLine")) {
        endLine = Optional.of(Integer.valueOf(parameter.value));
      }
    }

    if (timestampFormats.isEmpty()) {
      // Default
      timestampFormats.add(new PrecisionTimestampFormat(3));
    }
  }

  private List<QueryParameter> readQueryString(String query) {
    ImmutableList.Builder<QueryParameter> parameters = new ImmutableList.Builder<QueryParameter>();
    if (query != null) {
      String[] pairs = query.split("&");
      for (String pair : pairs) {
        String[] nameAndValue = pair.split("=", 2);
        String name = urlDecode(nameAndValue[0]);
        String value = (nameAndValue.length == 1 ? ""
            : urlDecode(nameAndValue[1]));
        parameters.add(new QueryParameter(name, value));
      }
    }
    return parameters.build();
  }

  private String urlDecode(String string) {
    try {
      return URLDecoder.decode(string, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private int readPrecision(String precision) {
    if ("seconds".equalsIgnoreCase(precision)) {
      return 0;
    }
    if ("milliseconds".equalsIgnoreCase(precision)) {
      return 3;
    }
    if ("microseconds".equalsIgnoreCase(precision)) {
      return 6;
    }
    if ("nanoseconds".equalsIgnoreCase(precision)) {
      return 9;
    }
    int intPrecision = Integer.parseInt(precision);
    if (intPrecision < 0) {
      throw new IllegalArgumentException(
          "Expected non-negative precision, but was: " + precision);
    }
    return intPrecision;
  }

  /**
   * Generate the next line in the page of time-stamps.
   * 
   * @param timestampsReader
   * @param logFileReader
   * @return the next line
   * @throws IOException
   */
  public Optional<String> nextLine(TimestampsReader timestampsReader,
      LogFileReader logFileReader) throws IOException {

    linesRead += readToStartLine(timestampsReader, logFileReader);
    resolveEndLine(logFileReader);
    if (endLine.isPresent() && linesRead >= endLine.get()) {
      return Optional.absent();
    }

    List<String> parts = new ArrayList<String>();

    Optional<Timestamp> timestamp = timestampsReader.read();
    linesRead++;
    if (timestamp.isPresent()) {
      for (Function<Timestamp, String> format : timestampFormats) {
        parts.add(format.apply(timestamp.get()));
      }
    }

    if (appendLogLine) {
      Optional<String> logFileLine = logFileReader.nextLine();
      if (logFileLine.isPresent()) {
        parts.add(logFileLine.get());
      }
    }

    if (parts.isEmpty()) {
      return Optional.absent();
    }
    return Optional.of(Joiner.on(' ').join(parts));
  }

  private int readToStartLine(TimestampsReader timestampsReader,
      LogFileReader logFileReader) throws IOException {
    int linesToSkip = Math.max(startLine - 1, 0);
    if (startLine < 0) {
      int lineCount = getLineCount(logFileReader);
      linesToSkip = lineCount + startLine;
    }
    startLine = 0;

    for (int line = 0; line < linesToSkip; line++) {
      timestampsReader.read();
      logFileReader.nextLine();
    }
    return linesToSkip;
  }

  private void resolveEndLine(LogFileReader logFileReader) throws IOException {
    if (endLine.isPresent() && endLine.get() < 0) {
      int lineCount = getLineCount(logFileReader);
      endLine = Optional.of(lineCount + endLine.get() + 1);
    }
  }

  private int getLineCount(LogFileReader logFileReader) throws IOException {
    if (cachedLineCount == null) {
      cachedLineCount = logFileReader.lineCount();
    }
    return cachedLineCount;
  }

  private static class QueryParameter {

    final String name;

    final String value;

    QueryParameter(String name, String value) {
      this.name = checkNotNull(name);
      this.value = checkNotNull(value);
    }

    @Override
    public String toString() {
      return name + "=" + value;
    }
  }

  private static class PrecisionTimestampFormat implements
      Function<Timestamp, String> {

    private final int precision;

    PrecisionTimestampFormat(int precision) {
      this.precision = precision;
    }

    @Override
    public String apply(@Nonnull Timestamp timestamp) {
      long seconds = timestamp.elapsedMillis / 1000;
      if (precision == 0) {
        return String.valueOf(seconds);
      }
      long millis = timestamp.elapsedMillis % 1000;
      String fractional = String.format("%03d", millis);
      if (precision <= 3) {
        fractional = fractional.substring(0, precision);
      } else {
        fractional += Strings.repeat("0", precision - 3);
      }
      return String.valueOf(seconds) + "." + fractional;
    }
  }
}
