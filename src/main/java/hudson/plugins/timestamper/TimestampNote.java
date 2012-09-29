/*
 * The MIT License
 * 
 * Copyright (c) 2010 Steven G. Brown
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
package hudson.plugins.timestamper;

import hudson.MarkupText;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleNote;
import hudson.model.Run;

/**
 * Time-stamp note that is inserted into the console log.
 * 
 * @author Steven G. Brown
 * @since 1.0
 */
public final class TimestampNote extends ConsoleNote<Object> {

  /**
   * Serialization UID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Milliseconds since the epoch.
   */
  private final long millisSinceEpoch;

  /**
   * Create a new {@link TimestampNote}.
   * 
   * @param millisSinceEpoch
   *          milliseconds since the epoch
   */
  public TimestampNote(long millisSinceEpoch) {
    this.millisSinceEpoch = millisSinceEpoch;
  }

  /**
   * Get the time-stamp recorded by this console note.
   * 
   * @param build
   *          the build
   * @return the time-stamp
   */
  public Timestamp getTimestamp(Run<?, ?> build) {
    long elapsedMillis = millisSinceEpoch - build.getTimeInMillis();
    return new Timestamp(elapsedMillis, millisSinceEpoch);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConsoleAnnotator<Object> annotate(Object context, MarkupText text,
      int charPos) {
    if (context instanceof Run<?, ?>) {
      Run<?, ?> build = (Run<?, ?>) context;
      Timestamp timestamp = getTimestamp(build);
      String timestampFormat = TimestamperConfig.get().getTimestampFormat();
      timestamp.markup(text, timestampFormat);
    }
    return null;
  }
}
