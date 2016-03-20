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
package hudson.plugins.timestamper.io;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.PluginManager;
import hudson.console.ConsoleNote;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.TimestampNote;
import hudson.tasks._ant.AntTargetNote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jenkins.model.Jenkins;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.reflect.Whitebox;

import com.google.common.base.Optional;

/**
 * Unit test for the {@link TimestampNotesReader} class.
 * 
 * @author Steven G. Brown
 */
public class TimestampNotesReaderTest {

  private Run<?, ?> build;

  private byte[] consoleLog = new byte[] {};

  private TimestampNotesReader timestampNotesReader;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    build = mock(Run.class);
    when(build.getLogInputStream()).thenAnswer(new Answer<InputStream>() {

      @Override
      public InputStream answer(InvocationOnMock invocation) throws Throwable {
        return new ByteArrayInputStream(consoleLog);
      }
    });

    timestampNotesReader = new TimestampNotesReader(build);

    // Need to mock Jenkins to read the console notes.
    Jenkins jenkins = mock(Jenkins.class);
    Whitebox.setInternalState(jenkins, "pluginManager",
        mock(PluginManager.class));
    Whitebox.setInternalState(Jenkins.class, "theInstance", jenkins);
  }

  /**
   */
  @After
  public void tearDown() {
    timestampNotesReader.close();
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRead() throws Exception {
    writeLine(new TimestampNote(1, 1));
    writeLine(new TimestampNote(2, 2));
    assertThat(readTimestamps(),
        is(Arrays.asList(new Timestamp(1, 1), new Timestamp(2, 2))));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRead_withOtherConsoleNotes() throws Exception {
    writeLine(new TimestampNote(1, 1), new AntTargetNote());
    assertThat(readTimestamps(),
        is(Collections.singletonList(new Timestamp(1, 1))));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRead_emptyLogFile() throws Exception {
    assertThat(readTimestamps(), is(Collections.<Timestamp> emptyList()));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRead_noTimestampNotes() throws Exception {
    writeLine(new AntTargetNote());
    assertThat(readTimestamps(), is(Collections.<Timestamp> emptyList()));
  }

  private void writeLine(ConsoleNote<?>... consoleNotes) throws Exception {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    outputStream.write(consoleLog);
    outputStream.write('\n');
    for (ConsoleNote<?> consoleNote : consoleNotes) {
      consoleNote.encodeTo(outputStream);
    }
    outputStream.write('a'); // rest of line
    consoleLog = outputStream.toByteArray();
  }

  private List<Timestamp> readTimestamps() throws Exception {
    List<Timestamp> timestamps = new ArrayList<Timestamp>();
    int iterations = 0;
    while (true) {
      Optional<Timestamp> next = timestampNotesReader.read();
      if (!next.isPresent()) {
        return timestamps;
      }
      timestamps.add(next.get());
      iterations++;
      if (iterations > 10000) {
        throw new IllegalStateException(
            "time-stamps do not appear to terminate. read so far: "
                + timestamps);
      }
    }
  }
}
