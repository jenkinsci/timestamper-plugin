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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;

/**
 * Unit test for the {@link Timestamp} class.
 *
 * @author Steven G. Brown
 */
public class TimestampTest {

  @Test
  public void testConstructor() {
    Timestamp timestamp = new Timestamp(123, 42000);
    assertThat(
        Arrays.asList(
            timestamp.elapsedMillis, timestamp.elapsedMillisKnown, timestamp.millisSinceEpoch),
        is(Arrays.asList(123L, true, 42000L)));
  }

  @Test
  public void testConstructor_unknownElapsed() {
    Timestamp timestamp = new Timestamp(null, 42000);
    assertThat(
        Arrays.asList(
            timestamp.elapsedMillis, timestamp.elapsedMillisKnown, timestamp.millisSinceEpoch),
        is(Arrays.asList(0L, false, 42000L)));
  }

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Timestamp.class).suppress(Warning.STRICT_INHERITANCE).verify();
  }
}
