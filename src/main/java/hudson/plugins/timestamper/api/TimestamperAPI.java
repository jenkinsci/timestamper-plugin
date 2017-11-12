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
package hudson.plugins.timestamper.api;

import java.io.BufferedReader;

import hudson.model.Run;
import hudson.plugins.timestamper.action.TimestampsActionOutput;
import hudson.plugins.timestamper.action.TimestampsActionQuery;

/**
 * Timestamper API intended for use by other plug-ins.
 *
 * @author Steven G. Brown
 */
public class TimestamperAPI {

  /**
   * Get access to the API.
   *
   * @return the Timestamper API
   * @since Timestamper 1.8
   */
  public static TimestamperAPI get() {
    return new TimestamperAPI();
  }

  private TimestamperAPI() {}

  /**
   * Read time-stamps for the given build. A query string can be provided in the same format as the
   * "/timestamps" URL. See the wiki for more information.
   *
   * @param build the build to inspect
   * @param query the query string
   * @return a {@link BufferedReader}
   * @since Timestamper 1.8
   */
  public BufferedReader read(Run<?, ?> build, String query) {
    return TimestampsActionOutput.open(build, TimestampsActionQuery.create(query));
  }
}
