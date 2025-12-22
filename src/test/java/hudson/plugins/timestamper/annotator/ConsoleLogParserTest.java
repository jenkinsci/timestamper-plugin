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
package hudson.plugins.timestamper.annotator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.console.AnnotatedLargeText;
import hudson.model.Run;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit test for the {@link ConsoleLogParser} class.
 *
 * @author Steven G. Brown
 */
class ConsoleLogParserTest {

    private static final char NEWLINE = 0x0A;

    @TempDir
    private File folder;

    private Run<?, ?> build;

    private int logLength;

    /** @return parameterised test data */
    static Stream<Object> data() {
        return Stream.of(
                new Object[] {false, false}, new Object[] {false, true}, new Object[] {true, false}, new Object[] {
                    true, true
                });
    }

    @BeforeEach
    void setUp() throws Exception {
        build = mock(Run.class);
        when(build.getRootDir()).thenReturn(folder);
        byte[] consoleLog = new byte[] {0x61, NEWLINE, NEWLINE, NEWLINE, NEWLINE, 0x61, NEWLINE};
        logLength = consoleLog.length;
        when(build.getLogInputStream()).thenReturn(new ByteArrayInputStream(consoleLog));
        AnnotatedLargeText<?> logText = mock(AnnotatedLargeText.class);
        when(logText.length()).thenReturn((long) logLength);
        when(build.getLogText()).thenReturn(logText);
    }

    @ParameterizedTest(name = "serialize={0},isBuilding={1}")
    @MethodSource("data")
    void testSeekStart(boolean serialize, boolean isBuilding) throws Exception {
        when(build.isBuilding()).thenReturn(isBuilding);

        ConsoleLogParser.Result result = new ConsoleLogParser.Result();
        result.lineNumber = 0;
        result.atNewLine = true;
        assertThat(seek(serialize, 0), is(result));
    }

    @ParameterizedTest(name = "serialize={0},isBuilding={1}")
    @MethodSource("data")
    void testSeekWithinLine(boolean serialize, boolean isBuilding) throws Exception {
        when(build.isBuilding()).thenReturn(isBuilding);

        ConsoleLogParser.Result result = new ConsoleLogParser.Result();
        result.lineNumber = 0;
        assertThat(seek(serialize, 1), is(result));
    }

    @ParameterizedTest(name = "serialize={0},isBuilding={1}")
    @MethodSource("data")
    void testSeekNextLine(boolean serialize, boolean isBuilding) throws Exception {
        when(build.isBuilding()).thenReturn(isBuilding);

        ConsoleLogParser.Result result = new ConsoleLogParser.Result();
        result.lineNumber = 1;
        result.atNewLine = true;
        assertThat(seek(serialize, 2), is(result));
    }

    @ParameterizedTest(name = "serialize={0},isBuilding={1}")
    @MethodSource("data")
    void testSeekEnd(boolean serialize, boolean isBuilding) throws Exception {
        when(build.isBuilding()).thenReturn(isBuilding);

        ConsoleLogParser.Result result = new ConsoleLogParser.Result();
        result.lineNumber = 5;
        result.atNewLine = true;
        assertThat(seek(serialize, logLength), is(result));
    }

    @ParameterizedTest(name = "serialize={0},isBuilding={1}")
    @MethodSource("data")
    void testSeekPastEnd(boolean serialize, boolean isBuilding) throws Exception {
        when(build.isBuilding()).thenReturn(isBuilding);

        ConsoleLogParser.Result result = new ConsoleLogParser.Result();
        result.lineNumber = 5;
        result.atNewLine = true;
        result.endOfFile = true;
        assertThat(seek(serialize, logLength + 1), is(result));
    }

    @ParameterizedTest(name = "serialize={0},isBuilding={1}")
    @MethodSource("data")
    void testSeekStartNegative(boolean serialize, boolean isBuilding) throws Exception {
        when(build.isBuilding()).thenReturn(isBuilding);

        ConsoleLogParser.Result result = new ConsoleLogParser.Result();
        result.lineNumber = 0;
        result.atNewLine = true;
        assertThat(seek(serialize, -logLength), is(result));
    }

    @ParameterizedTest(name = "serialize={0},isBuilding={1}")
    @MethodSource("data")
    void testSeekWithinLineNegative_isBuilding(boolean serialize, boolean isBuilding) throws Exception {
        assumeTrue(isBuilding);
        when(build.isBuilding()).thenReturn(isBuilding);

        ConsoleLogParser.Result result = new ConsoleLogParser.Result();
        result.lineNumber = 0;
        assertThat(seek(serialize, 1 - logLength), is(result));
    }

    @ParameterizedTest(name = "serialize={0},isBuilding={1}")
    @MethodSource("data")
    void testSeekWithinLineNegative_notBuilding(boolean serialize, boolean isBuilding) throws Exception {
        assumeFalse(isBuilding);
        when(build.isBuilding()).thenReturn(isBuilding);

        ConsoleLogParser.Result result = new ConsoleLogParser.Result();
        result.lineNumber = -4;
        assertThat(seek(serialize, 1 - logLength), is(result));
    }

    @ParameterizedTest(name = "serialize={0},isBuilding={1}")
    @MethodSource("data")
    void testSeekNextLineNegative_isBuilding(boolean serialize, boolean isBuilding) throws Exception {
        assumeTrue(isBuilding);
        when(build.isBuilding()).thenReturn(isBuilding);

        ConsoleLogParser.Result result = new ConsoleLogParser.Result();
        result.lineNumber = 1;
        result.atNewLine = true;
        assertThat(seek(serialize, 2 - logLength), is(result));
    }

    @ParameterizedTest(name = "serialize={0},isBuilding={1}")
    @MethodSource("data")
    void testSeekNextLineNegative_notBuilding(boolean serialize, boolean isBuilding) throws Exception {
        assumeFalse(isBuilding);
        when(build.isBuilding()).thenReturn(isBuilding);

        ConsoleLogParser.Result result = new ConsoleLogParser.Result();
        result.lineNumber = -3;
        result.atNewLine = true;
        assertThat(seek(serialize, 2 - logLength), is(result));
    }

    @ParameterizedTest(name = "serialize={0},isBuilding={1}")
    @MethodSource("data")
    void testSeekPastStartNegative(boolean serialize, boolean isBuilding) throws Exception {
        when(build.isBuilding()).thenReturn(isBuilding);

        ConsoleLogParser.Result result = new ConsoleLogParser.Result();
        result.lineNumber = 0;
        result.atNewLine = true;
        assertThat(seek(serialize, -logLength - 1), is(result));
    }

    private ConsoleLogParser.Result seek(boolean serialize, long pos) throws IOException {
        ConsoleLogParser parser = new ConsoleLogParser(pos);
        if (serialize) {
            parser = SerializationUtils.clone(parser);
        }
        return parser.seek(build);
    }
}
