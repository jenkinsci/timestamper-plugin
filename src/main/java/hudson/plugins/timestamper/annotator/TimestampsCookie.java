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

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Possible values for the time-stamps cookie.
 * 
 * @author Steven G. Brown
 * @since 1.5
 */
public enum TimestampsCookie {

  /**
   * Display the system clock time (default).
   */
  SYSTEM,

  /**
   * Display the elapsed time since the start of the build.
   */
  ELAPSED,

  /**
   * Do not display any time-stamps.
   */
  NONE;

  /**
   * Get the value of the cookie from the given HTTP request.
   * 
   * @param request
   *          the HTTP request
   * @return the cookie
   */
  public static TimestampsCookie get(HttpServletRequest request) {
    for (Cookie cookie : request.getCookies()) {
      if ("timestamper".equals(cookie.getName())) {
        try {
          return TimestampsCookie.valueOf(cookie.getValue().toUpperCase());
        } catch (IllegalArgumentException ex) {
          // value not recognised, fall back to the default
        }
      }
    }
    // default
    return TimestampsCookie.SYSTEM;
  }
}
