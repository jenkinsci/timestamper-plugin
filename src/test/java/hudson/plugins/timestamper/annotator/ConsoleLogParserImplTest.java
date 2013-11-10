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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.console.AnnotatedLargeText;
import hudson.model.Run;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Unit test for the ConsoleLogParserImpl class.
 * 
 * @author Steven G. Brown
 */
@RunWith(Parameterized.class)
public class ConsoleLogParserImplTest {

  private static final char NEWLINE = 0x0A;

  /**
   * @return parameterised test data
   */
  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[] { false }, new Object[] { true });
  }

  /**
   */
  @Parameter
  public boolean serialize;

  /**
   */
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private Run<?, ?> build;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    build = mock(Run.class);
    when(build.getRootDir()).thenReturn(folder.getRoot());
    byte[] consoleLog = new byte[] { 0x61, NEWLINE, 0x61, NEWLINE };
    when(build.getLogInputStream()).thenReturn(
        new ByteArrayInputStream(consoleLog));
    AnnotatedLargeText<?> logText = mock(AnnotatedLargeText.class);
    when(logText.length()).thenReturn((long) consoleLog.length);
    when(build.getLogText()).thenReturn(logText);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekStart() throws Exception {
    assertThat(seek(0), is(resultWith(lineNumber(0), atNewLine())));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekWithinLine() throws Exception {
    assertThat(seek(1), is(resultWith(lineNumber(0))));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekNextLine() throws Exception {
    assertThat(seek(2), is(resultWith(lineNumber(1), atNewLine())));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekEnd() throws Exception {
    assertThat(seek(4), is(resultWith(lineNumber(2), atNewLine())));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekPastEnd() throws Exception {
    assertThat(seek(5), is(resultWith(lineNumber(2), atNewLine(), endOfFile())));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekStartNegative() throws Exception {
    assertThat(seek(-4), is(resultWith(lineNumber(0), atNewLine())));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekWithinLineNegative() throws Exception {
    assertThat(seek(-3), is(resultWith(lineNumber(0))));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekPastStartNegative() throws Exception {
    assertThat(seek(-5), is(resultWith(lineNumber(0), atNewLine())));
  }

  private ConsoleLogParserImpl.Result seek(long pos) throws Exception {
    ConsoleLogParserImpl parser = new ConsoleLogParserImpl(pos);
    if (serialize) {
      parser = (ConsoleLogParserImpl) SerializationUtils.clone(parser);
    }
    return parser.seek(build);
  }

  private static Matcher<ConsoleLogParserImpl.Result> resultWith(
      ResultProperty... properties) {
    final ConsoleLogParserImpl.Result expectedResult = new ConsoleLogParserImpl.Result();
    for (ResultProperty property : properties) {
      property.set(expectedResult);
    }
    return new CustomMatcher<ConsoleLogParserImpl.Result>(
        expectedResult.toString()) {
      @Override
      public boolean matches(Object item) {
        return EqualsBuilder.reflectionEquals(item, expectedResult);
      }
    };
  }

  private static interface ResultProperty {
    void set(ConsoleLogParserImpl.Result result);
  }

  private static ResultProperty lineNumber(final int lineNumber) {
    return new ResultProperty() {
      @Override
      public void set(ConsoleLogParserImpl.Result result) {
        result.lineNumber = lineNumber;
      }
    };
  }

  private static ResultProperty atNewLine() {
    return new ResultProperty() {
      @Override
      public void set(ConsoleLogParserImpl.Result result) {
        result.atNewLine = true;
      }
    };
  }

  private static ResultProperty endOfFile() {
    return new ResultProperty() {
      @Override
      public void set(ConsoleLogParserImpl.Result result) {
        result.endOfFile = true;
      }
    };
  }
}
