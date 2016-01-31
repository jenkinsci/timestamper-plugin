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
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.plugins.timestamper.action.TimestampsAction;
import hudson.plugins.timestamper.format.TimestampFormatter;

/**
 * Time-stamp console note.
 * <p>
 * These are inserted into the log file when:
 * <ul>
 * <li>The build does not extend {@link AbstractBuild}, e.g. a pipeline job.</li>
 * <li>Running the Timestamper plugin prior to version 1.4.</li>
 * <li>The system property is set: ({@link #getSystemProperty()}). The is
 * intended to support scripts that were written prior to Timestamper 1.4 to
 * parse the log files. New scripts should query the {@code /timestamps} URL
 * instead (see {@link TimestampsAction}).</li>
 * </ul>
 * <p>
 * Otherwise, the time-stamps are stored in a separate file, which allows a more
 * compact format to be used and avoids filling the log files with encoded
 * console notes.
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
   * The elapsed time in milliseconds since the start of the build.
   * 
   * @since 1.7.4
   */
  private final Long elapsedMillis;

  /**
   * Milliseconds since the epoch.
   */
  private final long millisSinceEpoch;

  /**
   * Create a new {@link TimestampNote}.
   * 
   * @param elapsedMillis
   *          the elapsed time in milliseconds since the start of the build
   * @param millisSinceEpoch
   *          milliseconds since the epoch
   */
  public TimestampNote(long elapsedMillis, long millisSinceEpoch) {
    this.elapsedMillis = elapsedMillis;
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
    return getTimestamp((Object) build);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConsoleAnnotator<Object> annotate(Object context, MarkupText text,
      int charPos) {
    TimestampFormatter formatter = TimestampFormatter.get();
    Timestamp timestamp = getTimestamp(context);
    formatter.markup(text, timestamp);
    return null; // each time-stamp note affects one line only
  }

  private Timestamp getTimestamp(Object context) {
    if (context instanceof Run<?, ?>) {
      // The elapsed time can be determined by using the build start time
      Run<?, ?> build = (Run<?, ?>) context;
      long buildStartTime = build.getTimeInMillis();
      return new Timestamp(millisSinceEpoch - buildStartTime, millisSinceEpoch);
    }
    // Use the elapsed time recorded in this console note, if known
    return new Timestamp(elapsedMillis, millisSinceEpoch);
  }
}
