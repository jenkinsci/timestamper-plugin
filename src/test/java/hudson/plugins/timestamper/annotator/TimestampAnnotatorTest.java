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
package hudson.plugins.timestamper.annotator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import hudson.MarkupText;
import hudson.console.ConsoleAnnotator;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.format.TimestampFormat;
import hudson.plugins.timestamper.format.TimestampFormatProvider;
import hudson.plugins.timestamper.io.TimestampsWriter;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.stubbing.Answer;

/**
 * Unit test for the {@link TimestampAnnotator} class.
 *
 * @author Steven G. Brown
 */
class TimestampAnnotatorTest {

    @TempDir
    private File folder;

    private Run<?, ?> build;

    private static ConsoleLogParser.Result logPosition;

    private static List<Timestamp> capturedTimestamps;

    private TimestampsWriter writer;

    @BeforeEach
    void setUp() throws IOException {
        build = mock(Run.class);
        when(build.getRootDir()).thenReturn(folder);

        logPosition = new ConsoleLogParser.Result();
        capturedTimestamps = new ArrayList<>();

        writer = new TimestampsWriter(build);
    }

    @AfterEach
    void tearDown() throws IOException {
        writer.close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testStartOfLogFile(boolean serialize) throws Exception {
        List<Timestamp> timestamps = writeTimestamps(2);
        logPosition.lineNumber = 0;
        logPosition.atNewLine = true;
        assertThat(annotate(serialize), is(timestamps));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testStartOfLogFile_negativeLineNumber(boolean serialize) throws Exception {
        List<Timestamp> timestamps = writeTimestamps(2);
        logPosition.lineNumber = -2;
        logPosition.atNewLine = true;
        assertThat(annotate(serialize), is(timestamps));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testWithinFirstLine(boolean serialize) throws Exception {
        List<Timestamp> timestamps = writeTimestamps(2);
        logPosition.lineNumber = 0;
        logPosition.atNewLine = false;
        assertThat(annotate(serialize), is(timestamps.subList(1, 2)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testWithinFirstLine_negativeLineNumber(boolean serialize) throws Exception {
        List<Timestamp> timestamps = writeTimestamps(2);
        logPosition.lineNumber = -2;
        logPosition.atNewLine = false;
        assertThat(annotate(serialize), is(timestamps.subList(1, 2)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testNextLine(boolean serialize) throws Exception {
        List<Timestamp> timestamps = writeTimestamps(2);
        logPosition.lineNumber = 1;
        logPosition.atNewLine = true;
        assertThat(annotate(serialize), is(timestamps.subList(1, 2)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testNextLine_negativeLineNumber(boolean serialize) throws Exception {
        List<Timestamp> timestamps = writeTimestamps(2);
        logPosition.lineNumber = -1;
        logPosition.atNewLine = true;
        assertThat(annotate(serialize), is(timestamps.subList(1, 2)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testEndOfLogFile(boolean serialize) {
        logPosition.endOfFile = true;
        assertThat(annotate(serialize), is(Collections.<Timestamp>emptyList()));
    }

    private List<Timestamp> writeTimestamps(int count) throws IOException {
        List<Timestamp> timestamps = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            writer.write(i, 1);
            timestamps.add(new Timestamp(i, i));
        }
        return timestamps;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Timestamp> annotate(boolean serialize) {
        ConsoleLogParser logParser = new MockConsoleLogParser();
        ConsoleAnnotator annotator = new TimestampAnnotator(logParser);

        try (MockedStatic<TimestampFormatProvider> mocked = mockStatic(TimestampFormatProvider.class)) {
            captureFormattedTimestamps(mocked);
            int iterations = 0;
            while (annotator != null) {
                if (serialize) {
                    annotator = SerializationUtils.clone(annotator);
                }
                annotator = annotator.annotate(build, mock(MarkupText.class));
                iterations++;
                if (iterations > 100) {
                    throw new AssertionError("annotator is not terminating");
                }
            }
        }
        return capturedTimestamps;
    }

    private static void captureFormattedTimestamps(MockedStatic<TimestampFormatProvider> mocked) {
        final TimestampFormat format = mock(TimestampFormat.class);
        doAnswer((Answer<Void>) invocation -> {
                    Timestamp timestamp = (Timestamp) invocation.getArguments()[1];
                    capturedTimestamps.add(timestamp);
                    return null;
                })
                .when(format)
                .markup(any(MarkupText.class), any(Timestamp.class));
        mocked.when(TimestampFormatProvider::get).thenReturn(format);
    }

    private static class MockConsoleLogParser extends ConsoleLogParser {

        @Serial
        private static final long serialVersionUID = 1L;

        MockConsoleLogParser() {
            super(0);
        }

        @Override
        public Result seek(Run<?, ?> build) {
            return logPosition;
        }
    }
}
