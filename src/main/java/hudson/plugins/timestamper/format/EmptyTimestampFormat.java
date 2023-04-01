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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.plugins.timestamper.Timestamp;

/**
 * Converts a time-stamp to the empty time format.
 *
 * @author Steven G. Brown
 */
public final class EmptyTimestampFormat extends TimestampFormat {

    public static final EmptyTimestampFormat INSTANCE = new EmptyTimestampFormat();

    private EmptyTimestampFormat() {}

    /** {@inheritDoc} */
    @Override
    public String apply(@NonNull Timestamp timestamp) {
        return "";
    }

    @Override
    public void validate() {}

    /** {@inheritDoc} */
    @Override
    public String getPlainTextUrl() {
        return "consoleText";
    }
}
