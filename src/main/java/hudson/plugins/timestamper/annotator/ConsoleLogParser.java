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

import hudson.model.Run;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;

/**
 * Utility class that is able to parse the console log file of a build.
 * 
 * @author Steven G. Brown
 */
final class ConsoleLogParser {

  private final Run<?, ?> build;

  private int lineNumber;

  private boolean atNewLine = true;

  private boolean endOfFile;

  /**
   * Create a console log parser for a build.
   * 
   * @param build
   */
  ConsoleLogParser(Run<?, ?> build) {
    this.build = Preconditions.checkNotNull(build);
  }

  /**
   * Skip to a position in the console log file.
   * 
   * @param pos
   *          the position, measured in bytes from the beginning of the file
   * @throws IOException
   */
  void seek(long pos) throws IOException {
    InputStream inputStream = null;
    try {
      inputStream = new BufferedInputStream(build.getLogInputStream());
      for (long i = 0; i < pos; i++) {
        int value = inputStream.read();
        if (value == -1) {
          endOfFile = true;
          return;
        }
        atNewLine = value == 0x0A;
        if (atNewLine) {
          lineNumber++;
        }
      }
    } finally {
      Closeables.closeQuietly(inputStream);
    }
  }

  /**
   * @return the current line number, starting at line zero
   */
  int getLineNumber() {
    return lineNumber;
  }

  /**
   * @return whether the last-read character was a new line
   */
  boolean atNewLine() {
    return atNewLine;
  }

  /**
   * @return whether the last {@link #seek(long)} went past the end of the file
   */
  boolean endOfFile() {
    return endOfFile;
  }
}
