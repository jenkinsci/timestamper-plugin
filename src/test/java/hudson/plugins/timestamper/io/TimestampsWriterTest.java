/*
 * The MIT License
 *
 * Copyright (c) 2013 Steven G. Brown
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.io.CountingInputStream;
import hudson.model.Run;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit test for the {@link TimestampsWriter} class.
 *
 * @author Steven G. Brown
 */
public class TimestampsWriterTest {

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private Run<?, ?> build;

  private Path timestampsFile;

  private Path timestampsHashFile;

  private TimestampsWriter timestampsWriter;

  @Before
  public void setUp() {
    build = mock(Run.class);
    when(build.getRootDir()).thenReturn(folder.getRoot());
    timestampsFile = TimestamperPaths.timestampsFile(build);
    timestampsHashFile = timestampsFile.resolveSibling(timestampsFile.getFileName() + ".SHA-1");
  }

  @After
  public void tearDown() throws IOException {
    if (timestampsWriter != null) {
      timestampsWriter.close();
    }
  }

  @Test
  public void testWriteIncreasing() throws Exception {
    timestampsWriter = new TimestampsWriter(build);
    timestampsWriter.write(1, 1);
    timestampsWriter.write(2, 1);
    timestampsWriter.write(3, 1);
    assertThat(writtenTimestampData(), is(Arrays.asList(1, 1, 1)));
  }

  @Test
  public void testWriteDecreasing() throws Exception {
    timestampsWriter = new TimestampsWriter(build);
    timestampsWriter.write(3, 1);
    timestampsWriter.write(2, 1);
    timestampsWriter.write(1, 1);
    assertThat(writtenTimestampData(), is(Arrays.asList(3, -1, -1)));
  }

  @Test
  public void testWriteZeroTimes() throws Exception {
    timestampsWriter = new TimestampsWriter(build);
    timestampsWriter.write(0, 0);
    assertThat(writtenTimestampData(), is(Collections.<Integer>emptyList()));
  }

  @Test
  public void testWriteSeveralTimes() throws Exception {
    timestampsWriter = new TimestampsWriter(build);
    timestampsWriter.write(1, 1);
    timestampsWriter.write(6, 4);
    timestampsWriter.write(7, 1);
    assertThat(writtenTimestampData(), is(Arrays.asList(1, 5, 0, 0, 0, 1)));
  }

  @Test
  public void testWriteSameTimestampManyTimes() throws Exception {
    int bufferSize = 1024; // cf. hudson.plugins.timestamper.io.TimestampsWriter.BUFFER_SIZE
    int times = bufferSize + 1000; // larger than the buffer
    timestampsWriter = new TimestampsWriter(build);
    timestampsWriter.write(10000, times);
    List<Integer> writtenTimestampData = writtenTimestampData();
    assertThat(writtenTimestampData.get(0), is(10000));
    assertThat(writtenTimestampData.subList(1, writtenTimestampData.size()), everyItem(equalTo(0)));
    assertThat(writtenTimestampData, hasSize(times));
  }

  @Test
  public void testHashFile() throws Exception {
    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
    timestampsWriter = new TimestampsWriter(build, Optional.of(sha1));
    timestampsWriter.write(1, 1);
    timestampsWriter.write(2, 1);
    timestampsWriter.write(3, 1);
    timestampsWriter.writeDigest();
    timestampsWriter.close();

    byte[] fileContents = Files.readAllBytes(timestampsFile);
    byte[] expectedHash = MessageDigest.getInstance("SHA-1").digest(fileContents);
    List<String> hashLines = Files.readAllLines(timestampsHashFile, StandardCharsets.US_ASCII);
    assertThat(hashLines, hasSize(1));
    assertThat(hashLines.get(0).trim(), is(bytesToHex(expectedHash).toLowerCase()));
  }

  @Test
  public void testNoHashFile() throws Exception {
    timestampsWriter = new TimestampsWriter(build);
    timestampsWriter.write(1, 1);
    timestampsWriter.write(2, 1);
    timestampsWriter.write(3, 1);
    timestampsWriter.writeDigest();
    timestampsWriter.close();
    assertThat(
        Files.list(Objects.requireNonNull(timestampsHashFile.getParent()))
            .collect(Collectors.toList()),
        is(Collections.singletonList(timestampsFile)));
  }

  @Test
  public void testOnlyOneWriterPerBuild() throws Exception {
    timestampsWriter = new TimestampsWriter(build);
    assertThrows(IOException.class, () -> timestampsWriter = new TimestampsWriter(build));
  }

  private List<Integer> writtenTimestampData() throws Exception {
    byte[] fileContents = Files.readAllBytes(timestampsFile);
    CountingInputStream inputStream =
        new CountingInputStream(new ByteArrayInputStream(fileContents));
    List<Integer> timestampData = new ArrayList<>();
    while (inputStream.getCount() < fileContents.length) {
      timestampData.add((int) Varint.read(inputStream));
    }
    return timestampData;
  }

  private String bytesToHex(byte[] bytes) {
    final char[] hexArray = "0123456789ABCDEF".toCharArray();

    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = hexArray[v >>> 4];
      hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
  }
}
