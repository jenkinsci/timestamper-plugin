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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.io.TimestampsReader;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.OngoingStubbing;

import com.google.common.base.Optional;

/**
 * Unit test for the {@link TimestampsActionOutput} class.
 * 
 * @author Steven G. Brown
 */
@RunWith(MockitoJUnitRunner.class)
public class TimestampsActionOutputTest {

  @Mock
  private TimestampsReader reader;

  private TimestampsActionOutput output;

  private TimeZone systemDefaultTimeZone;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    systemDefaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("GMT"));

    List<Timestamp> timestamps = new ArrayList<Timestamp>();
    timestamps.add(new Timestamp(0, TimeUnit.SECONDS.toMillis(1)));
    timestamps.add(new Timestamp(1, TimeUnit.MINUTES.toMillis(1)));
    timestamps.add(new Timestamp(10, TimeUnit.HOURS.toMillis(1)));
    timestamps.add(new Timestamp(100, TimeUnit.DAYS.toMillis(1)));
    timestamps.add(new Timestamp(1000, TimeUnit.DAYS.toMillis(2)));
    timestamps.add(new Timestamp(10000, TimeUnit.DAYS.toMillis(3)));

    OngoingStubbing<Optional<Timestamp>> stubbing = when(reader.read());
    for (Timestamp timestamp : timestamps) {
      stubbing = stubbing.thenReturn(Optional.of(timestamp));
    }
    stubbing.thenReturn(Optional.<Timestamp> absent());

    output = new TimestampsActionOutput();
  }

  /**
   */
  @After
  public void tearDown() {
    TimeZone.setDefault(systemDefaultTimeZone);
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_emptyQueryString() throws Exception {
    output.setQuery("");
    assertThat(generate(), is("0.000\n" + "0.001\n" + "0.010\n" + "0.100\n"
        + "1.000\n" + "10.000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_nullQueryString() throws Exception {
    output.setQuery(null);
    assertThat(generate(), is("0.000\n" + "0.001\n" + "0.010\n" + "0.100\n"
        + "1.000\n" + "10.000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_zeroPrecision() throws Exception {
    output.setQuery("precision=0");
    assertThat(generate(), is("0\n" + "0\n" + "0\n" + "0\n" + "1\n" + "10\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_zeroPrecisionAndOnePrecision() throws Exception {
    output.setQuery("precision=0&precision=1");
    assertThat(generate(), is("0\n" + "0\n" + "0\n" + "0\n" + "1\n" + "10\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_secondsPrecision() throws Exception {
    output.setQuery("precision=seconds");
    assertThat(generate(), is("0\n" + "0\n" + "0\n" + "0\n" + "1\n" + "10\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_onePrecision() throws Exception {
    output.setQuery("precision=1");
    assertThat(generate(), is("0.0\n" + "0.0\n" + "0.0\n" + "0.1\n" + "1.0\n"
        + "10.0\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_twoPrecision() throws Exception {
    output.setQuery("precision=2");
    assertThat(generate(), is("0.00\n" + "0.00\n" + "0.01\n" + "0.10\n"
        + "1.00\n" + "10.00\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_threePrecision() throws Exception {
    output.setQuery("precision=3");
    assertThat(generate(), is("0.000\n" + "0.001\n" + "0.010\n" + "0.100\n"
        + "1.000\n" + "10.000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_millisecondsPrecision() throws Exception {
    output.setQuery("precision=milliseconds");
    assertThat(generate(), is("0.000\n" + "0.001\n" + "0.010\n" + "0.100\n"
        + "1.000\n" + "10.000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_sixPrecision() throws Exception {
    output.setQuery("precision=6");
    assertThat(generate(), is("0.000000\n" + "0.001000\n" + "0.010000\n"
        + "0.100000\n" + "1.000000\n" + "10.000000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_microsecondsPrecision() throws Exception {
    output.setQuery("precision=microseconds");
    assertThat(generate(), is("0.000000\n" + "0.001000\n" + "0.010000\n"
        + "0.100000\n" + "1.000000\n" + "10.000000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_nanosecondsPrecision() throws Exception {
    output.setQuery("precision=nanoseconds");
    assertThat(generate(), is("0.000000000\n" + "0.001000000\n"
        + "0.010000000\n" + "0.100000000\n" + "1.000000000\n"
        + "10.000000000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_emptyPrecision() throws Exception {
    output.setQuery("precision=");
    assertThat(generate(), is("0.000\n" + "0.001\n" + "0.010\n" + "0.100\n"
        + "1.000\n" + "10.000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_negativePrecision() throws Exception {
    output.setQuery("precision=-1");
    assertThat(generate(), is("0.000\n" + "0.001\n" + "0.010\n" + "0.100\n"
        + "1.000\n" + "10.000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_invalidPrecision() throws Exception {
    output.setQuery("precision=invalid");
    assertThat(generate(), is("0.000\n" + "0.001\n" + "0.010\n" + "0.100\n"
        + "1.000\n" + "10.000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_time() throws Exception {
    output.setQuery("time=dd:HH:mm:ss");
    assertThat(generate(),
        is("01:00:00:01\n" + "01:00:01:00\n" + "01:01:00:00\n"
            + "02:00:00:00\n" + "03:00:00:00\n" + "04:00:00:00\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_elapsed() throws Exception {
    output.setQuery("elapsed=s.SSS");
    assertThat(generate(), is("0.000\n" + "0.001\n" + "0.010\n" + "0.100\n"
        + "1.000\n" + "10.000\n"));
  }

  /**
   * @throws Exception
   */
  @Test
  public void testWrite_noTimestamps() throws Exception {
    when(reader.read()).thenReturn(Optional.<Timestamp> absent());
    output.setQuery("");
    assertThat(generate(), is(""));
  }

  private String generate() throws Exception {
    StringBuilder sb = new StringBuilder();
    while (true) {
      Optional<String> line = output.nextLine(reader);
      if (!line.isPresent()) {
        return sb.toString();
      }
      sb.append(line.get());
      sb.append("\n");
    }
  }
}
