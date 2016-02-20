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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.model.Run;
import hudson.plugins.timestamper.io.LogFileReader;
import hudson.plugins.timestamper.io.TimestampNotesReader;
import hudson.plugins.timestamper.io.TimestamperPaths;
import hudson.plugins.timestamper.io.TimestampsFileReader;
import hudson.plugins.timestamper.io.TimestampsReader;

import java.io.File;
import java.io.PrintWriter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;
import com.google.common.io.Files;

/**
 * Unit test for the {@link TimestampsAction} class.
 * 
 * @author Steven G. Brown
 */
@RunWith(MockitoJUnitRunner.class)
public class TimestampsActionTest {

  /**
   */
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Mock
  private Run<?, ?> build;

  @Mock
  private StaplerRequest request;

  @Mock
  private PrintWriter writer;

  @Mock
  private StaplerResponse response;

  @Mock
  private TimestampsActionOutput output;

  private TimestampsAction action;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    when(build.getRootDir()).thenReturn(folder.getRoot());
    when(response.getWriter()).thenReturn(writer);
    when(request.getQueryString()).thenReturn("query");

    when(output.nextLine(any(TimestampsReader.class), any(LogFileReader.class)))
        .thenReturn(Optional.of("line")).thenReturn(Optional.<String> absent());

    action = new TimestampsAction(build, output);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testDoIndex_timestampsFileExists() throws Exception {
    File timestampsFile = TimestamperPaths.timestampsFile(build);
    timestampsFile.getParentFile().mkdirs();
    Files.touch(timestampsFile);

    action.doIndex(request, response);

    verify(output).setQuery("query");
    verify(output, times(2)).nextLine(isA(TimestampsFileReader.class),
        isA(LogFileReader.class));
    verify(writer).println("line");
  }

  /**
   * @throws Exception
   */
  @Test
  public void testDoIndex_timestampsFileDoesNotExist() throws Exception {
    action.doIndex(request, response);

    verify(output).setQuery("query");
    verify(output, times(2)).nextLine(isA(TimestampNotesReader.class),
        isA(LogFileReader.class));
    verify(writer).println("line");
  }
}
