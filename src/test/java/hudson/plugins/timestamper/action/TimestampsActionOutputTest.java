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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.io.TimestampsReader;

import java.io.PrintWriter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.google.common.base.Optional;

/**
 * Unit test for the {@link TimestampsActionOutput} class.
 * 
 * @author Steven G. Brown
 */
@RunWith(MockitoJUnitRunner.class)
public class TimestampsActionOutputTest {

  @Mock
  private TimestampsReader reader;

  @Mock
  private PrintWriter writer;

  private StringBuilder written;

  @Mock
  private StaplerRequest request;

  private TimestampsActionOutput output;

  /**
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    when(reader.read()).thenReturn(Optional.of(new Timestamp(0, 0)),
        Optional.of(new Timestamp(1, 0)), Optional.of(new Timestamp(10, 0)),
        Optional.of(new Timestamp(100, 0)),
        Optional.of(new Timestamp(1000, 0)),
        Optional.of(new Timestamp(10000, 0)), Optional.<Timestamp> absent());

    written = new StringBuilder();
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        String arg = (String) invocation.getArguments()[0];
        written.append(arg);
        return null;
      }
    }).when(writer).write(anyString());

    output = new TimestampsActionOutput();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_defaultPrecision() throws Exception {
    output.write(reader, writer, request);
    assertThat(written.toString(), is("0.000\n" + "0.001\n" + "0.010\n"
        + "0.100\n" + "1.000\n" + "10.000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_zeroPrecision() throws Exception {
    when(request.getParameter("precision")).thenReturn("0");
    output.write(reader, writer, request);
    assertThat(written.toString(), is("0\n" + "0\n" + "0\n" + "0\n" + "1\n"
        + "10\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_secondsPrecision() throws Exception {
    when(request.getParameter("precision")).thenReturn("seconds");
    output.write(reader, writer, request);
    assertThat(written.toString(), is("0\n" + "0\n" + "0\n" + "0\n" + "1\n"
        + "10\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_onePrecision() throws Exception {
    when(request.getParameter("precision")).thenReturn("1");
    output.write(reader, writer, request);
    assertThat(written.toString(), is("0.0\n" + "0.0\n" + "0.0\n" + "0.1\n"
        + "1.0\n" + "10.0\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_twoPrecision() throws Exception {
    when(request.getParameter("precision")).thenReturn("2");
    output.write(reader, writer, request);
    assertThat(written.toString(), is("0.00\n" + "0.00\n" + "0.01\n" + "0.10\n"
        + "1.00\n" + "10.00\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_threePrecision() throws Exception {
    when(request.getParameter("precision")).thenReturn("3");
    output.write(reader, writer, request);
    assertThat(written.toString(), is("0.000\n" + "0.001\n" + "0.010\n"
        + "0.100\n" + "1.000\n" + "10.000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_millisecondsPrecision() throws Exception {
    when(request.getParameter("precision")).thenReturn("milliseconds");
    output.write(reader, writer, request);
    assertThat(written.toString(), is("0.000\n" + "0.001\n" + "0.010\n"
        + "0.100\n" + "1.000\n" + "10.000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_sixPrecision() throws Exception {
    when(request.getParameter("precision")).thenReturn("6");
    output.write(reader, writer, request);
    assertThat(written.toString(), is("0.000000\n" + "0.001000\n"
        + "0.010000\n" + "0.100000\n" + "1.000000\n" + "10.000000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_microsecondsPrecision() throws Exception {
    when(request.getParameter("precision")).thenReturn("microseconds");
    output.write(reader, writer, request);
    assertThat(written.toString(), is("0.000000\n" + "0.001000\n"
        + "0.010000\n" + "0.100000\n" + "1.000000\n" + "10.000000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_nanosecondsPrecision() throws Exception {
    when(request.getParameter("precision")).thenReturn("nanoseconds");
    output.write(reader, writer, request);
    assertThat(written.toString(), is("0.000000000\n" + "0.001000000\n"
        + "0.010000000\n" + "0.100000000\n" + "1.000000000\n"
        + "10.000000000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_emptyPrecision() throws Exception {
    when(request.getParameter("precision")).thenReturn("");
    output.write(reader, writer, request);
    assertThat(written.toString(), is("0.000\n" + "0.001\n" + "0.010\n"
        + "0.100\n" + "1.000\n" + "10.000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_negativePrecision() throws Exception {
    when(request.getParameter("precision")).thenReturn("-1");
    output.write(reader, writer, request);
    assertThat(written.toString(), is("0.000\n" + "0.001\n" + "0.010\n"
        + "0.100\n" + "1.000\n" + "10.000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_invalidPrecision() throws Exception {
    when(request.getParameter("precision")).thenReturn("invalid");
    output.write(reader, writer, request);
    assertThat(written.toString(), is("0.000\n" + "0.001\n" + "0.010\n"
        + "0.100\n" + "1.000\n" + "10.000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_noTimestamps() throws Exception {
    when(reader.read()).thenReturn(Optional.<Timestamp> absent());
    output.write(reader, writer, request);
    assertThat(written.toString(), is(""));
  }
}
