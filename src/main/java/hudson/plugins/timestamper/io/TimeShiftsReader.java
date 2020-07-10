/*
 * The MIT License
 *
 * Copyright (c) 2013 Steven G. Brown
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
package hudson.plugins.timestamper.io;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CountingInputStream;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.model.Run;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Read the time-shifts for a build from disk.
 *
 * <p>The time-shifts files were written by earlier versions of this plug-in.
 *
 * @author Steven G. Brown
 */
class TimeShiftsReader implements Serializable {

  private static final long serialVersionUID = 1L;

  private final File timeShiftsFile;

  /**
   * Cache of the time shifts for each entry.
   *
   * <p>Transient: derived from the contents of {@link #timeShiftsFile}.
   */
  @CheckForNull
  @SuppressFBWarnings("SE_TRANSIENT_FIELD_NOT_RESTORED")
  private transient Map<Long, Long> timeShifts;

  TimeShiftsReader(Run<?, ?> build) {
    this.timeShiftsFile = TimestamperPaths.timeShiftsFile(build);
  }

  /**
   * Get the time recorded for the given time-stamp entry.
   *
   * @return the recorded number of milliseconds since the epoch, or {@link Optional#empty()} if no
   *     time shift was recorded for that time-stamp entry
   */
  Optional<Long> getTime(long timestampEntry) throws IOException {
    if (timeShifts == null) {
      timeShifts = ImmutableMap.copyOf(readTimeShifts());
    }
    return Optional.ofNullable(timeShifts.get(timestampEntry));
  }

  private Map<Long, Long> readTimeShifts() throws IOException {
    if (!timeShiftsFile.isFile()) {
      return Collections.emptyMap();
    }
    Map<Long, Long> timeShifts = new HashMap<>();
    try (CountingInputStream inputStream =
        new CountingInputStream(new BufferedInputStream(new FileInputStream(timeShiftsFile)))) {
      while (inputStream.getCount() < timeShiftsFile.length()) {
        long entry = Varint.read(inputStream);
        long shift = Varint.read(inputStream);
        timeShifts.put(entry, shift);
      }
    }
    return timeShifts;
  }
}
