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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Utility methods for working with {@link Closeable} objects.
 * <p>
 * Same interface as the Guava 17.0 Closeable class. This is useful because the
 * Timestamper plugin is currently using an earlier version.
 * 
 * @author Steven G. Brown
 */
public class Closeables {

  /**
   * See Guava method Closeables.close(Closeables, boolean).
   * 
   * @param closeable
   * @param swallowIOException
   * @throws IOException
   */
  public static void close(Closeable closeable, boolean swallowIOException)
      throws IOException {
    com.google.common.io.Closeables.close(closeable, swallowIOException);
  }

  /**
   * See Guava 17.0 method Closeables.closeQuietly(InputStream).
   * 
   * @param inputStream
   */
  public static void closeQuietly(InputStream inputStream) {
    try {
      com.google.common.io.Closeables.close(inputStream, true);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  /**
   * See Guava 17.0 method Closeables.closeQuietly(Reader).
   * 
   * @param reader
   */
  public static void closeQuietly(Reader reader) {
    try {
      com.google.common.io.Closeables.close(reader, true);
    } catch (IOException impossible) {
      throw new AssertionError(impossible);
    }
  }

  private Closeables() {
  }
}
