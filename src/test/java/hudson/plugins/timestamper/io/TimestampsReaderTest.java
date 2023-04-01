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
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Unit test for the {@link TimestampsReader} class.
 *
 * @author Steven G. Brown
 */
@RunWith(Parameterized.class)
public class TimestampsReaderTest {

    /** @return parameterised test data */
    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[] {false}, new Object[] {true});
    }

    @Parameter(0)
    public boolean serialize;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Run<?, ?> build;

    private TimestampsReader timestampsReader;

    @Before
    public void setUp() {
        build = mock(Run.class);
        when(build.getRootDir()).thenReturn(folder.getRoot());
        timestampsReader = new TimestampsReader(build);
    }

    @After
    public void tearDown() {
        timestampsReader.close();
    }

    @Test
    public void testNoTimestampsToRead() throws Exception {
        assertThat(readTimestamps(), is(Collections.<Timestamp>emptyList()));
    }

    @Test
    public void testReadFromStart() throws Exception {
        writeTimestamps(Arrays.asList(1, 1, 1, 1));
        assertThat(readTimestamps(), is(Arrays.asList(t(1, 1), t(2, 2), t(3, 3), t(4, 4))));
    }

    @Test
    public void testSkipZero() throws Exception {
        writeTimestamps(Arrays.asList(1, 1, 1, 1));
        timestampsReader.skip(0);
        assertThat(readTimestamps(), is(Arrays.asList(t(1, 1), t(2, 2), t(3, 3), t(4, 4))));
    }

    @Test
    public void testSkipOne() throws Exception {
        writeTimestamps(Arrays.asList(1, 1, 1, 1));
        timestampsReader.skip(1);
        assertThat(readTimestamps(), is(Arrays.asList(t(2, 2), t(3, 3), t(4, 4))));
    }

    @Test
    public void testSkipTwo() throws Exception {
        writeTimestamps(Arrays.asList(1, 1, 1, 1));
        timestampsReader.skip(2);
        assertThat(readTimestamps(), is(Arrays.asList(t(3, 3), t(4, 4))));
    }

    @Test
    public void testSkipToEnd() throws Exception {
        writeTimestamps(Arrays.asList(1, 1, 1, 1));
        timestampsReader.skip(4);
        assertThat(readTimestamps(), is(Collections.<Timestamp>emptyList()));
    }

    @Test
    public void testSkipPastEnd() throws Exception {
        writeTimestamps(Arrays.asList(1, 1, 1, 1));
        timestampsReader.skip(5);
        assertThat(readTimestamps(), is(Collections.<Timestamp>emptyList()));
    }

    /**
     * Test that the time shifts file is read correctly. The time shifts file was previously generated
     * by this plug-in to record changes to the clock, i.e. when {@link System#currentTimeMillis()}
     * diverges from {@link System#nanoTime()}. Newer versions of this plug-in no longer create time
     * shifts files due to JENKINS-19778.
     */
    @Test
    public void testTimeShifts() throws Exception {
        writeTimestamps(Arrays.asList(1, 1, 1, 1, 20));
        writeTimeShifts(Arrays.asList(0, 10, 2, -10, 3, -10));
        assertThat(readTimestamps(), is(Arrays.asList(t(1, 10), t(2, 11), t(3, -10), t(4, -10), t(24, 10))));
    }

    private void writeTimestamps(List<Integer> timestampData) throws Exception {
        Path timestampsFile = TimestamperPaths.timestampsFile(build);
        writeToFile(timestampData, timestampsFile);
    }

    private void writeTimeShifts(List<Integer> timeShiftData) throws Exception {
        Path timeShiftsFile = TimestamperPaths.timeShiftsFile(build);
        writeToFile(timeShiftData, timeShiftsFile);
    }

    private void writeToFile(List<Integer> data, Path file) throws Exception {
        Files.createDirectories(Objects.requireNonNull(file.getParent()));
        try (OutputStream outputStream =
                Files.newOutputStream(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND)) {
            byte[] buffer = new byte[10];
            for (Integer value : data) {
                int len = Varint.write(value, buffer, 0);
                outputStream.write(buffer, 0, len);
            }
        }
    }

    private List<Timestamp> readTimestamps() throws IOException {
        List<Timestamp> timestamps = new ArrayList<>();
        int iterations = 0;
        while (true) {
            if (serialize) {
                timestampsReader = SerializationUtils.clone(timestampsReader);
            }
            Optional<Timestamp> next = timestampsReader.read();
            if (!next.isPresent()) {
                return timestamps;
            }
            timestamps.add(next.get());
            iterations++;
            if (iterations > 10000) {
                throw new IllegalStateException("time-stamps do not appear to terminate. read so far: " + timestamps);
            }
        }
    }

    private Timestamp t(long elapsedMillis, long millisSinceEpoch) {
        return new Timestamp(elapsedMillis, millisSinceEpoch);
    }
}
