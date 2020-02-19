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

import java.util.Objects;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * A time-stamp, consisting of the elapsed time and the clock time.
 *
 * @author Steven G. Brown
 */
@Immutable
public final class Timestamp {

  /** The elapsed time in milliseconds since the start of the build. */
  public final long elapsedMillis;

  /** Whether the elapsed time is known. */
  public final boolean elapsedMillisKnown;

  /** The clock time in milliseconds since midnight, January 1, 1970 UTC. */
  public final long millisSinceEpoch;

  /**
   * Create a {@link Timestamp}.
   *
   * @param elapsedMillis the elapsed time in milliseconds since the start of the build
   * @param millisSinceEpoch the clock time in milliseconds since midnight, January 1, 1970 UTC
   */
  public Timestamp(long elapsedMillis, long millisSinceEpoch) {
    this(Long.valueOf(elapsedMillis), millisSinceEpoch);
  }

  /**
   * Create a {@link Timestamp}.
   *
   * @param elapsedMillis the elapsed time in milliseconds since the start of the build (null if
   *     unknown)
   * @param millisSinceEpoch the clock time in milliseconds since midnight, January 1, 1970 UTC
   */
  public Timestamp(Long elapsedMillis, long millisSinceEpoch) {
    this.elapsedMillis = (elapsedMillis == null ? 0 : elapsedMillis);
    this.elapsedMillisKnown = (elapsedMillis != null);
    this.millisSinceEpoch = millisSinceEpoch;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Objects.hash(elapsedMillis, elapsedMillisKnown, millisSinceEpoch);
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Timestamp) {
      Timestamp other = (Timestamp) obj;
      return elapsedMillis == other.elapsedMillis
          && elapsedMillisKnown == other.elapsedMillisKnown
          && millisSinceEpoch == other.millisSinceEpoch;
    }
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("elapsedMillis", elapsedMillisKnown ? elapsedMillis : "(unknown)")
        .append("millisSinceEpoch", millisSinceEpoch)
        .toString();
  }
}
