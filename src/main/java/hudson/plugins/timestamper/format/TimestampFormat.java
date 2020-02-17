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

import com.google.common.base.Function;
import hudson.MarkupText;
import hudson.plugins.timestamper.Timestamp;

/**
 * Format for displaying time-stamps.
 *
 * @author Steven G. Brown
 */
public abstract class TimestampFormat implements Function<Timestamp, String> {

  /**
   * Format the given time-stamp as a string.
   *
   * @return the formatted time-stamp
   */
  @Override
  public abstract String apply(Timestamp timestamp);

  /**
   * Format the given time-stamp and add it to the mark-up text.
   *
   * @param text the mark-up text
   * @param timestamp the time-stamp to format
   */
  public void markup(MarkupText text, Timestamp timestamp) {
    String timestampString = apply(timestamp);
    // Wrap the time-stamp in a span element, which is used to detect the
    // time-stamp when inspecting the page with Javascript.
    String markup = "<span class=\"timestamp\">" + timestampString + "</span>";
    // Add as end tag, which will be inserted prior to tags added by other
    // console notes (e.g. AntTargetNote).
    text.addMarkup(0, 0, "", markup);
  }

  /**
   * Get the URL for displaying the plain text console and time-stamps in this format.
   *
   * @return the plain text URL
   */
  public abstract String getPlainTextUrl();
}
