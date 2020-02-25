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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import hudson.plugins.timestamper.Timestamp;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;

/**
 * Unit test for the {@link ElapsedTimestampFormat} class.
 *
 * @author Steven G. Brown
 */
public class ElapsedTimestampFormatTest {

  @Test
  public void testApply() {
    String elapsedTimeFormat = "ss.S";
    Timestamp timestamp = new Timestamp(123, 42000);
    assertThat(new ElapsedTimestampFormat(elapsedTimeFormat).apply(timestamp), is("00.123"));
  }

  @Test
  public void testGetPlainTextUrl() {
    ElapsedTimestampFormat format = new ElapsedTimestampFormat("'<b>'HH:mm:ss.S'</b> '");
    assertThat(format.getPlainTextUrl(), is("timestamps/?elapsed=HH:mm:ss.S&appendLog"));
  }

  @Test
  public void testGetPlainTextUrl_excessWhitespace() {
    ElapsedTimestampFormat format = new ElapsedTimestampFormat(" ' <b> ' HH:mm:ss.S ' </b> ' ");
    assertThat(format.getPlainTextUrl(), is("timestamps/?elapsed=HH:mm:ss.S&appendLog"));
  }

  @Test
  public void testEqualsAndHashCode() {
    EqualsVerifier.forClass(ElapsedTimestampFormat.class).suppress(Warning.NULL_FIELDS).verify();
  }
}
