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
package hudson.plugins.timestamper.action;

import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.io.TimestampsReader;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.StaplerRequest;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

/**
 * Output a page of time-stamps on behalf of {@link TimestampsAction}.
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
class TimestampsActionOutput {

  private static final Logger LOGGER = Logger
      .getLogger(TimestampsActionOutput.class.getName());

  /**
   * Write a page of time-stamps.
   * 
   * @param timestampsReader
   * @param writer
   * @param request
   * @throws IOException
   */
  void write(TimestampsReader timestampsReader, PrintWriter writer,
      StaplerRequest request) throws IOException {

    int precision = getPrecision(request);

    while (true) {
      Optional<Timestamp> timestamp = timestampsReader.read();
      if (!timestamp.isPresent()) {
        break;
      }
      writer.write(formatTimestamp(timestamp.get(), precision));
    }
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

  private String formatTimestamp(Timestamp timestamp, int precision) {
    long seconds = timestamp.elapsedMillis / 1000;
    if (precision == 0) {
      return String.valueOf(seconds) + "\n";
    }
    long millis = timestamp.elapsedMillis % 1000;
    String fractional = String.format("%03d", millis);
    if (precision <= 3) {
      fractional = fractional.substring(0, precision);
    } else {
      fractional += Strings.repeat("0", precision - 3);
    }
    return String.valueOf(seconds) + "." + fractional + "\n";
  }
}
