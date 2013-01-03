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

import java.io.Serializable;

import javax.annotation.concurrent.Immutable;

/**
 * Timestamper plug-in settings.
 * 
 * @author Steven G. Brown
 * @since 1.4
 */
@Immutable
public final class Settings implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String systemTimeFormat;

  private final String elapsedTimeFormat;

  /**
   * Create a new {@link Settings}.
   * 
   * @param systemTimeFormat
   * @param elapsedTimeFormat
   */
  public Settings(String systemTimeFormat, String elapsedTimeFormat) {
    this.systemTimeFormat = systemTimeFormat;
    this.elapsedTimeFormat = elapsedTimeFormat;
  }

  /**
   * Get the format for displaying the system clock time.
   * 
   * @return the system clock time format
   */
  public String getSystemTimeFormat() {
    return systemTimeFormat;
  }

  /**
   * Get the format for displaying the elapsed time.
   * 
   * @return the elapsed time format
   */
  public String getElapsedTimeFormat() {
    return elapsedTimeFormat;
  }
}
