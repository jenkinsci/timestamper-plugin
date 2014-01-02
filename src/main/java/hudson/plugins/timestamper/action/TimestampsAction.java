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
import hudson.console.ConsoleNote;
import hudson.model.Action;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.TimestampNote;
import hudson.plugins.timestamper.io.TimestampsReader;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.google.common.base.Strings;
import com.google.common.io.Closeables;

/**
 * Action which serves a page of timestamps. The format of this page will not
 * change, so it can be safely parsed by scripts.
 * <p>
 * Each line contains the elapsed time in seconds since the start of the build
 * for the equivalent line in the console log.
 * <p>
 * By default, the elapsed time will include three places after the decimal
 * point. The number of places after the decimal point can be configured by the
 * "precision" query parameter, which accepts a number of decimal places or the
 * values "seconds" or "milliseconds".
 * 
 * @author Steven G. Brown
 */
public final class TimestampsAction implements Action {

  private static final Logger LOGGER = Logger.getLogger(TimestampsAction.class
      .getName());

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
    this.build = checkNotNull(build);
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
    int precision = getPrecision(request);
    response.setContentType("text/plain;charset=UTF-8");
    PrintWriter writer = response.getWriter();

    TimestampsReader reader = new TimestampsReader(build);
    boolean timestampsFound = false;
    while (true) {
      List<Timestamp> timestamps = reader.read(1000);
      if (timestamps.isEmpty()) {
        break;
      }
      timestampsFound = true;
      for (Timestamp timestamp : timestamps) {
        writer.write(formatTimestamp(timestamp, precision));
      }
    }

    if (!timestampsFound) {
      writeConsoleNotes(writer, precision);
    }

    response.getWriter().flush();
  }

  private int getPrecision(StaplerRequest request) {
    String precision = request.getParameter("precision");
    if ("seconds".equalsIgnoreCase(precision)) {
      return 0;
    }
    if ("milliseconds".equalsIgnoreCase(precision)) {
      return 3;
    }
    if ("microseconds".equalsIgnoreCase(precision)) {
      return 6;
    }
    if ("nanoseconds".equalsIgnoreCase(precision)) {
      return 9;
    }
    if (!Strings.isNullOrEmpty(precision)) {
      try {
        int intPrecision = Integer.parseInt(precision);
        if (intPrecision < 0) {
          logUnrecognisedPrecision(precision);
        } else {
          return intPrecision;
        }
      } catch (NumberFormatException ex) {
        logUnrecognisedPrecision(precision);
      }
    }
    // Default precision.
    return 3;
  }

  private void logUnrecognisedPrecision(String precision) {
    LOGGER.log(Level.WARNING, "Unrecognised precision: " + precision);
  }

  private void writeConsoleNotes(PrintWriter writer, int precision)
      throws IOException {
    DataInputStream dataInputStream = new DataInputStream(
        new BufferedInputStream(build.getLogInputStream()));
    boolean threw = true;
    try {
      while (true) {
        dataInputStream.mark(1);
        int currentByte = dataInputStream.read();
        if (currentByte == -1) {
          threw = false;
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
            Timestamp timestamp = timestampNote.getTimestamp(build);
            writer.write(formatTimestamp(timestamp, precision));
          }
        }
      }
    } finally {
      Closeables.close(dataInputStream, threw);
    }
  }

  private String formatTimestamp(Timestamp timestamp, int precision) {
    long seconds = timestamp.elapsedMillis / 1000;
    if (precision == 0) {
      return String.valueOf(seconds) + "\n";
    }
    long millis = timestamp.elapsedMillis % 1000;
    String fractional = String.format("%03d", Long.valueOf(millis));
    if (precision <= 3) {
      fractional = fractional.substring(0, precision);
    } else if (precision > 3) {
      fractional += Strings.repeat("0", precision - 3);
    }
    return String.valueOf(seconds) + "." + fractional + "\n";
  }
}
