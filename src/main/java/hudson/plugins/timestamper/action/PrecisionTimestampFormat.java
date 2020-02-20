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
package hudson.plugins.timestamper.action;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Strings;
import hudson.plugins.timestamper.Timestamp;
import java.util.function.Function;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Formats time-stamps as the elapsed time in seconds, with a certain number of places after decimal
 * point.
 *
 * @author Steven G. Brown
 */
final class PrecisionTimestampFormat implements Function<Timestamp, String> {

  /** The number of places to display after the decimal point. */
  @Nonnegative private final int precision;

  PrecisionTimestampFormat(int precision) {
    checkArgument(precision >= 0);
    this.precision = precision;
  }

  /** {@inheritDoc} */
  @Override
  public String apply(@Nonnull Timestamp timestamp) {
    long seconds = timestamp.elapsedMillis / 1000;
    if (precision == 0) {
      return String.valueOf(seconds);
    }
    long millis = timestamp.elapsedMillis % 1000;
    String fractional = String.format("%03d", millis);
    if (precision <= 3) {
      fractional = fractional.substring(0, precision);
    } else {
      fractional += Strings.repeat("0", precision - 3);
    }
    return String.valueOf(seconds) + "." + fractional;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Integer.valueOf(precision).hashCode();
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof PrecisionTimestampFormat) {
      PrecisionTimestampFormat other = (PrecisionTimestampFormat) obj;
      return precision == other.precision;
    }
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return new ToStringBuilder(this).append("precision", precision).toString();
  }
}
