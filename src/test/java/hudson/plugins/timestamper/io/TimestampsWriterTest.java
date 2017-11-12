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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.powermock.reflect.Whitebox;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.CountingInputStream;
import com.google.common.io.Files;

import hudson.model.Run;

/**
 * Unit test for the {@link TimestampsWriter} class.
 *
 * @author Steven G. Brown
 */
public class TimestampsWriterTest {

  /** */
  @Rule public TemporaryFolder folder = new TemporaryFolder();

  /** */
  @Rule public ExpectedException thrown = ExpectedException.none();

  private Run<?, ?> build;

  private File timestampsFile;

  private File timestampsHashFile;

  private TimestampsWriter timestampsWriter;

  /** @throws Exception */
  @Before
  public void setUp() throws Exception {
    build = mock(Run.class);
    when(build.getRootDir()).thenReturn(folder.getRoot());
    timestampsFile = TimestamperPaths.timestampsFile(build);
    timestampsHashFile = new File(timestampsFile.getParent(), timestampsFile.getName() + ".SHA-1");
  }

  /** @throws Exception */
  @After
  public void tearDown() throws Exception {
    if (timestampsWriter != null) {
      timestampsWriter.close();
    }
  }

  /** @throws Exception */
  @Test
  public void testWriteIncreasing() throws Exception {
    timestampsWriter = new TimestampsWriter(build);
    timestampsWriter.write(1, 1);
    timestampsWriter.write(2, 1);
    timestampsWriter.write(3, 1);
    assertThat(writtenTimestampData(), is(Arrays.asList(1, 1, 1)));
  }

  /** @throws Exception */
  @Test
  public void testWriteDecreasing() throws Exception {
    timestampsWriter = new TimestampsWriter(build);
    timestampsWriter.write(3, 1);
    timestampsWriter.write(2, 1);
    timestampsWriter.write(1, 1);
    assertThat(writtenTimestampData(), is(Arrays.asList(3, -1, -1)));
  }

  /** @throws Exception */
  @Test
  public void testWriteZeroTimes() throws Exception {
    timestampsWriter = new TimestampsWriter(build);
    timestampsWriter.write(0, 0);
    assertThat(writtenTimestampData(), is(Collections.<Integer>emptyList()));
  }

  /** @throws Exception */
  @Test
  public void testWriteSeveralTimes() throws Exception {
    timestampsWriter = new TimestampsWriter(build);
    timestampsWriter.write(1, 1);
    timestampsWriter.write(6, 4);
    timestampsWriter.write(7, 1);
    assertThat(writtenTimestampData(), is(Arrays.asList(1, 5, 0, 0, 0, 1)));
  }

  /** @throws Exception */
  @Test
  public void testWriteSameTimestampManyTimes() throws Exception {
    int bufferSize = Whitebox.getField(TimestampsWriter.class, "BUFFER_SIZE").getInt(null);
    int times = bufferSize + 1000; // larger than the buffer
    timestampsWriter = new TimestampsWriter(build);
    timestampsWriter.write(10000, times);
    List<Integer> writtenTimestampData = writtenTimestampData();
    assertThat(writtenTimestampData.get(0), is(10000));
    assertThat(writtenTimestampData.subList(1, writtenTimestampData.size()), everyItem(equalTo(0)));
    assertThat(writtenTimestampData, hasSize(times));
  }

  /** @throws Exception */
  @Test
  public void testHashFile() throws Exception {
    MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
    timestampsWriter = new TimestampsWriter(build, Optional.of(sha1));
    timestampsWriter.write(1, 1);
    timestampsWriter.write(2, 1);
    timestampsWriter.write(3, 1);
    timestampsWriter.writeDigest();
    timestampsWriter.close();

    byte[] fileContents = Files.toByteArray(timestampsFile);
    byte[] expectedHash = MessageDigest.getInstance("SHA-1").digest(fileContents);
    assertThat(
        Files.toString(timestampsHashFile, Charsets.US_ASCII).trim(),
        is(DatatypeConverter.printHexBinary(expectedHash).toLowerCase()));
  }

  /** @throws Exception */
  @Test
  public void testNoHashFile() throws Exception {
    timestampsWriter = new TimestampsWriter(build);
    timestampsWriter.write(1, 1);
    timestampsWriter.write(2, 1);
    timestampsWriter.write(3, 1);
    timestampsWriter.writeDigest();
    timestampsWriter.close();
    assertThat(timestampsHashFile.getParentFile().listFiles(), is(new File[] {timestampsFile}));
  }

  /** @throws Exception */
  @Test
  public void testOnlyOneWriterPerBuild() throws Exception {
    timestampsWriter = new TimestampsWriter(build);
    thrown.expect(IOException.class);
    timestampsWriter = new TimestampsWriter(build);
  }

  private List<Integer> writtenTimestampData() throws Exception {
    byte[] fileContents = Files.toByteArray(timestampsFile);
    CountingInputStream inputStream =
        new CountingInputStream(new ByteArrayInputStream(fileContents));
    List<Integer> timestampData = new ArrayList<Integer>();
    while (inputStream.getCount() < fileContents.length) {
      timestampData.add((int) Varint.read(inputStream));
    }
    return timestampData;
  }
}
