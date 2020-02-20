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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test for {@link TimeZoneUtils}.
 *
 * @author Steven G. Brown
 */
@RunWith(Parameterized.class)
public class TimeZoneUtilsTest {

  /** @return the test cases */
  @Parameters(name = "getTimeZoneId[{0}]={1}")
  public static Iterable<Object[]> data() {
    List<Object[]> testCases = new ArrayList<>();
    testCases.add(new Object[] {0, "GMT"});
    testCases.add(new Object[] {offset(0, 30), "GMT+0:30"});
    testCases.add(new Object[] {-offset(0, 30), "GMT-0:30"});
    testCases.add(new Object[] {-offset(3, 30), "GMT-3:30"});
    testCases.add(new Object[] {offset(5, 45), "GMT+5:45"});
    testCases.add(new Object[] {offset(5, 30), "GMT+5:30"});
    for (int hour = 1; hour <= 14; hour++) {
      testCases.add(new Object[] {offset(hour, 0), "GMT+" + hour});
      testCases.add(new Object[] {-offset(hour, 0), "GMT-" + hour});
    }
    return testCases;
  }

  private static long offset(int hours, int minutes) {
    return TimeUnit.HOURS.toMillis(hours) + TimeUnit.MINUTES.toMillis(minutes);
  }

  @Parameter(0)
  public long offset;

  @Parameter(1)
  public String expectedTimeZoneId;

  @Test
  public void testGetTimeZoneId() {
    assertThat(TimeZoneUtils.getTimeZoneId(offset), is(expectedTimeZoneId));
  }

  @Test
  public void testValidTimeZone() {
    String timeZoneId = TimeZoneUtils.getTimeZoneId(offset);
    TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);
    assertThat((long) timeZone.getRawOffset(), is(offset));
  }
}
