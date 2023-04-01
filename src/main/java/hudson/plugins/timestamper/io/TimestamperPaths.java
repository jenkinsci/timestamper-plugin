/*
 * The MIT License
 *
 * Copyright (c) 2014 Steven G. Brown
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

import hudson.model.Run;
import java.nio.file.Path;

/**
 * File paths used by the time-stamp readers and writers.
 *
 * @author Steven G. Brown
 */
public class TimestamperPaths {

    public static Path timestampsFile(Run<?, ?> build) {
        Path timestamperDir = timestamperDir(build);
        return timestamperDir.resolve("timestamps");
    }

    static Path timeShiftsFile(Run<?, ?> build) {
        Path timestamperDir = timestamperDir(build);
        return timestamperDir.resolve("timeshifts");
    }

    private static Path timestamperDir(Run<?, ?> build) {
        return build.getRootDir().toPath().resolve("timestamper");
    }

    private TimestamperPaths() {}
}
