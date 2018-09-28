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

import hudson.Extension;
import hudson.MarkupText;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleAnnotatorFactory;
import hudson.model.Run;
import hudson.plugins.timestamper.Timestamp;
import hudson.plugins.timestamper.format.TimestampFormat;
import hudson.plugins.timestamper.format.TimestampFormatProvider;
import java.text.ParsePosition;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;

/**
 * Interprets marks added by {@link GlobalDecorator}.
 */
public final class GlobalAnnotator extends ConsoleAnnotator</* TODO pending https://github.com/jenkinsci/jenkins/pull/3662 */Object> {

    private static final long serialVersionUID = 1;

    @Override
    public ConsoleAnnotator<Object> annotate(Object context, MarkupText text) {
        if (!(context instanceof Run)) {
            return null;
        }
        Run<?, ?> build = (Run<?, ?>) context;
        long buildStartTime = build.getStartTimeInMillis();
        String html = text.toString(true);
        int start;
        if (html.startsWith("<span class=\"pipeline-new-node\" ")) { // cf. LogStorage.startStep
            start = html.indexOf('>') + 1;
        } else {
            start = 0;
        }
        if (html.startsWith("[", start)) {
            int end = html.indexOf(']', start);
            if (end != -1) {
                long millisSinceEpoch = GlobalDecorator.UTC_MILLIS.get().parse(html, new ParsePosition(start + 1)).getTime();
                Timestamp timestamp = new Timestamp(millisSinceEpoch - buildStartTime, millisSinceEpoch);
                TimestampFormat format = TimestampFormatProvider.get();
                format.markup(text, timestamp);
                text.addMarkup(0, 26, "<span style=\"display: none\">", "</span>");
            }
        }
        return this;
    }

    @Extension
    public static final class Factory extends ConsoleAnnotatorFactory<Object> {

        @Override
        public ConsoleAnnotator<Object> newInstance(Object context) {
            if (context instanceof Run && context instanceof FlowExecutionOwner.Executable) {
                return new GlobalAnnotator();
            }
            return null;
        }
    }

}
