/*
 * The MIT License
 * 
 * Copyright (c) 2012 Steven G. Brown
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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.model.Run;
import hudson.plugins.timestamper.TimestampNote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.io.input.NullInputStream;
import org.jvnet.hudson.test.HudsonTestCase;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Unit test for the {@link TimestampsAction} class.
 * 
 * @author Steven G. Brown
 */
public class TimestampsActionTest extends HudsonTestCase {

  private Run<?, ?> build;

  private TimestampsAction action;

  private StaplerRequest request;

  private StringBuilder written;

  private PrintWriter writer;

  private StaplerResponse response;

  /**
   * {@inheritDoc}
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();

    build = mock(Run.class);
    when(build.getLogInputStream()).thenReturn(new NullInputStream(0));
    action = new TimestampsAction(build);
    request = mock(StaplerRequest.class);

    written = new StringBuilder();
    writer = mock(PrintWriter.class);
    doAnswer(new Answer<Void>() {
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String arg = (String) invocation.getArguments()[0];
        written.append(arg);
        return null;
      }
    }).when(writer).write(anyString());
    response = mock(StaplerResponse.class);
    when(response.getWriter()).thenReturn(writer);
  }

  /**
   * @throws Exception
   */
  public void testNoLogFile() throws Exception {
    action.doIndex(request, response);
    assertThat(written.toString(), is(""));
  }

  /**
   * @throws Exception
   */
  public void testReadConsoleNotesDefaultPrecision() throws Exception {
    writeConsoleWithNotes();
    action.doIndex(request, response);
    assertThat(written.toString(), is("0.001\n" + "0.010\n" + "0.100\n"
        + "1.000\n" + "10.000\n"));
  }

  /**
   * @throws Exception
   */
  public void testReadConsoleNotesDefaultZeroPrecision() throws Exception {
    writeConsoleWithNotes();
    when(request.getParameter("precision")).thenReturn("0");
    action.doIndex(request, response);
    assertThat(written.toString(), is("0\n" + "0\n" + "0\n" + "1\n" + "10\n"));
  }

  /**
   * @throws Exception
   */
  public void testReadConsoleNotesDefaultSecondsPrecision() throws Exception {
    writeConsoleWithNotes();
    when(request.getParameter("precision")).thenReturn("seconds");
    action.doIndex(request, response);
    assertThat(written.toString(), is("0\n" + "0\n" + "0\n" + "1\n" + "10\n"));
  }

  /**
   * @throws Exception
   */
  public void testReadConsoleNotesOnePrecision() throws Exception {
    writeConsoleWithNotes();
    when(request.getParameter("precision")).thenReturn("1");
    action.doIndex(request, response);
    assertThat(written.toString(), is("0.0\n" + "0.0\n" + "0.1\n" + "1.0\n"
        + "10.0\n"));
  }

  /**
   * @throws Exception
   */
  public void testReadConsoleNotesTwoPrecision() throws Exception {
    writeConsoleWithNotes();
    when(request.getParameter("precision")).thenReturn("2");
    action.doIndex(request, response);
    assertThat(written.toString(), is("0.00\n" + "0.01\n" + "0.10\n" + "1.00\n"
        + "10.00\n"));
  }

  /**
   * @throws Exception
   */
  public void testReadConsoleNotesThreePrecision() throws Exception {
    writeConsoleWithNotes();
    when(request.getParameter("precision")).thenReturn("3");
    action.doIndex(request, response);
    assertThat(written.toString(), is("0.001\n" + "0.010\n" + "0.100\n"
        + "1.000\n" + "10.000\n"));
  }

  /**
   * @throws Exception
   */
  public void testReadConsoleNotesMillisecondsPrecision() throws Exception {
    writeConsoleWithNotes();
    when(request.getParameter("precision")).thenReturn("milliseconds");
    action.doIndex(request, response);
    assertThat(written.toString(), is("0.001\n" + "0.010\n" + "0.100\n"
        + "1.000\n" + "10.000\n"));
  }

  /**
   * @throws Exception
   */
  public void testReadConsoleNotesSixPrecision() throws Exception {
    writeConsoleWithNotes();
    when(request.getParameter("precision")).thenReturn("6");
    action.doIndex(request, response);
    assertThat(written.toString(), is("0.001000\n" + "0.010000\n"
        + "0.100000\n" + "1.000000\n" + "10.000000\n"));
  }

  /**
   * @throws Exception
   */
  public void testReadConsoleNotesMicrosecondsPrecision() throws Exception {
    writeConsoleWithNotes();
    when(request.getParameter("precision")).thenReturn("microseconds");
    action.doIndex(request, response);
    assertThat(written.toString(), is("0.001000\n" + "0.010000\n"
        + "0.100000\n" + "1.000000\n" + "10.000000\n"));
  }

  /**
   * @throws Exception
   */
  public void testReadConsoleNotesNanosecondsPrecision() throws Exception {
    writeConsoleWithNotes();
    when(request.getParameter("precision")).thenReturn("nanoseconds");
    action.doIndex(request, response);
    assertThat(written.toString(), is("0.001000000\n" + "0.010000000\n"
        + "0.100000000\n" + "1.000000000\n" + "10.000000000\n"));
  }

  /**
   * @throws Exception
   */
  public void testReadConsoleNotesNegativePrecision() throws Exception {
    writeConsoleWithNotes();
    when(request.getParameter("precision")).thenReturn("-1");
    action.doIndex(request, response);
    assertThat(written.toString(), is("0.001\n" + "0.010\n" + "0.100\n"
        + "1.000\n" + "10.000\n"));
  }

  /**
   * @throws Exception
   */
  public void testReadConsoleNotesInvalidPrecision() throws Exception {
    writeConsoleWithNotes();
    when(request.getParameter("precision")).thenReturn("invalid");
    action.doIndex(request, response);
    assertThat(written.toString(), is("0.001\n" + "0.010\n" + "0.100\n"
        + "1.000\n" + "10.000\n"));
  }

  private void writeConsoleWithNotes() throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    long millisSinceEpoch = 1;
    for (int i = 0; i < 5; i++) {
      TimestampNote timestampNote = new TimestampNote(millisSinceEpoch);
      timestampNote.encodeTo(outputStream);
      outputStream.write('a' + i);
      millisSinceEpoch *= 10;
    }
    byte[] consoleLog = outputStream.toByteArray();
    when(build.getLogInputStream()).thenReturn(
        new ByteArrayInputStream(consoleLog));
  }
}
