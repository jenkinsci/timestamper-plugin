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

import static com.google.common.base.Preconditions.checkNotNull;
import hudson.MarkupText;
import hudson.console.ConsoleAnnotator;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.format.TimestampFormat;
import hudson.plugins.timestamper.format.TimestampFormatProvider;
import hudson.plugins.timestamper.io.TimestampsFileReader;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.CheckForNull;

import com.google.common.base.Optional;

/**
 * Inserts formatted time-stamps into the annotated console output.
 * 
 * @author Steven G. Brown
 */
public final class TimestampAnnotator extends ConsoleAnnotator<Object> {

  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = Logger
      .getLogger(TimestampAnnotator.class.getName());

  private final ConsoleLogParser logParser;

  @CheckForNull
  private TimestampsFileReader timestampsReader;

  @CheckForNull
  private transient TimestampFormat format;

  /**
   * Create a new {@link TimestampAnnotator}.
   * 
   * @param logParser
   *          the console log parser
   */
  TimestampAnnotator(ConsoleLogParser logParser) {
    this.logParser = checkNotNull(logParser);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConsoleAnnotator<Object> annotate(Object context, MarkupText text) {
    if (!(context instanceof Run<?, ?>)) {
      return null; // do not annotate the following lines
    }
    Run<?, ?> build = (Run<?, ?>) context;

    try {
      if (timestampsReader == null) {
        ConsoleLogParserImpl.Result logPosition = logParser.seek(build);
        if (logPosition.endOfFile) {
          return null; // do not annotate the following lines
        }
        timestampsReader = new TimestampsFileReader(build);
        timestampsReader.skip(logPosition.lineNumber);
        Optional<Timestamp> timestamp = timestampsReader.read();
        if (logPosition.atNewLine && timestamp.isPresent()) {
          markup(text, timestamp.get());
        }
        return this;
      }
      Optional<Timestamp> timestamp = timestampsReader.read();
      if (timestamp.isPresent()) {
        markup(text, timestamp.get());
        return this;
      }
    } catch (IOException ex) {
      LOGGER.log(Level.WARNING,
          "Error reading timestamps for " + build.getFullDisplayName(), ex);
    }
    if (timestampsReader != null) {
      timestampsReader.close();
    }
    return null; // do not annotate the following lines
  }

  private void markup(MarkupText text, Timestamp timestamp) {
    if (format == null) {
      format = TimestampFormatProvider.get();
    }
    format.markup(text, timestamp);
  }
}
