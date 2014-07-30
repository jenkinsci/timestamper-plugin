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
import hudson.plugins.timestamper.action.TimestampsAction;
import hudson.plugins.timestamper.format.TimestampFormatter;

/**
 * Time-stamp note that was inserted into the console note by the Timestamper
 * plugin prior to version 1.4.
 * <p>
 * The time-stamps are now stored in a separate file, which allows a more
 * compact format to be used. Having the timestamps in the console log file was
 * also inconvenient when reading the file in a text editor.
 * <p>
 * It is possible to restore the old behaviour of inserting console notes by
 * setting a system property: {@link #getSystemProperty()}. This will allow
 * scripts which expect to find the time-stamps within the console log file to
 * continue working. New scripts should rely on the page served by
 * {@link TimestampsAction} instead.
 * 
 * @author Steven G. Brown
 */
public final class TimestampNote extends ConsoleNote<Object> {

  /**
   * Serialization UID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Get the system property which will cause these console notes to be inserted
   * into the console log file.
   * 
   * @return the system property
   */
  public static String getSystemProperty() {
    return "timestamper-consolenotes";
  }

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
      TimestampFormatter formatter = TimestampFormatter.get();
      Timestamp timestamp = getTimestamp(build);
      formatter.markup(text, timestamp);
    }
    return null; // each time-stamp note affects one line only
  }
}
