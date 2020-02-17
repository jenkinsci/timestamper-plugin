/*
 * The MIT License
 *
 * Copyright (c) 2016 Steven G. Brown
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
package hudson.plugins.timestamper.format;

import static com.google.common.base.Preconditions.checkNotNull;

import hudson.plugins.timestamper.Timestamp;
import javax.annotation.Nonnull;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.time.DurationFormatUtils;

/**
 * Converts a time-stamp to the elapsed time format.
 *
 * @author Steven G. Brown
 */
public final class ElapsedTimestampFormat extends TimestampFormat {

  private final String elapsedTimeFormat;

  public ElapsedTimestampFormat(String elapsedTimeFormat) {
    this.elapsedTimeFormat = checkNotNull(elapsedTimeFormat);
  }

  /** {@inheritDoc} */
  @Override
  public String apply(@Nonnull Timestamp timestamp) {
    if (timestamp.elapsedMillisKnown) {
      return DurationFormatUtils.formatDuration(timestamp.elapsedMillis, elapsedTimeFormat);
    }
    return "";
  }

  /** {@inheritDoc} */
  @Override
  public String getPlainTextUrl() {
    String elapsedParamValue = elapsedTimeFormat;
    elapsedParamValue = FormatStringUtils.stripHtmlTags(elapsedParamValue);
    elapsedParamValue = FormatStringUtils.trim(elapsedParamValue);

    return "timestamps/?elapsed=" + elapsedParamValue + "&appendLog";
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return elapsedTimeFormat.hashCode();
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ElapsedTimestampFormat) {
      ElapsedTimestampFormat other = (ElapsedTimestampFormat) obj;
      return elapsedTimeFormat.equals(other.elapsedTimeFormat);
    }
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return new ToStringBuilder(this).append("format", elapsedTimeFormat).toString();
  }
}
