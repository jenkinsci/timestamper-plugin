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
import hudson.model.Run;

import java.io.ByteArrayInputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit test for the ConsoleLogParser class.
 * 
 * @author Steven G. Brown
 */
public class ConsoleLogParserTest {

  /**
   */
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private ConsoleLogParser consoleLogParser;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    Run<?, ?> build = mock(Run.class);
    when(build.getRootDir()).thenReturn(folder.getRoot());
    byte[] consoleLog = new byte[] { 0x61, 0x0A, 0x61, 0x0A };
    when(build.getLogInputStream()).thenReturn(
        new ByteArrayInputStream(consoleLog));
    consoleLogParser = new ConsoleLogParser(build);
  }

  /**
   */
  @Test
  public void testDefaults() {
    assertThat(consoleLogParser.getLineNumber(), is(0));
    assertThat(consoleLogParser.atNewLine(), is(true));
    assertThat(consoleLogParser.endOfFile(), is(false));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekStart() throws Exception {
    consoleLogParser.seek(0);
    assertThat(consoleLogParser.getLineNumber(), is(0));
    assertThat(consoleLogParser.atNewLine(), is(true));
    assertThat(consoleLogParser.endOfFile(), is(false));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekWithinLine() throws Exception {
    consoleLogParser.seek(1);
    assertThat(consoleLogParser.getLineNumber(), is(0));
    assertThat(consoleLogParser.atNewLine(), is(false));
    assertThat(consoleLogParser.endOfFile(), is(false));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekNextLine() throws Exception {
    consoleLogParser.seek(2);
    assertThat(consoleLogParser.getLineNumber(), is(1));
    assertThat(consoleLogParser.atNewLine(), is(true));
    assertThat(consoleLogParser.endOfFile(), is(false));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testSeekPastEnd() throws Exception {
    consoleLogParser.seek(5);
    assertThat(consoleLogParser.getLineNumber(), is(2));
    assertThat(consoleLogParser.atNewLine(), is(true));
    assertThat(consoleLogParser.endOfFile(), is(true));
  }
}
