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

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.io.LogFileReader;
import hudson.plugins.timestamper.io.TimestampsReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.stubbing.OngoingStubbing;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

/**
 * Unit test for the {@link TimestampsActionOutput} class.
 * 
 * @author Steven G. Brown
 */
@RunWith(Parameterized.class)
public class TimestampsActionOutputTest {

  /**
   * @return the test data
   */
  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        { "", DEFAULT_OUTPUT },
        { null, DEFAULT_OUTPUT },
        { "precision=0", asList("0", "0", "0", "0", "1", "10") },
        { "precision=0&precision=1",
            asList("0 0.0", "0 0.0", "0 0.0", "0 0.1", "1 1.0", "10 10.0") },
        { "precision=seconds", asList("0", "0", "0", "0", "1", "10") },
        { "precision=1", asList("0.0", "0.0", "0.0", "0.1", "1.0", "10.0") },
        { "precision=2",
            asList("0.00", "0.00", "0.01", "0.10", "1.00", "10.00") },
        { "precision=3",
            asList("0.000", "0.001", "0.010", "0.100", "1.000", "10.000") },
        { "precision=milliseconds",
            asList("0.000", "0.001", "0.010", "0.100", "1.000", "10.000") },
        {
            "precision=6",
            asList("0.000000", "0.001000", "0.010000", "0.100000", "1.000000",
                "10.000000") },
        {
            "precision=microseconds",
            asList("0.000000", "0.001000", "0.010000", "0.100000", "1.000000",
                "10.000000") },
        {
            "precision=nanoseconds",
            asList("0.000000000", "0.001000000", "0.010000000", "0.100000000",
                "1.000000000", "10.000000000") },
        {
            "time=dd:HH:mm:ss",
            asList("01:00:00:01", "01:00:01:00", "01:01:00:00", "02:00:00:00",
                "03:00:00:00", "04:00:00:00") },
        {
            "time=dd:HH:mm:ss&timeZone=GMT+10",
            asList("01:10:00:01", "01:10:01:00", "01:11:00:00", "02:10:00:00",
                "03:10:00:00", "04:10:00:00") },
        {
            "time=dd:HH:mm:ss&timeZone=GMT-10",
            asList("31:14:00:01", "31:14:01:00", "31:15:00:00", "01:14:00:00",
                "02:14:00:00", "03:14:00:00") },
        { "elapsed=s.SSS",
            asList("0.000", "0.001", "0.010", "0.100", "1.000", "10.000") },
        {
            "time=dd:HH:mm:ss&elapsed=s.SSS",
            asList("01:00:00:01 0.000", "01:00:01:00 0.001",
                "01:01:00:00 0.010", "02:00:00:00 0.100", "03:00:00:00 1.000",
                "04:00:00:00 10.000") } });
  }

  private static final List<String> DEFAULT_OUTPUT = asList("0.000", "0.001",
      "0.010", "0.100", "1.000", "10.000");

  /**
   */
  @Parameter(0)
  public String query;

  /**
   */
  @Parameter(1)
  public List<String> expectedResult;

  private TimestampsReader timestampsReader;

  private LogFileReader logFileReader;

  private TimestampsActionOutput output;

  private TimeZone systemDefaultTimeZone;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    systemDefaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

    timestampsReader = mock(TimestampsReader.class);
    OngoingStubbing<Optional<Timestamp>> s = when(timestampsReader.read());
    s = s.thenReturn(ts(0, TimeUnit.SECONDS.toMillis(1)));
    s = s.thenReturn(ts(1, TimeUnit.MINUTES.toMillis(1)));
    s = s.thenReturn(ts(10, TimeUnit.HOURS.toMillis(1)));
    s = s.thenReturn(ts(100, TimeUnit.DAYS.toMillis(1)));
    s = s.thenReturn(ts(1000, TimeUnit.DAYS.toMillis(2)));
    s = s.thenReturn(ts(10000, TimeUnit.DAYS.toMillis(3)));
    s.thenReturn(Optional.<Timestamp> absent());

    logFileReader = mock(LogFileReader.class);
    when(logFileReader.nextLine()).thenReturn(Optional.of("line1"))
        .thenReturn(Optional.of("line2")).thenReturn(Optional.of("line3"))
        .thenReturn(Optional.of("line4")).thenReturn(Optional.of("line5"))
        .thenReturn(Optional.of("line6"))
        .thenReturn(Optional.<String> absent());
    when(logFileReader.lineCount()).thenReturn(6);

    output = new TimestampsActionOutput();
  }

  private Optional<Timestamp> ts(long elapsedMillis, long millisSinceEpoch) {
    return Optional.of(new Timestamp(elapsedMillis, millisSinceEpoch));
  }

  /**
   */
  @After
  public void tearDown() {
    TimeZone.setDefault(systemDefaultTimeZone);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite() throws Exception {
    output.setQuery(query);
    assertThat(readLines(), is(expectedResult));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_appendLog() throws Exception {
    output.setQuery(appendToQuery(query, "appendLog"));
    assertThat(readLines(), is(appendLog(expectedResult)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_appendLog_true() throws Exception {
    output.setQuery(appendToQuery(query, "appendLog=true"));
    assertThat(readLines(), is(appendLog(expectedResult)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_appendLog_false() throws Exception {
    output.setQuery(appendToQuery(query, "appendLog=false"));
    assertThat(readLines(), is(expectedResult));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_appendLog_prepend() throws Exception {
    output.setQuery(prependToQuery(query, "appendLog"));
    assertThat(readLines(), is(appendLog(expectedResult)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_changeCaseOfQueryParameterNames() throws Exception {
    output.setQuery(changeCaseOfQueryParameterNames(appendToQuery(query,
        "appendLog")));
    assertThat(readLines(), is(appendLog(expectedResult)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_startLine_two() throws Exception {
    output.setQuery(appendToQuery(query, "appendLog&startLine=2"));
    assertThat(readLines(), is(appendLog(expectedResult).subList(1, 6)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_startLine_one() throws Exception {
    output.setQuery(appendToQuery(query, "appendLog&startLine=1"));
    assertThat(readLines(), is(appendLog(expectedResult)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_startLine_zero() throws Exception {
    output.setQuery(appendToQuery(query, "appendLog&startLine=0"));
    assertThat(readLines(), is(appendLog(expectedResult)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_startLine_negative() throws Exception {
    output.setQuery(appendToQuery(query, "appendLog&startLine=-1"));
    assertThat(readLines(), is(asList(appendLog(expectedResult).get(5))));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_endLine_two() throws Exception {
    output.setQuery(appendToQuery(query, "appendLog&endLine=2"));
    assertThat(readLines(), is(appendLog(expectedResult).subList(0, 2)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_endLine_one() throws Exception {
    output.setQuery(appendToQuery(query, "appendLog&endLine=1"));
    assertThat(readLines(), is(asList(appendLog(expectedResult).get(0))));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_endLine_zero() throws Exception {
    output.setQuery(appendToQuery(query, "appendLog&endLine=0"));
    assertThat(readLines(), is(Collections.<String> emptyList()));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_endLine_negativeOne() throws Exception {
    output.setQuery(appendToQuery(query, "appendLog&endLine=-1"));
    assertThat(readLines(), is(appendLog(expectedResult)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_endLine_negativeTwo() throws Exception {
    output.setQuery(appendToQuery(query, "appendLog&endLine=-2"));
    assertThat(readLines(), is(appendLog(expectedResult).subList(0, 5)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_startLineAndEndLine() throws Exception {
    output.setQuery(appendToQuery(query, "appendLog&startLine=2&endLine=-2"));
    assertThat(readLines(), is(appendLog(expectedResult).subList(1, 5)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_startLineAndEndLine_lowercase() throws Exception {
    output.setQuery(appendToQuery(query, "appendLog&startline=2&endline=-2"));
    assertThat(readLines(), is(appendLog(expectedResult).subList(1, 5)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_startLineAndEndLine_bothPositive() throws Exception {
    output.setQuery(appendToQuery(query, "appendLog&startLine=2&endLine=5"));
    assertThat(readLines(), is(appendLog(expectedResult).subList(1, 5)));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_startLineAndEndLine_bothNegative() throws Exception {
    output.setQuery(appendToQuery(query, "appendLog&startLine=-4&endLine=-2"));
    assertThat(readLines(), is(appendLog(expectedResult).subList(2, 5)));

    // for efficiency, avoid counting the number of lines more than once
    verify(logFileReader, times(1)).lineCount();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_startLineAndEndLine_overlap() throws Exception {
    output.setQuery(appendToQuery(query, "appendLog&startLine=4&endLine=-4"));
    assertThat(readLines(), is(Collections.<String> emptyList()));
  }

  /**
   * @throws Exception
   */
  @Test(expected = IllegalArgumentException.class)
  public void testWrite_negativePrecision() throws Exception {
    output.setQuery(appendToQuery(query, "precision=-1"));
  }

  /**
   * @throws Exception
   */
  @Test(expected = NumberFormatException.class)
  public void testWrite_invalidPrecision() throws Exception {
    output.setQuery(appendToQuery(query, "precision=invalid"));
  }

  /**
   * @throws Exception
   */
  @Test(expected = NumberFormatException.class)
  public void testWrite_invalidStartLine() throws Exception {
    output.setQuery(appendToQuery(query, "startLine=invalid"));
  }

  /**
   * @throws Exception
   */
  @Test(expected = NumberFormatException.class)
  public void testWrite_invalidEndLine() throws Exception {
    output.setQuery(appendToQuery(query, "endLine=invalid"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_noTimestamps() throws Exception {
    when(timestampsReader.read()).thenReturn(Optional.<Timestamp> absent());
    output.setQuery(query);
    assertThat(readLines(), is(Collections.<String> emptyList()));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_noTimestamps_appendLog() throws Exception {
    when(timestampsReader.read()).thenReturn(Optional.<Timestamp> absent());
    output.setQuery(appendToQuery(query, "appendLog"));
    assertThat(readLines(),
        is(appendLog(listOfEmptyStrings(expectedResult.size()))));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_noLogFile() throws Exception {
    when(logFileReader.nextLine()).thenReturn(Optional.<String> absent());
    output.setQuery(query);
    assertThat(readLines(), is(expectedResult));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_noLogFile_appendLog() throws Exception {
    when(logFileReader.nextLine()).thenReturn(Optional.<String> absent());
    output.setQuery(appendToQuery(query, "appendLog"));
    assertThat(readLines(), is(expectedResult));
  }

  /**
   * Append additional parameters to the query string.
   * 
   * @param query
   * @param additional
   * @return the combined query string
   */
  private String appendToQuery(String query, String additional) {
    if (Strings.isNullOrEmpty(query)) {
      return additional;
    }
    return query + "&" + additional;
  }

  /**
   * Prepend an additional parameter to the query string.
   * 
   * @param query
   * @param additional
   * @return the combined query string
   */
  private String prependToQuery(String query, String additional) {
    if (Strings.isNullOrEmpty(query)) {
      return additional;
    }
    return additional + "&" + query;
  }

  /**
   * Append the expected log lines to the expected result.
   * 
   * @param lines
   * @return the expected result including the contents of the log
   */
  private List<String> appendLog(List<String> lines) {
    int i = 1;
    List<String> result = new ArrayList<String>();
    for (String line : lines) {
      String logLine = "line" + i;
      i++;

      result.add(line.isEmpty() ? logLine : line + " " + logLine);
    }
    return result;
  }

  /**
   * Change the case of all query parameter names.
   * 
   * @param query
   * @return the modified query
   */
  private String changeCaseOfQueryParameterNames(String query) {
    if (Strings.isNullOrEmpty(query)) {
      return query;
    }

    Pattern paramNamePattern = Pattern.compile("(^|\\&)(.+?)(\\=|\\&|$)");
    Matcher m = paramNamePattern.matcher(query);
    StringBuffer sb = new StringBuffer();
    while (m.find()) {
      String name = m.group();
      name = (name.toLowerCase().equals(name) ? name.toUpperCase() : name
          .toLowerCase());
      m.appendReplacement(sb, name);
    }
    m.appendTail(sb);
    String result = sb.toString();

    if (result.equals(query)) {
      throw new IllegalStateException(
          "Invalid test. No changes made to query: " + query);
    }
    return result;
  }

  /**
   * Create a list of empty strings.
   * 
   * @param size
   * @return a new list
   */
  private List<String> listOfEmptyStrings(int size) {
    List<String> emptyStrings = new ArrayList<String>();
    for (int i = 0; i < size; i++) {
      emptyStrings.add("");
    }
    return emptyStrings;
  }

  /**
   * Read all lines from the {@link TimestampsActionOutput}.
   * 
   * @return the output lines
   * @throws Exception
   */
  private List<String> readLines() throws Exception {
    List<String> lines = new ArrayList<String>();
    while (true) {
      Optional<String> line = output.nextLine(timestampsReader, logFileReader);
      if (!line.isPresent()) {
        return lines;
      }
      lines.add(line.get());
    }
  }
}
