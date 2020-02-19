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

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import hudson.PluginManager;
import hudson.console.ConsoleNote;
import hudson.model.AbstractBuild;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.TimestampNote;
import hudson.plugins.timestamper.io.LogFileReader.Line;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;
import jenkins.model.Jenkins;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.powermock.reflect.Whitebox;

/**
 * Unit test for {@link LogFileReader}.
 *
 * @author Steven G. Brown
 */
public class LogFileReaderTest {

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  private AbstractBuild<?, ?> build;

  private LogFileReader logFileReader;

  private Timestamp timestamp;

  private String logFileContents;

  private File uncompressedLogFile;

  private File gzippedLogFile;

  private File nonExistantFile;

  private Field INSECURE; // SECURITY-382

  @Before
  public void setUp() throws Exception {
    INSECURE = ConsoleNote.class.getDeclaredField("INSECURE");
    INSECURE.setAccessible(true);
    INSECURE.set(null, true);
    build = mock(AbstractBuild.class);
    when(build.getLogInputStream()).thenCallRealMethod();
    when(build.getLogReader()).thenCallRealMethod();
    Whitebox.setInternalState(build, "charset", Charsets.UTF_8.name());

    logFileReader = new LogFileReader(build);

    timestamp = new Timestamp(42, 1000);
    logFileContents =
        "line1\nline2"
            + new TimestampNote(timestamp.elapsedMillis, timestamp.millisSinceEpoch).encode()
            + "\n";

    // Uncompressed log file
    uncompressedLogFile = tempFolder.newFile();
    Files.write(logFileContents, uncompressedLogFile, Charsets.UTF_8);

    // Gzipped log file
    gzippedLogFile = tempFolder.newFile("logFile.gz");
    try (FileOutputStream fileOutputStream = new FileOutputStream(gzippedLogFile);
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream)) {
      gzipOutputStream.write(logFileContents.getBytes(Charset.defaultCharset()));
    }

    // Non-existant log file
    nonExistantFile = new File(tempFolder.getRoot(), "logFile");

    // Need to mock Jenkins to read the console notes.
    Jenkins jenkins = mock(Jenkins.class);
    Whitebox.setInternalState(jenkins, "pluginManager", mock(PluginManager.class));
    Whitebox.setInternalState(Jenkins.class, "theInstance", jenkins);
  }

  @After
  public void tearDown() throws Exception {
    Whitebox.setInternalState(Jenkins.class, "theInstance", (Jenkins) null);
    logFileReader.close();
    INSECURE.set(null, false);
  }

  @Test
  public void testNextLine_logFileExists() throws Exception {
    when(build.getLogFile()).thenReturn(uncompressedLogFile);
    testNextLine();
  }

  @Test
  public void testNextLine_zippedLogFile() throws Exception {
    when(build.getLogFile()).thenReturn(gzippedLogFile);
    testNextLine();
  }

  private void testNextLine() throws Exception {
    List<String> texts = new ArrayList<String>();
    List<Optional<Timestamp>> timestamps = new ArrayList<Optional<Timestamp>>();
    for (int i = 0; i < 3; i++) {
      Optional<Line> line = logFileReader.nextLine();
      if (!line.isPresent()) {
        break;
      }
      texts.add(line.get().getText());
      timestamps.add(line.get().readTimestamp());
    }

    List<String> expectedTexts = ImmutableList.of("line1", "line2");
    assertThat("texts", texts, is(expectedTexts));

    List<Optional<Timestamp>> expectedTimestamps =
        ImmutableList.of(Optional.empty(), Optional.of(timestamp));
    assertThat("timestamps", timestamps, is(expectedTimestamps));
  }

  @Test
  public void testNextLine_noLogFile() throws Exception {
    when(build.getLogFile()).thenReturn(nonExistantFile);
    assertThat(logFileReader.nextLine(), is(Optional.empty()));
  }

  @Test
  public void testReadTimestamp_logContainsEscapeCharacters() throws Exception {
    File logFile = tempFolder.newFile();
    when(build.getLogFile()).thenReturn(logFile);

    List<String> logFileContents =
        Arrays.asList(
            "\u001B[35m\u001B[1mScanning dependencies of target\u001B[0m",
            //
            "abc" + ConsoleNote.PREAMBLE_STR,
            "abc" + ConsoleNote.PREAMBLE_STR + "def",
            //
            "abc" + ConsoleNote.PREAMBLE_STR + encodeConsoleNote(2, ""),
            //
            "abc"
                + ConsoleNote.PREAMBLE_STR
                + encodeConsoleNote(2, "de")
                + ConsoleNote.POSTAMBLE_STR.substring(0, 2),
            //
            "abc"
                + ConsoleNote.PREAMBLE_STR
                + encodeConsoleNote(2, "de")
                + ConsoleNote.POSTAMBLE_STR);
    Files.write(Joiner.on('\n').join(logFileContents), logFile, Charsets.UTF_8);

    List<Optional<Timestamp>> timestamps = new ArrayList<Optional<Timestamp>>();
    for (int i = 0; i <= logFileContents.size(); i++) {
      Optional<Line> line = logFileReader.nextLine();
      if (!line.isPresent()) {
        break;
      }
      timestamps.add(line.get().readTimestamp());
    }

    List<Optional<Timestamp>> expectedTimestamps = new ArrayList<Optional<Timestamp>>();
    for (int i = 0; i < logFileContents.size(); i++) {
      expectedTimestamps.add(Optional.empty());
    }
    assertThat(timestamps, is(expectedTimestamps));
  }

  private String encodeConsoleNote(int size, String content) throws Exception {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    try (DataOutputStream dataOutputStream =
        new DataOutputStream(new Base64OutputStream(byteArrayOutputStream, true, -1, null))) {
      dataOutputStream.writeInt(size);
      dataOutputStream.writeBytes(content);
    }

    return byteArrayOutputStream.toString();
  }

  @Test
  public void testLineCount_logFileExists() throws Exception {
    when(build.getLogFile()).thenReturn(uncompressedLogFile);
    assertThat(logFileReader.lineCount(), is(2));
  }

  @Test
  public void testLineCount_zippedLogFile() throws Exception {
    when(build.getLogFile()).thenReturn(gzippedLogFile);
    assertThat(logFileReader.lineCount(), is(2));
  }

  @Test
  public void testLineCount_noLogFile() throws Exception {
    when(build.getLogFile()).thenReturn(nonExistantFile);
    assertThat(logFileReader.lineCount(), is(0));
  }
}
