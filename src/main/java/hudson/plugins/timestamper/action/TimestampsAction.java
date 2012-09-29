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

import hudson.console.ConsoleNote;
import hudson.model.Action;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.TimestampNote;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Action which serves a page of timestamps. The format of this page will not
 * change, so it can be safely parsed by scripts.
 * <p>
 * Each line contains the elapsed time in seconds since the start of the build
 * for the equivalent line in the console log. The elapsed time will include
 * three places after the decimal point.
 * 
 * @author Steven G. Brown
 * @since 1.3.2
 */
public final class TimestampsAction implements Action {

  /**
   * The build to inspect.
   */
  private final Run<?, ?> build;

  /**
   * Create a {@link TimestampsAction} for the given build.
   * 
   * @param build
   *          the build to inspect
   */
  TimestampsAction(Run<?, ?> build) {
    this.build = build;
  }

  /**
   * {@inheritDoc}
   */
  public String getIconFileName() {
    // Do not display this action.
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public String getDisplayName() {
    // Do not display this action.
    return null;
  }

  /**
   * {@inheritDoc}
   */
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
    writeConsoleNotes(response.getWriter());
    response.getWriter().flush();
  }

  private void writeConsoleNotes(PrintWriter writer) throws IOException {
    DataInputStream dataInputStream = new DataInputStream(
        new BufferedInputStream(build.getLogInputStream()));
    try {
      while (true) {
        dataInputStream.mark(1);
        int currentByte = dataInputStream.read();
        if (currentByte == -1) {
          return;
        }
        if (currentByte == ConsoleNote.PREAMBLE[0]) {
          dataInputStream.reset();
          ConsoleNote<?> consoleNote;
          try {
            consoleNote = ConsoleNote.readFrom(dataInputStream);
          } catch (ClassNotFoundException ex) {
            // Unknown console note. Ignore.
            continue;
          }
          if (consoleNote instanceof TimestampNote) {
            TimestampNote timestampNote = (TimestampNote) consoleNote;
            writeTimestamp(writer, timestampNote.getTimestamp(build));
          }
        }
      }
    } finally {
      IOUtils.closeQuietly(dataInputStream);
    }
  }

  private void writeTimestamp(PrintWriter writer, Timestamp timestamp) {
    long seconds = timestamp.elapsedMillis / 1000;
    long fractional = timestamp.elapsedMillis % 1000;
    String line = String.format("%d.%03d\n", Long.valueOf(seconds),
        Long.valueOf(fractional));
    writer.write(line);
  }
}
