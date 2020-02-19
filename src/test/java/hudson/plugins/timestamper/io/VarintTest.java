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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.primitives.Bytes;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

/**
 * Unit test for the Varint class.
 *
 * @author Steven G. Brown
 */
@RunWith(Theories.class)
public class VarintTest {

  private static Pattern BINARY_PATTERN = Pattern.compile("([01]{8} )*[01]{8}");

  /** */
  @DataPoint
  public static VarintValue MIN =
      new VarintValue(
          Long.MIN_VALUE,
          binary(
              "10000000 10000000 10000000 10000000 10000000 10000000 10000000 10000000 10000000 00000001"));

  /** */
  @DataPoint
  public static VarintValue NEGATIVE_ONE =
      new VarintValue(
          -1,
          binary(
              "11111111 11111111 11111111 11111111 11111111 11111111 11111111 11111111 11111111 00000001"));

  /** */
  @DataPoint public static VarintValue ZERO = new VarintValue(0, binary("00000000"));

  /** */
  @DataPoint public static VarintValue ONE = new VarintValue(1, binary("00000001"));

  /** Maximum value that can be stored in a single byte. */
  @DataPoint public static VarintValue ONE_BYTE_MAX = new VarintValue(127, binary("01111111"));

  /** Higher than {@link #ONE_BYTE_MAX}. Need two bytes. */
  @DataPoint
  public static VarintValue TWO_BYTES_MIN = new VarintValue(128, binary("10000000 00000001"));

  /** */
  @DataPoint
  public static VarintValue THREE_HUNDRED = new VarintValue(300, binary("10101100 00000010"));

  /** */
  @DataPoint
  public static VarintValue FIVE_HUNDRED = new VarintValue(500, binary("11110100 00000011"));

  /** */
  @DataPoint
  public static VarintValue MAX =
      new VarintValue(
          Long.MAX_VALUE,
          binary(
              "11111111 11111111 11111111 11111111 11111111 11111111 11111111 11111111 01111111"));

  static class VarintValue {

    VarintValue(long value, byte[] varintEncoding) {
      this.value = value;
      this.varintEncoding = varintEncoding;
    }

    long value;
    byte[] varintEncoding;
  }

  /**
   * @param value
   * @throws Exception
   */
  @Theory
  public void testWriteSingleVarint(VarintValue value) throws Exception {
    byte[] buffer = new byte[value.varintEncoding.length];
    Varint.write(value.value, buffer, 0);
    assertThat(buffer, is(value.varintEncoding));
  }

  /**
   * @param value
   * @throws Exception
   */
  @Theory
  public void testReadSingleVarint(VarintValue value) throws Exception {
    long readValue = Varint.read(new ByteArrayInputStream(value.varintEncoding));
    assertThat(readValue, is(value.value));
  }

  /**
   * @param valueOne
   * @param valueTwo
   * @throws Exception
   */
  @Theory
  public void testWriteTwoVarints(VarintValue valueOne, VarintValue valueTwo) throws Exception {
    byte[] buffer = new byte[valueOne.varintEncoding.length + valueTwo.varintEncoding.length];
    int offset = Varint.write(valueOne.value, buffer, 0);
    Varint.write(valueTwo.value, buffer, offset);
    assertThat(buffer, is(Bytes.concat(valueOne.varintEncoding, valueTwo.varintEncoding)));
  }

  /**
   * @param valueOne
   * @param valueTwo
   * @throws Exception
   */
  @Theory
  public void testReadTwoVarints(VarintValue valueOne, VarintValue valueTwo) throws Exception {
    InputStream inputStream =
        new ByteArrayInputStream(Bytes.concat(valueOne.varintEncoding, valueTwo.varintEncoding));
    long readValueOne = Varint.read(inputStream);
    assertThat("first value", readValueOne, is(valueOne.value));
    long readValueTwo = Varint.read(inputStream);
    assertThat("second value", readValueTwo, is(valueTwo.value));
  }

  /**
   * @param value
   * @throws Exception
   */
  @Theory
  public void testOffset(VarintValue value) throws Exception {
    byte[] buffer = new byte[value.varintEncoding.length];
    int offset = Varint.write(value.value, buffer, 0);
    assertThat(offset, is(value.varintEncoding.length));
  }

  private static byte[] binary(String binary) {
    if (!BINARY_PATTERN.matcher(binary).matches()) {
      throw new IllegalArgumentException(binary);
    }
    List<Byte> bytes = new ArrayList<Byte>();
    for (String byteString : binary.split(" ")) {
      bytes.add((byte) Integer.parseInt(byteString, 2));
    }
    return Bytes.toArray(bytes);
  }
}
