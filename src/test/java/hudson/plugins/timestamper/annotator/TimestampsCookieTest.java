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
package hudson.plugins.timestamper.annotator;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for the TimestampsCookie class.
 * 
 * @author Steven G. Brown
 */
public class TimestampsCookieTest {

  private HttpServletRequest request;

  /**
   */
  @Before
  public void setUp() {
    request = mock(HttpServletRequest.class);
  }

  /**
   */
  @Test
  public void systemCookie() {
    when(request.getCookies()).thenReturn(cookie("system"));
    assertThat(TimestampsCookie.get(request), is(TimestampsCookie.SYSTEM));
  }

  /**
   */
  @Test
  public void elapsedCookie() {
    when(request.getCookies()).thenReturn(cookie("elapsed"));
    assertThat(TimestampsCookie.get(request), is(TimestampsCookie.ELAPSED));
  }

  /**
   */
  @Test
  public void noneCookie() {
    when(request.getCookies()).thenReturn(cookie("none"));
    assertThat(TimestampsCookie.get(request), is(TimestampsCookie.NONE));
  }

  /**
   */
  @Test
  public void noCookie() {
    when(request.getCookies()).thenReturn(new Cookie[] {});
    assertThat(TimestampsCookie.get(request), is(TimestampsCookie.SYSTEM));
  }

  /**
   */
  @Test
  public void unrecognisedCookie() {
    when(request.getCookies()).thenReturn(cookie("unrecognised"));
    assertThat(TimestampsCookie.get(request), is(TimestampsCookie.SYSTEM));
  }

  private Cookie[] cookie(String value) {
    return new Cookie[] { new Cookie("jenkins-timestamper", value) };
  }
}
