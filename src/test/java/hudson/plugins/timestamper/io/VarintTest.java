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

import com.google.common.primitives.Bytes;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit test for the Varint class.
 *
 * @author Steven G. Brown
 */
class VarintTest {

    private static final Pattern BINARY_PATTERN = Pattern.compile("([01]{8} )*[01]{8}");

    static Stream<VarintValue> data() {
        return Stream.of(
                new VarintValue(
                        Long.MIN_VALUE,
                        binary(
                                "10000000 10000000 10000000 10000000 10000000 10000000 10000000 10000000 10000000 00000001")),
                new VarintValue(
                        -1,
                        binary(
                                "11111111 11111111 11111111 11111111 11111111 11111111 11111111 11111111 11111111 00000001")),
                new VarintValue(0, binary("00000000")),
                new VarintValue(1, binary("00000001")),
                /** Maximum value that can be stored in a single byte. */
                new VarintValue(127, binary("01111111")),
                /** Higher than {@link #ONE_BYTE_MAX}. Need two bytes. */
                new VarintValue(128, binary("10000000 00000001")),
                new VarintValue(300, binary("10101100 00000010")),
                new VarintValue(500, binary("11110100 00000011")),
                new VarintValue(
                        Long.MAX_VALUE,
                        binary("11111111 11111111 11111111 11111111 11111111 11111111 11111111 11111111 01111111")));
    }

    static class VarintValue {

        VarintValue(long value, byte[] varintEncoding) {
            this.value = value;
            this.varintEncoding = varintEncoding;
        }

        final long value;
        final byte[] varintEncoding;
    }

    @ParameterizedTest
    @MethodSource("data")
    void testWriteSingleVarint(VarintValue value) throws Exception {
        byte[] buffer = new byte[value.varintEncoding.length];
        Varint.write(value.value, buffer, 0);
        assertThat(buffer, is(value.varintEncoding));
    }

    @ParameterizedTest
    @MethodSource("data")
    void testReadSingleVarint(VarintValue value) throws Exception {
        long readValue = Varint.read(new ByteArrayInputStream(value.varintEncoding));
        assertThat(readValue, is(value.value));
    }

    @ParameterizedTest
    @MethodSource("data")
    void testWriteTwoVarints(VarintValue value) throws Exception {
        VarintValue other = data().unordered().findFirst().orElseThrow();

        byte[] buffer = new byte[value.varintEncoding.length + other.varintEncoding.length];
        int offset = Varint.write(value.value, buffer, 0);
        Varint.write(other.value, buffer, offset);
        assertThat(buffer, is(Bytes.concat(value.varintEncoding, other.varintEncoding)));
    }

    @ParameterizedTest
    @MethodSource("data")
    void testReadTwoVarints(VarintValue value) throws Exception {
        VarintValue other = data().unordered().findFirst().orElseThrow();

        InputStream inputStream = new ByteArrayInputStream(Bytes.concat(value.varintEncoding, other.varintEncoding));
        long readValueOne = Varint.read(inputStream);
        assertThat("first value", readValueOne, is(value.value));
        long readValueTwo = Varint.read(inputStream);
        assertThat("second value", readValueTwo, is(other.value));
    }

    @ParameterizedTest
    @MethodSource("data")
    void testOffset(VarintValue value) throws Exception {
        byte[] buffer = new byte[value.varintEncoding.length];
        int offset = Varint.write(value.value, buffer, 0);
        assertThat(offset, is(value.varintEncoding.length));
    }

    private static byte[] binary(String binary) {
        if (!BINARY_PATTERN.matcher(binary).matches()) {
            throw new IllegalArgumentException(binary);
        }
        List<Byte> bytes = new ArrayList<>();
        for (String byteString : binary.split(" ")) {
            bytes.add((byte) Integer.parseInt(byteString, 2));
        }
        return Bytes.toArray(bytes);
    }
}
