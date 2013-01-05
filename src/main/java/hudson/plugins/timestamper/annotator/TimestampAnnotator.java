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
import hudson.plugins.timestamper.TimestampFormatter;
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

  private final TimestampFormatter formatter;

  private final long offset;

  private TimestampsIO.Reader timestampsReader;

  /**
   * Create a new {@link TimestampAnnotator}.
   * 
   * @param formatter
   *          the time-stamp formatter
   * @param offset
   *          the offset for viewing the console log. A non-negative offset is
   *          from the start of the file, and a negative offset is back from the
   *          end of the file.
   */
  TimestampAnnotator(TimestampFormatter formatter, long offset) {
    this.formatter = formatter;
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

    try {
      if (timestampsReader == null) {
        timestampsReader = new TimestampsIO.Reader(build);
        return markup(text,
            timestampsReader.find(consoleFilePointer(build), build));
      }
      Timestamp timestamp = timestampsReader.next();
      if (timestamp != null) {
        return markup(text, timestamp);
      }
    } catch (IOException ex) {
      LOGGER.log(Level.WARNING,
          "Error reading timestamps for " + build.getFullDisplayName(), ex);
    }
    return null;
  }

  /**
   * Get the console file pointer from the offset.
   * 
   * @param build
   *          the build
   * @return the console log pointer
   */
  private long consoleFilePointer(Run<?, ?> build) {
    long start = offset;
    if (offset < 0) {
      start = build.getLogFile().length() + offset;
    }
    return start;
  }

  /**
   * Add a time-stamp to the given text.
   * 
   * @param text
   *          the text to modify
   * @param timestamp
   *          the time-stamp
   * @return {@code this}
   */
  private TimestampAnnotator markup(MarkupText text, Timestamp timestamp) {
    if (timestamp != null) {
      formatter.markup(text, timestamp);
    }
    return this;
  }
}
