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

import javax.annotation.concurrent.Immutable;

import com.google.common.io.Closeables;

/**
 * Implementation of ConsoleLogParser.
 * 
 * @author Steven G. Brown
 */
@Immutable
class ConsoleLogParserImpl implements ConsoleLogParser {

  private static final long serialVersionUID = 1L;

  private final long pos;

  /**
   * Create a new {@link ConsoleLogParserImpl}.
   * 
   * @param pos
   *          the position to find in the console log file. A non-negative
   *          position is from the start of the file, and a negative position is
   *          back from the end of the file.
   */
  ConsoleLogParserImpl(long pos) {
    this.pos = pos;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConsoleLogParser.Result seek(Run<?, ?> build) throws IOException {
    ConsoleLogParser.Result result = new ConsoleLogParser.Result();
    result.atNewLine = true;
    InputStream inputStream = new BufferedInputStream(build.getLogInputStream());
    boolean threw = true;
    try {
      long posFromStart = pos;
      if (pos < 0) {
        posFromStart = build.getLogText().length() + pos;
      }
      for (long i = 0; i < posFromStart; i++) {
        int value = inputStream.read();
        if (value == -1) {
          result.endOfFile = true;
          break;
        }
        result.atNewLine = value == 0x0A;
        if (result.atNewLine) {
          result.lineNumber++;
        }
      }
      threw = false;
    } finally {
      Closeables.close(inputStream, threw);
    }
    return result;
  }
}
