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
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.console.AnnotatedLargeText;
import hudson.model.Run;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;
import org.apache.commons.lang.SerializationUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Unit test for the {@link ConsoleLogParser} class.
 *
 * @author Steven G. Brown
 */
@RunWith(Parameterized.class)
public class ConsoleLogParserTest {

  private static final char NEWLINE = 0x0A;

  /** @return parameterised test data */
  @Parameters(name = "serialize={0},isBuilding={1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[] {false, false},
        new Object[] {false, true},
        new Object[] {true, false},
        new Object[] {true, true});
  }

  @Parameter(0)
  public boolean serialize;

  @Parameter(1)
  public boolean isBuilding;

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private Run<?, ?> build;

  private int logLength;

  @Before
  public void setUp() throws Exception {
    build = mock(Run.class);
    when(build.getRootDir()).thenReturn(folder.getRoot());
    byte[] consoleLog = new byte[] {0x61, NEWLINE, NEWLINE, NEWLINE, NEWLINE, 0x61, NEWLINE};
    logLength = consoleLog.length;
    when(build.getLogInputStream()).thenReturn(new ByteArrayInputStream(consoleLog));
    AnnotatedLargeText<?> logText = mock(AnnotatedLargeText.class);
    when(logText.length()).thenReturn((long) logLength);
    when(build.getLogText()).thenReturn(logText);
    when(build.isBuilding()).thenReturn(isBuilding);
  }

  @Test
  public void testSeekStart() throws Exception {
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = 0;
    result.atNewLine = true;
    assertThat(seek(0), is(result));
  }

  @Test
  public void testSeekWithinLine() throws Exception {
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = 0;
    assertThat(seek(1), is(result));
  }

  @Test
  public void testSeekNextLine() throws Exception {
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = 1;
    result.atNewLine = true;
    assertThat(seek(2), is(result));
  }

  @Test
  public void testSeekEnd() throws Exception {
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = 5;
    result.atNewLine = true;
    assertThat(seek(logLength), is(result));
  }

  @Test
  public void testSeekPastEnd() throws Exception {
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = 5;
    result.atNewLine = true;
    result.endOfFile = true;
    assertThat(seek(logLength + 1), is(result));
  }

  @Test
  public void testSeekStartNegative() throws Exception {
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = 0;
    result.atNewLine = true;
    assertThat(seek(-logLength), is(result));
  }

  @Test
  public void testSeekWithinLineNegative_isBuilding() throws Exception {
    assumeThat(isBuilding, is(true));
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = 0;
    assertThat(seek(1 - logLength), is(result));
  }

  @Test
  public void testSeekWithinLineNegative_notBuilding() throws Exception {
    assumeThat(isBuilding, is(false));
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = -5;
    assertThat(seek(1 - logLength), is(result));
  }

  @Test
  public void testSeekNextLineNegative_isBuilding() throws Exception {
    assumeThat(isBuilding, is(true));
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = 1;
    result.atNewLine = true;
    assertThat(seek(2 - logLength), is(result));
  }

  @Test
  public void testSeekNextLineNegative_notBuilding() throws Exception {
    assumeThat(isBuilding, is(false));
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = -4;
    result.atNewLine = true;
    assertThat(seek(2 - logLength), is(result));
  }

  @Test
  public void testSeekPastStartNegative() throws Exception {
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = 0;
    result.atNewLine = true;
    assertThat(seek(-logLength - 1), is(result));
  }

  private ConsoleLogParser.Result seek(long pos) throws Exception {
    ConsoleLogParser parser = new ConsoleLogParser(pos);
    if (serialize) {
      parser = (ConsoleLogParser) SerializationUtils.clone(parser);
    }
    return parser.seek(build);
  }
}
