/*
 * The MIT License
 *
 * Copyright 2018 CloudBees, Inc.
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

package hudson.plugins.timestamper.pipeline;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.MarkupText;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleAnnotatorFactory;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.format.TimestampFormat;
import hudson.plugins.timestamper.format.TimestampFormatProvider;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/** Interprets marks added by {@link GlobalDecorator}. */
public final class GlobalAnnotator extends ConsoleAnnotator<Object> {

    private static final long serialVersionUID = 1;

    private static final Logger LOGGER = Logger.getLogger(GlobalAnnotator.class.getName());

    @Override
    public ConsoleAnnotator<Object> annotate(@NonNull Object context, @NonNull MarkupText text) {
        Run<?, ?> build;
        if (context instanceof Run) {
            build = (Run<?, ?>) context;
        } else if (context instanceof FlowNode) {
            FlowExecutionOwner owner = ((FlowNode) context).getExecution().getOwner();
            if (owner == null) {
                return null;
            }
            Queue.Executable executable;
            try {
                executable = owner.getExecutable();
            } catch (IOException x) {
                LOGGER.log(Level.FINE, null, x);
                return null;
            }
            if (executable instanceof Run) {
                build = (Run<?, ?>) executable;
            } else {
                return null;
            }
        } else {
            return null;
        }
        long buildStartTime = build.getStartTimeInMillis();
        parseTimestamp(text.getText(), buildStartTime).ifPresent(timestamp -> {
            TimestampFormat format = TimestampFormatProvider.get();
            format.markup(text, timestamp);
            text.addMarkup(0, 26, "<span style=\"display: none\">", "</span>");
        });
        return this;
    }

    /** Parse this line for a timestamp if such a timestamp is present. */
    @Restricted(NoExternalUse.class)
    public static Optional<Timestamp> parseTimestamp(String text, long buildStartTime) {
        if (text.startsWith("[")) {
            int end = text.indexOf(']');
            if (end != -1) {
                try {
                    long millisSinceEpoch = ZonedDateTime.parse(text.substring(1, end), GlobalDecorator.UTC_MILLIS)
                            .toInstant()
                            .toEpochMilli();
                    // Alternately: Instant.parse(text.substring(1, end)).toEpochMilli()
                    Timestamp timestamp = new Timestamp(millisSinceEpoch - buildStartTime, millisSinceEpoch);
                    return Optional.of(timestamp);
                } catch (DateTimeParseException x) {
                    // something else, ignore
                }
            }
        }
        return Optional.empty();
    }

    @Extension
    public static final class Factory extends ConsoleAnnotatorFactory<Object> {

        @Override
        public ConsoleAnnotator<Object> newInstance(Object context) {
            if (context instanceof Run && context instanceof FlowExecutionOwner.Executable) {
                return new GlobalAnnotator();
            } else if (context instanceof FlowNode) {
                return new GlobalAnnotator();
            }
            // Note that prior to 2.145, we actually get FlowNode.class here rather than a FlowNode,
            // so there is no per-step annotation.
            return null;
        }
    }
}
