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

import java.util.regex.Pattern;

/**
 * Utility class for parsing time-stamp format strings.
 * 
 * @author Steven G. Brown
 */
class FormatStringUtils {

  private static final Pattern START_TAG_PATTERN = Pattern.compile(
      "\\<\\p{Alpha}+.*?\\>", Pattern.CASE_INSENSITIVE);

  private static final Pattern END_TAG_PATTERN = Pattern.compile(
      "\\</\\p{Alpha}+\\>", Pattern.CASE_INSENSITIVE);

  /**
   * Strip HTML tags from the given string.
   * <p>
   * This may strip too much from the string when it contains angle bracket
   * characters which are not part of an HTML tag. It seems unlikely that this
   * will occur for time-stamp format strings.
   * 
   * @param input
   *          the input string
   * @return the string without HTML tags
   */
  static String stripHtmlTags(String input) {
    input = START_TAG_PATTERN.matcher(input).replaceAll("");
    input = END_TAG_PATTERN.matcher(input).replaceAll("");
    return input;
  }

  private static final String QUOTED_WHITESPACE = "\\s*'\\s*'\\s*";

  /**
   * Trim the given string, including single-quoted text.
   * 
   * @param input
   *          the input string
   * @return the trimmed string
   */
  static String trim(String input) {
    input = input.replaceFirst("^" + QUOTED_WHITESPACE, "");
    input = input.replaceFirst(QUOTED_WHITESPACE + "$", "");
    return input.trim();
  }
}
