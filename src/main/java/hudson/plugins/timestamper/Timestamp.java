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
package hudson.plugins.timestamper;

import hudson.MarkupText;

import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.time.FastDateFormat;

/**
 * A time-stamp, consisting of the elapsed time and the clock time.
 * 
 * @author Steven G. Brown
 * @since 1.3.2
 */
public final class Timestamp {

  /**
   * The elapsed time in milliseconds since the start of the build.
   * <p>
   * Provided by <a href=
   * "http://docs.oracle.com/javase/6/docs/api/java/lang/System.html#nanoTime()"
   * >System.nanoTime()</a>.
   * <p>
   * For builds created with the Timestamper plugin prior to version 1.4, the
   * elapsed time will instead be calculated based on {@link #millisSinceEpoch}
   * and the start time of the build. In this case, the elapsed time values may
   * not always be increasing and may be negative.
   */
  public final long elapsedMillis;

  /**
   * The clock time in milliseconds since midnight, January 1, 1970 UTC.
   * <p>
   * Provided by <a href=
   * "http://docs.oracle.com/javase/6/docs/api/java/lang/System.html#currentTimeMillis()"
   * >System.currentTimeMillis()</a>.
   */
  public final long millisSinceEpoch;

  /**
   * Create a {@link Timestamp}.
   * 
   * @param elapsedMillis
   *          the elapsed time in milliseconds since the start of the build
   * @param millisSinceEpoch
   *          the clock time in milliseconds since midnight, January 1, 1970 UTC
   */
  public Timestamp(long elapsedMillis, long millisSinceEpoch) {
    this.elapsedMillis = elapsedMillis;
    this.millisSinceEpoch = millisSinceEpoch;
  }

  /**
   * Format this time-stamp and insert it into the given text.
   * 
   * @param text
   *          the text to modify
   * @param timestampFormat
   *          the time-stamp format
   */
  public void markup(MarkupText text, String timestampFormat) {
    String formattedDate = FastDateFormat.getInstance(timestampFormat).format(
        new Date(millisSinceEpoch));
    // Add as end tag, which will be inserted prior to tags added by other
    // console notes (e.g. AntTargetNote).
    text.addMarkup(0, 0, "", formattedDate);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = 17;
    result = 37 * result + (int) (elapsedMillis ^ (elapsedMillis >>> 32));
    result = 37 * result + (int) (millisSinceEpoch ^ (millisSinceEpoch >>> 32));
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Timestamp) {
      Timestamp other = (Timestamp) obj;
      return elapsedMillis == other.elapsedMillis
          && millisSinceEpoch == other.millisSinceEpoch;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
        .append("elapsedMillis", elapsedMillis)
        .append("millisSinceEpoch", millisSinceEpoch).toString();
  }
}
