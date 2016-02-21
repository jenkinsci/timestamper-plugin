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

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Utility class for parsing time-stamp format strings.
 * 
 * @author Steven G. Brown
 */
class FormatStringUtils {

  /**
   * Strip HTML tags that may occur within a time-stamp format string.
   * 
   * @param input
   *          the input string
   * @return the string without HTML tags
   */
  static String stripHtmlTags(String input) {
    for (String tag : Arrays.asList("b", "i", "code", "strong", "em", "span")) {
      input = stripStartingTags(input, tag);
      input = stripEndingTags(input, tag);
    }
    return input;
  }

  private static String stripStartingTags(String input, String tag) {
    String pattern = "\\<" + Pattern.quote(tag) + ".*?\\>(?i)";
    return input.replaceAll(pattern, "");
  }

  private static String stripEndingTags(String input, String tag) {
    String pattern = "\\<\\/" + Pattern.quote(tag) + "\\>(?i)";
    return input.replaceAll(pattern, "");
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
