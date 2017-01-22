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

import org.junit.Test;

/**
 * Unit test for the {@link FormatStringUtils} class.
 * 
 * @author Steven G. Brown
 */
public class FormatStringUtilsTest {

  /**
   */
  @Test
  public void testStripTags_bold() {
    assertThat(FormatStringUtils.stripHtmlTags("1<b>2</b>3"), is("123"));
  }

  /**
   */
  @Test
  public void testStripTags_italic() {
    assertThat(FormatStringUtils.stripHtmlTags("1<i>2</i>3"), is("123"));
  }

  /**
   */
  @Test
  public void testStripTags_code() {
    assertThat(FormatStringUtils.stripHtmlTags("1<code>2</code>3"), is("123"));
  }

  /**
   */
  @Test
  public void testStripTags_strong() {
    assertThat(FormatStringUtils.stripHtmlTags("1<strong>2</strong>3"), is("123"));
  }

  /**
   */
  @Test
  public void testStripTags_em() {
    assertThat(FormatStringUtils.stripHtmlTags("1<em>2</em>3"), is("123"));
  }

  /**
   */
  @Test
  public void testStripTags_span() {
    assertThat(FormatStringUtils.stripHtmlTags("1<span style=\"color: blue;\">2</span>3"),
        is("123"));
  }

  /**
   */
  @Test
  public void testStripTags_empty() {
    assertThat(FormatStringUtils.stripHtmlTags(""), is(""));
  }

  /**
   */
  @Test
  public void testTrim_noQuotes() {
    assertThat(FormatStringUtils.trim(" 1 "), is("1"));
  }

  /**
   */
  @Test
  public void testTrim_withQuotes() {
    assertThat(FormatStringUtils.trim(" ' ' 1 ' ' "), is("1"));
  }

  /**
   */
  @Test
  public void testTrim_withQuotesInMiddle() {
    assertThat(FormatStringUtils.trim(" ' ' 1 ' ' 2 ' ' "), is("1 ' ' 2"));
  }

  /**
   */
  @Test
  public void testTrim_empty() {
    assertThat(FormatStringUtils.trim(""), is(""));
  }
}
