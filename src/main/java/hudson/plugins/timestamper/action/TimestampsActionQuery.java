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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.format.ElapsedTimestampFormat;
import hudson.plugins.timestamper.format.SystemTimestampFormat;

/**
 * Query for retrieving a page of time-stamps from
 * {@link TimestampsActionOutput}.
 * 
 * @author Steven G. Brown
 */
public final class TimestampsActionQuery {

  /**
   * Create a new {@link TimestampsActionQuery}.
   * 
   * @param query
   *          the query string
   * @return a new query
   */
  public static TimestampsActionQuery create(String query) {
    int startLine = 0;
    Optional<Integer> endLine = Optional.absent();
    List<Function<Timestamp, String>> timestampFormats = new ArrayList<Function<Timestamp, String>>();
    boolean appendLogLine = false;
    boolean currentTime = false;

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
        timestampFormats.add(new SystemTimestampFormat(parameter.value, timeZoneId, locale));
      } else if (parameter.name.equalsIgnoreCase("elapsed")) {
        timestampFormats.add(new ElapsedTimestampFormat(parameter.value));
      } else if (parameter.name.equalsIgnoreCase("precision")) {
        int precision = readPrecision(parameter.value);
        timestampFormats.add(new PrecisionTimestampFormat(precision));
      } else if (parameter.name.equalsIgnoreCase("appendLog")) {
        appendLogLine = (parameter.value.isEmpty() || Boolean.parseBoolean(parameter.value));
      } else if (parameter.name.equalsIgnoreCase("currentTime")) {
        currentTime = (parameter.value.isEmpty() || Boolean.parseBoolean(parameter.value));
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

    return new TimestampsActionQuery(startLine, endLine, timestampFormats, appendLogLine,
        currentTime);
  }

  private static List<QueryParameter> readQueryString(String query) {
    ImmutableList.Builder<QueryParameter> parameters = new ImmutableList.Builder<QueryParameter>();
    if (query != null) {
      String[] pairs = query.split("&");
      for (String pair : pairs) {
        String[] nameAndValue = pair.split("=", 2);
        String name = urlDecode(nameAndValue[0]);
        String value = (nameAndValue.length == 1 ? "" : urlDecode(nameAndValue[1]));
        parameters.add(new QueryParameter(name, value));
      }
    }
    return parameters.build();
  }

  private static String urlDecode(String string) {
    try {
      return URLDecoder.decode(string, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private static int readPrecision(String precision) {
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
      throw new IllegalArgumentException("Expected non-negative precision, but was: " + precision);
    }
    return intPrecision;
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

  final int startLine;

  final Optional<Integer> endLine;

  final List<Function<Timestamp, String>> timestampFormats;

  final boolean appendLogLine;

  final boolean currentTime;

  TimestampsActionQuery(int startLine, Optional<Integer> endLine,
      List<? extends Function<Timestamp, String>> timestampFormats, boolean appendLogLine,
      boolean currentTime) {
    this.startLine = startLine;
    this.endLine = checkNotNull(endLine);
    this.timestampFormats = ImmutableList.copyOf(timestampFormats);
    this.appendLogLine = appendLogLine;
    this.currentTime = currentTime;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(startLine, endLine, timestampFormats, appendLogLine, currentTime);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof TimestampsActionQuery) {
      TimestampsActionQuery other = (TimestampsActionQuery) obj;
      return startLine == other.startLine && endLine.equals(other.endLine)
          && timestampFormats.equals(other.timestampFormats) && appendLogLine == other.appendLogLine
          && currentTime == other.currentTime;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return new ToStringBuilder(this).append("startLine", startLine).append("endLine", endLine)
        .append("timestampFormats", timestampFormats).append("appendLogLine", appendLogLine)
        .append("currentTime", currentTime).toString();
  }
}
