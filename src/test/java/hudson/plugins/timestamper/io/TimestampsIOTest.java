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
package hudson.plugins.timestamper.io;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.base.Optional;

/**
 * Test for integration between the {@link TimestampsFileReader} and
 * {@link TimestampsWriter} classes.
 * 
 * @author Steven G. Brown
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Run.class)
public class TimestampsIOTest {

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
    build = PowerMockito.mock(Run.class);
    when(build.getRootDir()).thenReturn(folder.getRoot());
    when(build.getTimeInMillis()).thenReturn(1l);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testReadFromStartWhileWriting() throws Exception {
    TimestampsWriter writer = new TimestampsWriter(build);
    TimestampsFileReader reader = new TimestampsFileReader(build);
    try {
      writer.write(2, 1);
      assertThat(reader.read(), is(Optional.of(new Timestamp(1, 2))));
      writer.write(3, 1);
      assertThat(reader.read(), is(Optional.of(new Timestamp(2, 3))));
      writer.write(4, 2);
      assertThat(reader.read(), is(Optional.of(new Timestamp(3, 4))));
      assertThat(reader.read(), is(Optional.of(new Timestamp(3, 4))));
      writer.write(5, 1);
      assertThat(reader.read(), is(Optional.of(new Timestamp(4, 5))));
    } finally {
      if (reader != null) {
        reader.close();
      }
      writer.close();
    }
  }
}
