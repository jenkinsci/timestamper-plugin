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
import hudson.PluginManager;
import hudson.model.Run;
import hudson.plugins.timestamper.TimestampNote;
import hudson.plugins.timestamper.action.TimestampsActionTest.NoLogFileTest;
import hudson.plugins.timestamper.action.TimestampsActionTest.TimestampNotesTest;
import hudson.plugins.timestamper.action.TimestampsActionTest.TimestampWriterTest;
import hudson.plugins.timestamper.io.TimestampsWriter;
import hudson.plugins.timestamper.io.TimestampsWriterImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jenkins.model.Jenkins;

import org.apache.commons.io.input.NullInputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

/**
 * Unit test for the {@link TimestampsAction} class.
 * 
 * @author Steven G. Brown
 */
@RunWith(Suite.class)
@SuiteClasses({ NoLogFileTest.class, TimestampNotesTest.class,
    TimestampWriterTest.class })
@SuppressWarnings("boxing")
public class TimestampsActionTest {

  static final List<Long> millisSinceEpochToWrite = Arrays.asList(0l, 1l, 10l,
      100l, 1000l, 10000l);

  /**
   */
  @RunWith(PowerMockRunner.class)
  @PrepareForTest(Jenkins.class)
  public static abstract class SetUp {

    /**
     */
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    Run<?, ?> build;

    TimestampsAction action;

    StaplerRequest request;

    StringBuilder written;

    PrintWriter writer;

    StaplerResponse response;

    /**
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
      build = mock(Run.class);
      when(build.getRootDir()).thenReturn(folder.getRoot());
      when(build.getLogInputStream()).thenReturn(new NullInputStream(0));
      action = new TimestampsAction(build);
      request = mock(StaplerRequest.class);

      written = new StringBuilder();
      writer = mock(PrintWriter.class);
      doAnswer(new Answer<Void>() {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
          String arg = (String) invocation.getArguments()[0];
          written.append(arg);
          return null;
        }
      }).when(writer).write(anyString());
      response = mock(StaplerResponse.class);
      when(response.getWriter()).thenReturn(writer);

      // Need to mock Jenkins to read the console notes.
      Jenkins jenkins = mock(Jenkins.class);
      PluginManager pluginManager = mock(PluginManager.class);
      Whitebox.setInternalState(jenkins, PluginManager.class, pluginManager);
      PowerMockito.mockStatic(Jenkins.class);
      when(Jenkins.getInstance()).thenReturn(jenkins);
    }
  }

  /**
   */
  public static abstract class ReadTimestampsTests extends SetUp {

    /**
     * @throws Exception
     */
    @Test
    public void testReadTimestampsDefaultPrecision() throws Exception {
      action.doIndex(request, response);
      assertThat(written.toString(), is("0.000\n" + "0.001\n" + "0.010\n"
          + "0.100\n" + "1.000\n" + "10.000\n"));
    }

    /**
     * @throws Exception
     */
    @Test
    public void testReadTimestampsDefaultZeroPrecision() throws Exception {
      when(request.getParameter("precision")).thenReturn("0");
      action.doIndex(request, response);
      assertThat(written.toString(), is("0\n" + "0\n" + "0\n" + "0\n" + "1\n"
          + "10\n"));
    }

    /**
     * @throws Exception
     */
    @Test
    public void testReadTimestampsDefaultSecondsPrecision() throws Exception {
      when(request.getParameter("precision")).thenReturn("seconds");
      action.doIndex(request, response);
      assertThat(written.toString(), is("0\n" + "0\n" + "0\n" + "0\n" + "1\n"
          + "10\n"));
    }

    /**
     * @throws Exception
     */
    @Test
    public void testReadTimestampsOnePrecision() throws Exception {
      when(request.getParameter("precision")).thenReturn("1");
      action.doIndex(request, response);
      assertThat(written.toString(), is("0.0\n" + "0.0\n" + "0.0\n" + "0.1\n"
          + "1.0\n" + "10.0\n"));
    }

    /**
     * @throws Exception
     */
    @Test
    public void testReadTimestampsTwoPrecision() throws Exception {
      when(request.getParameter("precision")).thenReturn("2");
      action.doIndex(request, response);
      assertThat(written.toString(), is("0.00\n" + "0.00\n" + "0.01\n"
          + "0.10\n" + "1.00\n" + "10.00\n"));
    }

    /**
     * @throws Exception
     */
    @Test
    public void testReadTimestampsThreePrecision() throws Exception {
      when(request.getParameter("precision")).thenReturn("3");
      action.doIndex(request, response);
      assertThat(written.toString(), is("0.000\n" + "0.001\n" + "0.010\n"
          + "0.100\n" + "1.000\n" + "10.000\n"));
    }

    /**
     * @throws Exception
     */
    @Test
    public void testReadTimestampsMillisecondsPrecision() throws Exception {
      when(request.getParameter("precision")).thenReturn("milliseconds");
      action.doIndex(request, response);
      assertThat(written.toString(), is("0.000\n" + "0.001\n" + "0.010\n"
          + "0.100\n" + "1.000\n" + "10.000\n"));
    }

    /**
     * @throws Exception
     */
    @Test
    public void testReadTimestampsSixPrecision() throws Exception {
      when(request.getParameter("precision")).thenReturn("6");
      action.doIndex(request, response);
      assertThat(written.toString(), is("0.000000\n" + "0.001000\n"
          + "0.010000\n" + "0.100000\n" + "1.000000\n" + "10.000000\n"));
    }

    /**
     * @throws Exception
     */
    @Test
    public void testReadTimestampsMicrosecondsPrecision() throws Exception {
      when(request.getParameter("precision")).thenReturn("microseconds");
      action.doIndex(request, response);
      assertThat(written.toString(), is("0.000000\n" + "0.001000\n"
          + "0.010000\n" + "0.100000\n" + "1.000000\n" + "10.000000\n"));
    }

    /**
     * @throws Exception
     */
    @Test
    public void testReadTimestampsNanosecondsPrecision() throws Exception {
      when(request.getParameter("precision")).thenReturn("nanoseconds");
      action.doIndex(request, response);
      assertThat(written.toString(), is("0.000000000\n" + "0.001000000\n"
          + "0.010000000\n" + "0.100000000\n" + "1.000000000\n"
          + "10.000000000\n"));
    }

    /**
     * @throws Exception
     */
    @Test
    public void testReadTimestampsEmptyPrecision() throws Exception {
      when(request.getParameter("precision")).thenReturn("");
      action.doIndex(request, response);
      assertThat(written.toString(), is("0.000\n" + "0.001\n" + "0.010\n"
          + "0.100\n" + "1.000\n" + "10.000\n"));
    }

    /**
     * @throws Exception
     */
    @Test
    public void testReadTimestampsNegativePrecision() throws Exception {
      when(request.getParameter("precision")).thenReturn("-1");
      action.doIndex(request, response);
      assertThat(written.toString(), is("0.000\n" + "0.001\n" + "0.010\n"
          + "0.100\n" + "1.000\n" + "10.000\n"));
    }

    /**
     * @throws Exception
     */
    @Test
    public void testReadTimestampsInvalidPrecision() throws Exception {
      when(request.getParameter("precision")).thenReturn("invalid");
      action.doIndex(request, response);
      assertThat(written.toString(), is("0.000\n" + "0.001\n" + "0.010\n"
          + "0.100\n" + "1.000\n" + "10.000\n"));
    }
  }

  /**
   */
  public static class NoLogFileTest extends SetUp {

    /**
     * @throws Exception
     */
    @Test
    public void testNoLogFile() throws Exception {
      action.doIndex(request, response);
      assertThat(written.toString(), is(""));
    }
  }

  /**
   */
  public static class TimestampNotesTest extends ReadTimestampsTests {

    /**
     * @throws Exception
     */
    @Before
    public void writeTimestamps() throws Exception {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      int i = 0;
      for (long millisSinceEpoch : millisSinceEpochToWrite) {
        TimestampNote timestampNote = new TimestampNote(millisSinceEpoch);
        timestampNote.encodeTo(outputStream);
        outputStream.write('a' + i);
        i++;
      }
      byte[] consoleLog = outputStream.toByteArray();
      when(build.getLogInputStream()).thenReturn(
          new ByteArrayInputStream(consoleLog));
    }
  }

  /**
   */
  public static class TimestampWriterTest extends ReadTimestampsTests {

    /**
     * @throws Exception
     */
    @Before
    public void writeTimestamps() throws Exception {
      TimestampsWriter writer = new TimestampsWriterImpl(build);
      try {
        for (long millisSinceEpoch : millisSinceEpochToWrite) {
          writer.write(TimeUnit.MILLISECONDS.toNanos(millisSinceEpoch),
              millisSinceEpoch, 1);
        }
      } finally {
        writer.close();
      }
    }
  }
}
