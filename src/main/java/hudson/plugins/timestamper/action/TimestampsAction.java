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
package hudson.plugins.timestamper.action;

import static com.google.common.base.Preconditions.checkNotNull;
import hudson.model.Action;
import hudson.model.Run;
import hudson.plugins.timestamper.io.TimestampNotesReader;
import hudson.plugins.timestamper.io.TimestamperPaths;
import hudson.plugins.timestamper.io.TimestampsFileReader;
import hudson.plugins.timestamper.io.TimestampsReader;

import java.io.IOException;
import java.io.PrintWriter;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Action which serves a page of time-stamps. The format of this page will not
 * change, so it can be safely parsed by scripts.
 * <p>
 * See {@link TimestampsActionOutput} for the format of this page.
 * 
 * @author Steven G. Brown
 */
public final class TimestampsAction implements Action {

  /**
   * The build to inspect.
   */
  private final Run<?, ?> build;

  /**
   * Generates the page of time-stamps.
   */
  private final TimestampsActionOutput output;

  /**
   * Create a {@link TimestampsAction} for the given build.
   * 
   * @param build
   *          the build to inspect
   * @param output
   *          generates the page of time-stamps
   */
  TimestampsAction(Run<?, ?> build, TimestampsActionOutput output) {
    this.build = checkNotNull(build);
    this.output = checkNotNull(output);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getIconFileName() {
    return null; // do not display this action
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDisplayName() {
    return null; // do not display this action
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getUrlName() {
    return "timestamps";
  }

  /**
   * Serve a page at this URL.
   * 
   * @param request
   * @param response
   * @throws IOException
   */
  public void doIndex(StaplerRequest request, StaplerResponse response)
      throws IOException {
    response.setContentType("text/plain;charset=UTF-8");

    TimestampsReader timestampsReader;
    if (TimestamperPaths.timestampsFile(build).isFile()) {
      timestampsReader = new TimestampsFileReader(build);
    } else {
      timestampsReader = new TimestampNotesReader(build);
    }

    try {
      PrintWriter writer = response.getWriter();
      output.write(timestampsReader, writer, request.getQueryString());
      writer.flush();
    } finally {
      timestampsReader.close();
    }
  }
}
