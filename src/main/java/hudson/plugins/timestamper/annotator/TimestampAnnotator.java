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

import hudson.MarkupText;
import hudson.console.ConsoleAnnotator;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.TimestampsIO;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Inserts formatted time-stamps into the annotated console output.
 * 
 * @author Steven G. Brown
 * @since 1.4
 */
public final class TimestampAnnotator extends ConsoleAnnotator<Object> {

  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = Logger
      .getLogger(TimestampAnnotator.class.getName());

  private final String timestampFormat;

  private final long offset;

  private TimestampsIO.Reader timestampsReader;

  /**
   * Create a new {@link TimestampAnnotator}.
   * 
   * @param timestampFormat
   *          the time-stamp format
   * @param offset
   *          the offset for viewing the console log. A non-negative offset is
   *          from the start of the file, and a negative offset is back from the
   *          end of the file.
   */
  TimestampAnnotator(String timestampFormat, long offset) {
    this.timestampFormat = timestampFormat;
    this.offset = offset;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConsoleAnnotator<Object> annotate(Object context, MarkupText text) {
    if (!(context instanceof Run<?, ?>)) {
      return null;
    }
    Run<?, ?> build = (Run<?, ?>) context;

    Timestamp timestamp = null;
    try {
      if (timestampsReader == null) {
        timestampsReader = new TimestampsIO.Reader(build);
        timestampsReader.find(consoleFilePointer(build, offset), build);
        return this;
      }
      timestamp = timestampsReader.next();
    } catch (IOException ex) {
      LOGGER.log(Level.WARNING,
          "Error reading timestamps for " + build.getFullDisplayName(), ex);
    }

    if (timestamp == null) {
      // No more time-stamps or an error.
      return null;
    }
    timestamp.markup(text, timestampFormat);
    return this;
  }

  /**
   * Get the console file pointer from the offset.
   * 
   * @param build
   *          the build
   * @param offset
   *          the offset
   * @return the console log pointer
   */
  private static long consoleFilePointer(Run<?, ?> build, long offset) {
    long start = offset;
    if (offset < 0) {
      start = build.getLogFile().length() + offset;
    }
    return start;
  }
}
