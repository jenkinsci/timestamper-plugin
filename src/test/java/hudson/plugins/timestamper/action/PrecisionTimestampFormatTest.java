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

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import hudson.plugins.timestamper.Timestamp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.ImmutableList;

/**
 * Unit test for the {@link PrecisionTimestampFormat} class.
 * 
 * @author Steven G. Brown
 */
@RunWith(Parameterized.class)
public class PrecisionTimestampFormatTest {

  private static final List<Timestamp> TIMESTAMPS = ImmutableList.of(
      new Timestamp(0, TimeUnit.SECONDS.toMillis(1)),
      //
      new Timestamp(1, TimeUnit.MINUTES.toMillis(1)),
      //
      new Timestamp(10, TimeUnit.HOURS.toMillis(1)),
      //
      new Timestamp(100, TimeUnit.DAYS.toMillis(1)),
      //
      new Timestamp(1000, TimeUnit.DAYS.toMillis(2)),
      //
      new Timestamp(10000, TimeUnit.DAYS.toMillis(3)));

  /**
   * @return the test data
   */
  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] { { 0, asList("0", "0", "0", "0", "1", "10") },
        { 1, asList("0.0", "0.0", "0.0", "0.1", "1.0", "10.0") },
        { 2, asList("0.00", "0.00", "0.01", "0.10", "1.00", "10.00") },
        { 3, asList("0.000", "0.001", "0.010", "0.100", "1.000", "10.000") },
        { 4, asList("0.0000", "0.0010", "0.0100", "0.1000", "1.0000", "10.0000") },
        { 5, asList("0.00000", "0.00100", "0.01000", "0.10000", "1.00000", "10.00000") },
        { 6, asList("0.000000", "0.001000", "0.010000", "0.100000", "1.000000", "10.000000") },
        { 7, asList("0.0000000", "0.0010000", "0.0100000", "0.1000000", "1.0000000",
            "10.0000000") },
        { 8, asList("0.00000000", "0.00100000", "0.01000000", "0.10000000", "1.00000000",
            "10.00000000") },
        { 9, asList("0.000000000", "0.001000000", "0.010000000", "0.100000000", "1.000000000",
            "10.000000000") } });
  }

  /**
   */
  @Parameter(0)
  public int precision;

  /**
   */
  @Parameter(1)
  public List<String> expectedResult;

  /**
   */
  @Test
  public void testApply() {
    PrecisionTimestampFormat format = new PrecisionTimestampFormat(precision);

    List<String> result = new ArrayList<String>();
    for (Timestamp timestamp : TIMESTAMPS) {
      result.add(format.apply(timestamp));
    }

    assertThat(result, is(expectedResult));
  }

  /**
   */
  @Test
  public void testEqualsAndHashCode() {
    EqualsVerifier.forClass(PrecisionTimestampFormat.class).verify();
  }
}
