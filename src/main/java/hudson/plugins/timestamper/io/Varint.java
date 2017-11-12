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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for reading and writing long values in Base 128 Varint format. See:
 * https://developers.google.com/protocol-buffers/docs/encoding#varints
 *
 * @author Steven G. Brown
 */
final class Varint {

  /**
   * Write a value to the given byte array as a Base 128 Varint. See:
   * https://developers.google.com/protocol-buffers/docs/encoding#varints
   *
   * @param value
   * @param writeTo
   * @param offset
   * @return the new offset after writing the value
   * @throws IOException
   */
  static int write(long value, byte[] writeTo, int offset) throws IOException {
    while (true) {
      if ((value & ~0x7FL) == 0) {
        writeTo[offset] = (byte) value;
        offset++;
        return offset;
      }
      writeTo[offset] = (byte) (((int) value & 0x7F) | 0x80);
      offset++;
      value >>>= 7;
    }
  }

  /**
   * Read a value as a Base 128 Varint. See:
   * https://developers.google.com/protocol-buffers/docs/encoding#varints
   *
   * @param inputStream
   * @return the value
   * @throws IOException
   */
  static long read(InputStream inputStream) throws IOException {
    int shift = 0;
    long result = 0;
    while (shift < 64) {
      final int value = inputStream.read();
      if (value == -1) {
        throw new EOFException();
      }
      final byte b = (byte) value;
      result |= (long) (b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        return result;
      }
      shift += 7;
    }
    throw new IOException("Malformed varint");
  }

  private Varint() {}
}
