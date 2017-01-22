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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.stubbing.OngoingStubbing;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.io.LogFileReader;
import hudson.plugins.timestamper.io.LogFileReader.Line;
import hudson.plugins.timestamper.io.TimestampsReader;

/**
 * Unit test for the {@link TimestampsActionOutput} class.
 * 
 * @author Steven G. Brown
 */
@RunWith(Parameterized.class)
public class TimestampsActionOutputTest {

  private static final Optional<Integer> NO_ENDLINE = Optional.absent();

  private static final Function<Timestamp, String> FORMAT = new Function<Timestamp, String>() {
    @Override
    public String apply(@Nonnull Timestamp timestamp) {
      return String.valueOf(timestamp.millisSinceEpoch);
    }
  };

  private static final Function<Timestamp, String> ELAPSED_FORMAT = new Function<Timestamp, String>() {
    @Override
    public String apply(@Nonnull Timestamp timestamp) {
      return String.valueOf(timestamp.elapsedMillis);
    }
  };

  private static final List<Timestamp> TIMESTAMPS = ImmutableList.of(new Timestamp(0, 1),
      //
      new Timestamp(1, 2),
      //
      new Timestamp(10, 3),
      //
      new Timestamp(100, 4),
      //
      new Timestamp(1000, 5),
      //
      new Timestamp(10000, 6));

  /**
   * @return the test data
   */
  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    List<Object[]> testCases = new ArrayList<Object[]>();

    testCases.add(new Object[] { "format",
        new TimestampsActionQuery(0, NO_ENDLINE, Collections.singletonList(FORMAT), false, false),
        Arrays.asList("1", "2", "3", "4", "5", "6") });

    testCases.add(new Object[] {
        "format + elapsed_format", new TimestampsActionQuery(0, NO_ENDLINE,
            ImmutableList.of(FORMAT, ELAPSED_FORMAT), false, false),
        Arrays.asList("1 0", "2 1", "3 10", "4 100", "5 1000", "6 10000") });

    testCases.add(new Object[] { "appendLogLine",
        new TimestampsActionQuery(0, NO_ENDLINE, Collections.singletonList(FORMAT), true, false),
        Arrays.asList("1  line1", "2  line2", "3  line3", "4  line4", "5  line5", "6  line6") });

    testCases.add(new Object[] { "currentTime",
        new TimestampsActionQuery(0, NO_ENDLINE, ImmutableList.of(FORMAT, ELAPSED_FORMAT), false, true),
        Arrays.asList("1 42") });

    // start line
    testCases.add(new Object[] { "start 2",
        new TimestampsActionQuery(2, NO_ENDLINE, Collections.singletonList(FORMAT), true, false),
        Arrays.asList("2  line2", "3  line3", "4  line4", "5  line5", "6  line6") });
    testCases.add(new Object[] { "start 1",
        new TimestampsActionQuery(1, NO_ENDLINE, Collections.singletonList(FORMAT), true, false),
        Arrays.asList("1  line1", "2  line2", "3  line3", "4  line4", "5  line5", "6  line6") });
    testCases.add(new Object[] { "start -1",
        new TimestampsActionQuery(-1, NO_ENDLINE, Collections.singletonList(FORMAT), true, false),
        Arrays.asList("6  line6") });
    testCases.add(new Object[] { "start -2",
        new TimestampsActionQuery(-2, NO_ENDLINE, Collections.singletonList(FORMAT), true, false),
        Arrays.asList("5  line5", "6  line6") });

    // end line
    testCases
        .add(
            new Object[] {
                "end 2", new TimestampsActionQuery(0, Optional.of(2),
                    Collections.singletonList(FORMAT), true, false),
                Arrays.asList("1  line1", "2  line2") });
    testCases.add(new Object[] { "end 1", new TimestampsActionQuery(0, Optional.of(1),
        Collections.singletonList(FORMAT), true, false), Arrays.asList("1  line1") });
    testCases.add(new Object[] { "end 0", new TimestampsActionQuery(0, Optional.of(0),
        Collections.singletonList(FORMAT), true, false), Collections.emptyList() });
    testCases.add(new Object[] { "end -1",
        new TimestampsActionQuery(0, Optional.of(-1), Collections.singletonList(FORMAT), true,
            false),
        Arrays.asList("1  line1", "2  line2", "3  line3", "4  line4", "5  line5", "6  line6") });
    testCases.add(new Object[] {
        "end -2", new TimestampsActionQuery(0, Optional.of(-2), Collections.singletonList(FORMAT),
            true, false),
        Arrays.asList("1  line1", "2  line2", "3  line3", "4  line4", "5  line5") });

    // start line and end line
    testCases.add(new Object[] {
        "start 2, end -2", new TimestampsActionQuery(2, Optional.of(-2),
            Collections.singletonList(FORMAT), true, false),
        Arrays.asList("2  line2", "3  line3", "4  line4", "5  line5") });
    testCases.add(new Object[] {
        "start 2, end 5", new TimestampsActionQuery(2, Optional.of(5),
            Collections.singletonList(FORMAT), true, false),
        Arrays.asList("2  line2", "3  line3", "4  line4", "5  line5") });
    testCases.add(new Object[] {
        "start -4, end -2", new TimestampsActionQuery(-4, Optional.of(-2),
            Collections.singletonList(FORMAT), true, false),
        Arrays.asList("3  line3", "4  line4", "5  line5") });
    testCases.add(new Object[] { "start 4, end -4", new TimestampsActionQuery(4, Optional.of(-4),
        Collections.singletonList(FORMAT), true, false), Collections.emptyList() });

    return testCases;
  }

  /**
   */
  @Parameter(0)
  public String testCaseDescription;

  /**
   */
  @Parameter(1)
  public TimestampsActionQuery query;

  /**
   */
  @Parameter(2)
  public List<String> expectedLines;

  private TimestampsReader timestampsReader;

  private LogFileReader logFileReader;

  private BufferedReader reader;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    timestampsReader = mock(TimestampsReader.class);
    OngoingStubbing<Optional<Timestamp>> readStubbing = when(timestampsReader.read());
    for (Timestamp timestamp : TIMESTAMPS) {
      readStubbing = readStubbing.thenReturn(Optional.of(timestamp));
    }
    readStubbing.thenReturn(Optional.<Timestamp>absent());

    List<Line> lines = new ArrayList<Line>();
    for (int i = 1; i <= TIMESTAMPS.size(); i++) {
      Line line = mock(Line.class);
      when(line.getText()).thenReturn("line" + i);
      when(line.readTimestamp()).thenReturn(Optional.<Timestamp>absent());
      lines.add(line);
    }

    logFileReader = mock(LogFileReader.class);
    OngoingStubbing<Optional<Line>> nextLineStubbing = when(logFileReader.nextLine());
    for (Line line : lines) {
      nextLineStubbing = nextLineStubbing.thenReturn(Optional.of(line));
    }
    nextLineStubbing.thenReturn(Optional.<Line>absent());
    when(logFileReader.lineCount()).thenReturn(6);

    reader = TimestampsActionOutput.open(timestampsReader, logFileReader, query,
        new Timestamp(42, 1));
  }

  /**
   * @throws Exception
   */
  @After
  public void tearDown() throws Exception {
    // for efficiency, avoid counting the number of lines more than once
    verify(logFileReader, atMost(1)).lineCount();

    reader.close();
  }

  /**
   */
  @Test
  public void testRead_eachCharacter() throws Exception {
    StringBuilder result = new StringBuilder();
    int character;
    while ((character = reader.read()) != -1) {
      result.append((char) character);
    }
    assertThat(result.toString(), is(joinLines(expectedLines)));
  }

  /**
   */
  @Test
  public void testRead_allAtOnce() throws Exception {
    String expectedResult = joinLines(expectedLines);
    char[] result = new char[expectedResult.length()];
    reader.read(result, 0, result.length);
    assertThat(String.valueOf(result), is(expectedResult));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRead_noTimestamps() throws Exception {
    assumeThat(query.currentTime, is(false));

    // Remove formatted timestamps from expected result
    if (query.appendLogLine) {
      expectedLines = Lists.transform(expectedLines, new Function<String, String>() {
        @Override
        public String apply(@Nonnull String input) {
          return input.replaceFirst("^.*(  \\w*)$", "$1");
        }
      });
    } else {
      expectedLines = Collections.emptyList();
    }
    when(timestampsReader.read()).thenReturn(Optional.<Timestamp>absent());
    assertThat(readLines(), is(expectedLines));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRead_timestampsInLogFileOnly() throws Exception {
    when(timestampsReader.read()).thenReturn(Optional.<Timestamp>absent());

    List<Line> lines = new ArrayList<Line>();
    int i = 1;
    for (Timestamp timestamp : TIMESTAMPS) {
      Line line = mock(Line.class);
      when(line.getText()).thenReturn("line" + i);
      when(line.readTimestamp()).thenReturn(Optional.of(timestamp));
      lines.add(line);
      i++;
    }

    OngoingStubbing<Optional<Line>> nextLineStubbing = when(logFileReader.nextLine());
    for (Line line : lines) {
      nextLineStubbing = nextLineStubbing.thenReturn(Optional.of(line));
    }
    nextLineStubbing.thenReturn(Optional.<Line>absent());

    assertThat(readLines(), is(expectedLines));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRead_noLogFile() throws Exception {
    if (query.appendLogLine) {
      // Remove log line from expected result
      expectedLines = Lists.transform(expectedLines, new Function<String, String>() {
        @Override
        public String apply(@Nonnull String input) {
          return input.replaceFirst("\\w*$", "");
        }
      });
    }
    when(logFileReader.nextLine()).thenReturn(Optional.<Line>absent());
    assertThat(readLines(), is(expectedLines));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRead_noTimestampsAndNoLogFile() throws Exception {
    assumeThat(query.currentTime, is(false));

    when(timestampsReader.read()).thenReturn(Optional.<Timestamp>absent());
    when(logFileReader.nextLine()).thenReturn(Optional.<Line>absent());
    assertThat(readLines(), is(Collections.<String>emptyList()));
  }

  private String joinLines(List<String> lines) {
    StringBuilder result = new StringBuilder();
    for (String line : lines) {
      result.append(line).append("\n");
    }
    return result.toString();
  }

  private List<String> readLines() throws Exception {
    List<String> lines = new ArrayList<String>();
    String line;
    while ((line = reader.readLine()) != null) {
      lines.add(line);
    }
    return lines;
  }
}
