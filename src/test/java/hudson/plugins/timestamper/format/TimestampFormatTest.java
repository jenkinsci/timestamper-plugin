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
import static org.mockito.Mockito.mock;
import hudson.MarkupText;
import hudson.console.ConsoleNote;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.tasks._ant.AntTargetNote;

import org.junit.Test;

/**
 * Unit test for the {@link TimestampFormat} class.
 * 
 * @author Steven G. Brown
 */
public class TimestampFormatTest {

  private String timestampString = "TIMESTAMP";

  private String prefix = "<span class=\"timestamp\">" + timestampString
      + "</span>";

  /**
   */
  @Test
  public void testMarkup() {
    assertThat(markup("line").toString(true), is(prefix + "line"));
  }

  /**
   */
  @Test
  public void testMarkupThenAntTargetNote() {
    assertThat(annotate(markup("target:"), new AntTargetNote()).toString(true),
        is(prefix + "<b class=ant-target>target</b>:"));
  }

  /**
   */
  @Test
  public void testAntTargetNoteThenMarkup() {
    assertThat(markup(annotate("target:", new AntTargetNote())).toString(true),
        is(prefix + "<b class=ant-target>target</b>:"));
  }

  private MarkupText markup(String text) {
    return markup(new MarkupText(text));
  }

  private MarkupText markup(MarkupText markupText) {
    TimestampFormat format = new TimestampFormat() {

      @Override
      public String apply(Timestamp timestamp) {
        return timestampString;
      }
    };
    format.markup(markupText, new Timestamp(123, 42000));
    return markupText;
  }

  @SuppressWarnings("rawtypes")
  private MarkupText annotate(String text, ConsoleNote... notes) {
    MarkupText markupText = new MarkupText(text);
    return annotate(markupText, notes);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private MarkupText annotate(MarkupText markupText, ConsoleNote... notes) {
    Object context = mock(Run.class);
    for (ConsoleNote note : notes) {
      note.annotate(context, markupText, 0);
    }
    return markupText;
  }
}
