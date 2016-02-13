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
package hudson.plugins.timestamper.api;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.model.Run;
import hudson.plugins.timestamper.action.TimestampsActionOutput;
import hudson.plugins.timestamper.io.TimestampNotesReader;
import hudson.plugins.timestamper.io.TimestamperPaths;
import hudson.plugins.timestamper.io.TimestampsFileReader;
import hudson.plugins.timestamper.io.TimestampsReader;

import java.io.BufferedReader;
import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.base.Optional;
import com.google.common.io.Files;

/**
 * Unit test for the {@link TimestamperAPI} class.
 * 
 * @author Steven G. Brown
 */
@RunWith(MockitoJUnitRunner.class)
public class TimestamperAPITest {

  /**
   */
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  @Mock
  private Run<?, ?> build;

  @Mock
  private TimestampsActionOutput output;

  private TimestamperAPI api;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    when(build.getRootDir()).thenReturn(folder.getRoot());

    when(output.nextLine(any(TimestampsReader.class)))
        .thenReturn(Optional.of("line1")).thenReturn(Optional.of("line2"))
        .thenReturn(Optional.<String> absent());

    api = new TimestamperAPI(output);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRead_timestampsFileExists() throws Exception {
    File timestampsFile = TimestamperPaths.timestampsFile(build);
    timestampsFile.getParentFile().mkdirs();
    Files.touch(timestampsFile);

    assertThat(read(), is("line1\nline2\n"));

    verify(output).setQuery("query");
    verify(output, atLeast(1)).nextLine(isA(TimestampsFileReader.class));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testRead_timestampsFileDoesNotExist() throws Exception {
    assertThat(read(), is("line1\nline2\n"));

    verify(output).setQuery("query");
    verify(output, atLeast(1)).nextLine(isA(TimestampNotesReader.class));
  }

  private String read() throws Exception {
    BufferedReader reader = api.read(build, "query");
    StringBuilder result = new StringBuilder();
    while (true) {
      String line = reader.readLine();
      if (line == null) {
        return result.toString();
      }
      result.append(line);
      result.append("\n");
    }
  }
}
