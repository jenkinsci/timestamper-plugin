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

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import hudson.plugins.timestamper.format.ElapsedTimestampFormat;
import hudson.plugins.timestamper.format.SystemTimestampFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Unit test for the {@link TimestampsActionQuery} class.
 *
 * @author Steven G. Brown
 */
@RunWith(Parameterized.class)
public class TimestampsActionQueryTest {

  private static final Optional<Integer> NO_ENDLINE = Optional.empty();

  private static final Optional<String> NO_TIMEZONE = Optional.empty();

  private static final TimestampsActionQuery DEFAULT =
      new TimestampsActionQuery(
          0, NO_ENDLINE, Collections.singletonList(new PrecisionTimestampFormat(3)), false, false);

  /** @return the test data */
  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    List<Object[]> testCases = new ArrayList<>();

    // No query
    testCases.add(new Object[] {"", DEFAULT});
    testCases.add(new Object[] {null, DEFAULT});

    // Precision format
    for (int precision = 0; precision <= 9; precision++) {
      testCases.add(
          new Object[] {
            "precision=" + precision,
            new TimestampsActionQuery(
                0,
                NO_ENDLINE,
                Collections.singletonList(new PrecisionTimestampFormat(precision)),
                false,
                false)
          });
    }
    List<String> precisionStrings =
        Arrays.asList("seconds", "milliseconds", "microseconds", "nanoseconds");
    for (int i = 0; i < precisionStrings.size(); i++) {
      testCases.add(
          new Object[] {
            "precision=" + precisionStrings.get(i),
            new TimestampsActionQuery(
                0,
                NO_ENDLINE,
                Collections.singletonList(new PrecisionTimestampFormat(i * 3)),
                false,
                false)
          });
    }
    testCases.addAll(
        Arrays.asList(
            new Object[][] {
              {"precision=-1", IllegalArgumentException.class},
              {"precision=invalid", NumberFormatException.class}
            }));

    // Time format
    testCases.add(
        new Object[] {
          "time=dd:HH:mm:ss",
          new TimestampsActionQuery(
              0,
              NO_ENDLINE,
              Collections.singletonList(
                  new SystemTimestampFormat("dd:HH:mm:ss", NO_TIMEZONE, Locale.getDefault())),
              false,
              false)
        });
    testCases.add(
        new Object[] {
          "time=dd:HH:mm:ss&timeZone=GMT+10",
          new TimestampsActionQuery(
              0,
              NO_ENDLINE,
              Collections.singletonList(
                  new SystemTimestampFormat(
                      "dd:HH:mm:ss", Optional.of("GMT+10"), Locale.getDefault())),
              false,
              false)
        });
    testCases.add(
        new Object[] {
          "time=dd:HH:mm:ss&timeZone=GMT-10",
          new TimestampsActionQuery(
              0,
              NO_ENDLINE,
              Collections.singletonList(
                  new SystemTimestampFormat(
                      "dd:HH:mm:ss", Optional.of("GMT-10"), Locale.getDefault())),
              false,
              false)
        });
    testCases.add(
        new Object[] {
          "time=EEEE, d MMMM&locale=en",
          new TimestampsActionQuery(
              0,
              NO_ENDLINE,
              Collections.singletonList(
                  new SystemTimestampFormat("EEEE, d MMMM", NO_TIMEZONE, Locale.ENGLISH)),
              false,
              false)
        });
    testCases.add(
        new Object[] {
          "time=EEEE, d MMMM&locale=de",
          new TimestampsActionQuery(
              0,
              NO_ENDLINE,
              Collections.singletonList(
                  new SystemTimestampFormat("EEEE, d MMMM", NO_TIMEZONE, Locale.GERMAN)),
              false,
              false)
        });

    // Elapsed format
    testCases.add(
        new Object[] {
          "elapsed=s.SSS",
          new TimestampsActionQuery(
              0,
              NO_ENDLINE,
              Collections.singletonList(new ElapsedTimestampFormat("s.SSS")),
              false,
              false)
        });

    // Multiple formats
    testCases.add(
        new Object[] {
          "precision=0&precision=1",
          new TimestampsActionQuery(
              0,
              NO_ENDLINE,
              ImmutableList.of(new PrecisionTimestampFormat(0), new PrecisionTimestampFormat(1)),
              false,
              false)
        });
    testCases.add(
        new Object[] {
          "time=dd:HH:mm:ss&elapsed=s.SSS",
          new TimestampsActionQuery(
              0,
              NO_ENDLINE,
              ImmutableList.of(
                  new SystemTimestampFormat("dd:HH:mm:ss", NO_TIMEZONE, Locale.getDefault()),
                  new ElapsedTimestampFormat("s.SSS")),
              false,
              false)
        });

    // Start line and end line
    List<Optional<Integer>> lineValues =
        ImmutableList.of(
            Optional.of(-1), Optional.of(0), Optional.of(1), Optional.empty());
    for (Optional<Integer> startLine : lineValues) {
      for (Optional<Integer> endLine : lineValues) {
        List<String> params = new ArrayList<>();
        startLine.ifPresent(integer -> params.add("startLine=" + integer));
        endLine.ifPresent(integer -> params.add("endLine=" + integer));
        String query = Joiner.on('&').join(params);

        if (!query.isEmpty()) {
          testCases.add(
              new Object[] {
                query,
                new TimestampsActionQuery(
                    startLine.orElse(0), endLine, DEFAULT.timestampFormats, false, false)
              });
        }
      }
    }
    testCases.add(new Object[] {"startLine=invalid", NumberFormatException.class});
    testCases.add(new Object[] {"endLine=invalid", NumberFormatException.class});

    // Append log line
    Map<String, Boolean> appendLogParams =
        ImmutableMap.of("appendLog", true, "appendLog=true", true, "appendLog=false", false);
    for (Map.Entry<String, Boolean> mapEntry : appendLogParams.entrySet()) {
      String appendLogParam = mapEntry.getKey();
      boolean appendLog = mapEntry.getValue();

      testCases.add(
          new Object[] {
            appendLogParam,
            new TimestampsActionQuery(0, NO_ENDLINE, DEFAULT.timestampFormats, appendLog, false)
          });
      testCases.add(
          new Object[] {
            "precision=0&" + appendLogParam,
            new TimestampsActionQuery(
                0,
                NO_ENDLINE,
                Collections.singletonList(new PrecisionTimestampFormat(0)),
                appendLog,
                false)
          });
      testCases.add(
          new Object[] {
            appendLogParam + "&precision=0",
            new TimestampsActionQuery(
                0,
                NO_ENDLINE,
                Collections.singletonList(new PrecisionTimestampFormat(0)),
                appendLog,
                false)
          });
    }

    // Current time
    Map<String, Boolean> currentTimeParams =
        ImmutableMap.of("currentTime", true, "currentTime=true", true, "currentTime=false", false);
    for (Map.Entry<String, Boolean> mapEntry : currentTimeParams.entrySet()) {
      String currentTimeParam = mapEntry.getKey();
      boolean currentTime = mapEntry.getValue();

      testCases.add(
          new Object[] {
            currentTimeParam,
            new TimestampsActionQuery(0, NO_ENDLINE, DEFAULT.timestampFormats, false, currentTime)
          });
      testCases.add(
          new Object[] {
            "precision=0&" + currentTimeParam,
            new TimestampsActionQuery(
                0,
                NO_ENDLINE,
                Collections.singletonList(new PrecisionTimestampFormat(0)),
                false,
                currentTime)
          });
      testCases.add(
          new Object[] {
            currentTimeParam + "&precision=0",
            new TimestampsActionQuery(
                0,
                NO_ENDLINE,
                Collections.singletonList(new PrecisionTimestampFormat(0)),
                false,
                currentTime)
          });
    }

    return testCases;
  }

  @Parameter(0)
  public String queryString;

  @Parameter(1)
  public Object expectedResult;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testCreate() {
    if (expectedResult instanceof Class<?>) {
      @SuppressWarnings("unchecked")
      Class<? extends Throwable> expectedThrowable = (Class<? extends Throwable>) expectedResult;
      thrown.expect(expectedThrowable);
    }
    TimestampsActionQuery query = TimestampsActionQuery.create(queryString);
    assertThat(query, is(expectedResult));
  }

  @Test
  public void testCreate_changeCaseOfQueryParameterNames() {
    queryString = changeCaseOfQueryParameterNames(queryString);
    testCreate();
  }

  @Test
  public void testEqualsAndHashCode() {
    EqualsVerifier.forClass(TimestampsActionQuery.class).suppress(Warning.NULL_FIELDS).verify();
  }

  /**
   * Change the case of all query parameter names.
   *
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
      name = (name.toLowerCase().equals(name) ? name.toUpperCase() : name.toLowerCase());
      m.appendReplacement(sb, name);
    }
    m.appendTail(sb);
    String result = sb.toString();

    if (result.equals(query)) {
      throw new IllegalStateException("Invalid test. No changes made to query: " + query);
    }
    return result;
  }
}
