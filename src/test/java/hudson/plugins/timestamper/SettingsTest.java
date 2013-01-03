/*
 * The MIT License
 * 
 * Copyright (c) 2013 Steven G. Brown
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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for the {@link Settings} class.
 * 
 * @author Steven G. Brown
 */
public class SettingsTest {

  private Settings settings;

  /**
   */
  @Before
  public void setUp() {
    settings = new Settings("system", "elapsed");
  }

  /**
   */
  @Test
  public void getSystemTimeFormat() {
    assertThat(settings.getSystemTimeFormat(), is("system"));
  }

  /**
   */
  @Test
  public void serializeThenGetSystemTimeFormat() {
    settings = serialiseThenDeserialise(settings);
    assertThat(settings.getSystemTimeFormat(), is("system"));
  }

  /**
   */
  @Test
  public void getElapsedTimeFormat() {
    assertThat(settings.getElapsedTimeFormat(), is("elapsed"));
  }

  /**
   */
  @Test
  public void serializeThenGetElapsedTimeFormat() {
    settings = serialiseThenDeserialise(settings);
    assertThat(settings.getElapsedTimeFormat(), is("elapsed"));
  }

  private Settings serialiseThenDeserialise(Settings settings) {
    return (Settings) SerializationUtils.clone(settings);
  }
}
