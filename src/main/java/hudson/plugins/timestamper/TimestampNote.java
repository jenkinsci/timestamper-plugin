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

import java.sql.Date;
import java.text.SimpleDateFormat;

/**
 * Time-stamp note that is inserted into the console output.
 * 
 * @author Steven G. Brown
 */
public final class TimestampNote extends ConsoleNote<Object> {

  /**
   * Serialization UID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Thread-local variable that provides the time-stamp format.
   */
  private static final ThreadLocal<SimpleDateFormat> dateFormatThreadLocal = new ThreadLocal<SimpleDateFormat>() {

    /**
     * {@inheritDoc}
     */
    @Override
    protected SimpleDateFormat initialValue() {
      return new SimpleDateFormat("HH:mm:ss");
    }
  };

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
  TimestampNote(long millisSinceEpoch) {
    this.millisSinceEpoch = millisSinceEpoch;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConsoleAnnotator<Object> annotate(Object context, MarkupText text,
      int charPos) {
    String formattedDate = dateFormatThreadLocal.get().format(
        new Date(millisSinceEpoch));
    text.addMarkup(0, "<b>" + formattedDate + "</b>  ");
    return null;
  }
}
