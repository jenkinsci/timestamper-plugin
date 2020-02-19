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
package hudson.plugins.timestamper;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.AbstractBuild;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test for the {@link TimestamperBuildWrapper} class.
 *
 * @author Steven G. Brown
 */
public class TimestamperBuildWrapperTest {

  /** */
  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private TimestamperBuildWrapper buildWrapper;

  private AbstractBuild<?, ?> build;

  private ByteArrayOutputStream outputStream;

  /** */
  @Before
  public void setUp() {
    System.clearProperty(TimestampNote.getSystemProperty());
    buildWrapper = new TimestamperBuildWrapper();
    build = mock(AbstractBuild.class);
    when(build.getRootDir()).thenReturn(folder.getRoot());
    outputStream = new ByteArrayOutputStream();
  }

  /** */
  @After
  public void tearDown() {
    System.clearProperty(TimestampNote.getSystemProperty());
  }

  /** @throws Exception */
  @Test
  public void testDecorate() throws Exception {
    OutputStream decoratedOutputStream = buildWrapper.decorateLogger(build, outputStream);
    assertThat(decoratedOutputStream, instanceOf(TimestamperOutputStream.class));
  }

  /** @throws Exception */
  @Test
  public void testDecorateWithTimestampNoteSystemProperty() throws Exception {
    System.setProperty(TimestampNote.getSystemProperty(), "true");
    OutputStream decoratedOutputStream = buildWrapper.decorateLogger(build, outputStream);
    assertThat(decoratedOutputStream, instanceOf(TimestampNotesOutputStream.class));
  }
}
