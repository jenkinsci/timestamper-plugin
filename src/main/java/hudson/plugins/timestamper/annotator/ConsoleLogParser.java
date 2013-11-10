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

import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Parser that is able to find a position in the console log file of a build.
 * 
 * @author Steven G. Brown
 */
interface ConsoleLogParser extends Serializable {

  /**
   * Skip to a position in the console log file.
   * 
   * @param build
   *          the build to inspect
   * @return the result
   * @throws IOException
   */
  Result seek(Run<?, ?> build) throws IOException;

  static final class Result {

    /**
     * The current line number, starting at line zero.
     */
    int lineNumber;

    /**
     * Whether the last-read character was a new line.
     */
    boolean atNewLine;

    /**
     * Whether the position is past the end of the file.
     */
    boolean endOfFile;

    @Override
    public String toString() {
      return ReflectionToStringBuilder.toString(this,
          ToStringStyle.SHORT_PREFIX_STYLE);
    }
  }
}
