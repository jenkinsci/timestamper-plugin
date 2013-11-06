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

import hudson.model.Run;
import hudson.plugins.timestamper.io.TimestampsIO;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Functions;

/**
 * Utility methods for use by the Timestamper unit tests.
 * 
 * @author Steven G. Brown
 */
public class TimestamperTestAssistant {

  /**
   * Wrap the given time-stamp string in an HTML span element.
   * 
   * @param timestampString
   *          the string to wrap
   * @return the HTML
   */
  public static String span(String timestampString) {
    return "<span class=\"timestamp\">" + timestampString + "</span>";
  }

  /**
   * Read all time-stamps for the given build.
   * 
   * @param build
   *          the build to inspect
   * @return the time-stamps
   * @throws Exception
   */
  public static List<Timestamp> readAllTimestamps(Run<?, ?> build)
      throws Exception {
    return readAllTimestamps(build, Functions.<TimestampsIO.Reader> identity());
  }

  /**
   * Read all time-stamps for the given build.
   * 
   * @param build
   *          the build
   * @param readerTransformer
   *          function that will be used to transform the time-stamps reader
   *          prior to each read operation
   * @return the time-stamps
   * @throws Exception
   */
  public static List<Timestamp> readAllTimestamps(Run<?, ?> build,
      Function<TimestampsIO.Reader, TimestampsIO.Reader> readerTransformer)
      throws Exception {
    TimestampsIO.Reader reader = new TimestampsIO.Reader(build);
    List<Timestamp> timestampsRead = new ArrayList<Timestamp>();
    for (int i = 0; i < 10000; i++) {
      reader = readerTransformer.apply(reader);
      Timestamp timestamp = reader.next();
      if (timestamp == null) {
        return timestampsRead;
      }
      timestampsRead.add(timestamp);
    }
    throw new IllegalStateException(
        "time-stamps do not appear to terminate. read so far: "
            + timestampsRead);
  }
}
