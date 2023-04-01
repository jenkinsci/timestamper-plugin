/*
 * The MIT License
 *
 * Copyright (c) 2010 Steven G. Brown
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

import hudson.MarkupText;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleNote;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.plugins.timestamper.action.TimestampsAction;
import hudson.plugins.timestamper.format.TimestampFormat;
import hudson.plugins.timestamper.format.TimestampFormatProvider;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Time-stamp console note.
 *
 * <p>These are inserted into the log file when:
 *
 * <ul>
 *   <li>Running a Pipeline build or any other build which does not extend {@link AbstractBuild}
 *       prior to version 1.9 OR
 *   <li>Running a Freestyle build or any other build which extends {@link AbstractBuild} prior to
 *       version 1.4 OR
 *   <li>Running a Freestyle build or any other build which extends {@link AbstractBuild} and the
 *       system property is set: ({@link #getSystemProperty()}). This is intended to support scripts
 *       that were written prior to version 1.4 to parse the log files. New scripts should query the
 *       {@code /timestamps} URL instead (see {@link TimestampsAction}).
 * </ul>
 *
 * <p>Otherwise, the time-stamps are stored in a separate file, which allows a more compact format
 * to be used and avoids filling the log files with encoded console notes.
 *
 * @author Steven G. Brown
 */
public final class TimestampNote extends ConsoleNote<Object> {

    /** Serialization UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Get the system property which will cause these console notes to be inserted into the console
     * log file.
     *
     * @return the system property
     */
    public static String getSystemProperty() {
        return "timestamper-consolenotes";
    }

    /** @return whether time-stamp notes apply to that type of build */
    public static boolean useTimestampNotes(Class<?> buildClass) {
        return !AbstractBuild.class.isAssignableFrom(buildClass);
    }

    /**
     * The elapsed time in milliseconds since the start of the build.
     *
     * @since 1.7.4
     */
    private final Long elapsedMillis;

    /** Milliseconds since the epoch. */
    private final long millisSinceEpoch;

    /**
     * Create a new {@link TimestampNote}.
     *
     * @param elapsedMillis the elapsed time in milliseconds since the start of the build
     * @param millisSinceEpoch milliseconds since the epoch
     */
    public TimestampNote(long elapsedMillis, long millisSinceEpoch) {
        this.elapsedMillis = elapsedMillis;
        this.millisSinceEpoch = millisSinceEpoch;
    }

    /**
     * Get the time-stamp recorded by this console note.
     *
     * @param context the object that owns the console output in question
     * @return the time-stamp
     */
    public Timestamp getTimestamp(Object context) {
        if (elapsedMillis == null && context instanceof Run<?, ?>) {
            // The elapsed time can be determined by using the build start time
            Run<?, ?> build = (Run<?, ?>) context;
            long buildStartTime = build.getStartTimeInMillis();
            return new Timestamp(millisSinceEpoch - buildStartTime, millisSinceEpoch);
        }
        // Use the elapsed time recorded in this console note, if known
        return new Timestamp(elapsedMillis, millisSinceEpoch);
    }

    /** {@inheritDoc} */
    @Override
    public ConsoleAnnotator<Object> annotate(Object context, MarkupText text, int charPos) {
        TimestampFormat format = TimestampFormatProvider.get();
        Timestamp timestamp = getTimestamp(context);
        format.markup(text, timestamp);
        if (!(context instanceof Run<?, ?>)) {
            text.addMarkup(0, "<style>.timestamper-plain-text {visibility: hidden;}</style>");
        }
        return null; // each time-stamp note affects one line only
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("elapsedMillis", elapsedMillis)
                .append("millisSinceEpoch", millisSinceEpoch)
                .toString();
    }
}
