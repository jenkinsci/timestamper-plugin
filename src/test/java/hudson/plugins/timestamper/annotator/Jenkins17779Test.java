package hudson.plugins.timestamper.annotator;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import hudson.MarkupText;
import hudson.model.Run;
import hudson.plugins.timestamper.TimestamperTestAssistant;
import hudson.plugins.timestamper.format.TimestampFormatter;
import hudson.plugins.timestamper.format.TimestampFormatterImpl;
import hudson.plugins.timestamper.io.TimestampsWriter;
import hudson.plugins.timestamper.io.TimestampsWriterImpl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jvnet.hudson.test.Bug;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.io.Files;

/**
 * @author Kohsuke Kawaguchi
 */
public class Jenkins17779Test {

  /**
   */
  @Rule
  public TemporaryFolder folder = new TemporaryFolder();

  private Run<?, ?> build;

  /**
   * Creates a dummy timestamp record that says each line took 1ms to render.
   * 
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    final byte[] consoleLogContents = "AB\n\n\n\nCD\n".getBytes("US-ASCII");
    File consoleLog = folder.newFile();
    Files.write(consoleLogContents, consoleLog);

    build = mock(Run.class);
    when(build.getRootDir()).thenReturn(folder.getRoot());
    when(build.getLogInputStream()).thenAnswer(new Answer<InputStream>() {
      @Override
      public InputStream answer(InvocationOnMock invocation) throws Throwable {
        return new ByteArrayInputStream(consoleLogContents);
      }
    });
    when(build.getLogFile()).thenReturn(consoleLog);

    TimestampsWriter writer = new TimestampsWriterImpl(build);
    try {
      for (int i = 0; i < 10; i++) {
        writer.write(TimeUnit.MILLISECONDS.toNanos(i), i, 1);
      }
    } finally {
      writer.close();
    }
  }

  /**
   * Regression test for JENKINS-17779.
   * 
   * @throws Exception
   */
  @Test
  @Bug(17779)
  public void fastForwardShouldHandleDoubleEmptyLines() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    TimestampFormatter f = new TimestampFormatterImpl("S", "", request);

    // fast-forward to 'CD' which is line 5
    TimestampAnnotator a = new TimestampAnnotator(f, 6);
    MarkupText text = new MarkupText("CD");
    a.annotate(build, text);
    assertThat(text.toString(true), is(TimestamperTestAssistant.span("4")
        + "CD"));

    // should get the same result if we go line by line
    a = new TimestampAnnotator(f, 0);
    for (int i = 0; i < 5; i++) {
      text = new MarkupText("CD");
      a.annotate(build, text);
    }
    assertThat(text.toString(true), is(TimestamperTestAssistant.span("4")
        + "CD"));
  }
}
