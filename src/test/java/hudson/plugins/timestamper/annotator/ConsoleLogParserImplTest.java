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

  private String consoleLog;

  private int logLength;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    consoleLog = new String(new byte[] { 0x61, NEWLINE, NEWLINE, NEWLINE, NEWLINE,
            0x61, NEWLINE });
    logLength = consoleLog.length();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekStart() throws Exception {
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = 0;
    result.atNewLine = true;
    assertThat(seek(0), is(result));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekWithinLine() throws Exception {
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = 0;
    assertThat(seek(1), is(result));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekNextLine() throws Exception {
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = 1;
    result.atNewLine = true;
    assertThat(seek(2), is(result));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekEnd() throws Exception {
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = 5;
    result.atNewLine = true;
    assertThat(seek(logLength), is(result));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekPastEnd() throws Exception {
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = 5;
    result.atNewLine = true;
    result.endOfFile = true;
    assertThat(seek(logLength + 1), is(result));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekStartNegative() throws Exception {
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = 0;
    result.atNewLine = true;
    assertThat(seek(-logLength), is(result));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekWithinLineNegative() throws Exception {
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = 0;
    assertThat(seek(1 - logLength), is(result));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekPastStartNegative() throws Exception {
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.lineNumber = 0;
    result.atNewLine = true;
    assertThat(seek(-logLength - 1), is(result));
  }

  private ConsoleLogParserImpl.Result seek(long pos) throws Exception {
    ConsoleLogParserImpl parser = new ConsoleLogParserImpl(pos);
    if (serialize) {
      parser = (ConsoleLogParserImpl) SerializationUtils.clone(parser);
    }
    return parser.seek(consoleLog, logLength);
  }
}
