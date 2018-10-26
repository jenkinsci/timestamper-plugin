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
import hudson.console.LineTransformationOutputStream;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.plugins.timestamper.TimestamperConfig;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionOwner;
import org.jenkinsci.plugins.workflow.log.TaskListenerDecorator;

/**
 * Applies plain-text timestamp prefixes to all Pipeline log lines.
 */
public final class GlobalDecorator extends TaskListenerDecorator {
    
    private static final Logger LOGGER = Logger.getLogger(GlobalDecorator.class.getName());

    // TODO java.time is thread-safe but I got lost figuring out how to use it… https://stackoverflow.com/a/29626123/12916 + https://stackoverflow.com/a/26539985/12916
    static Supplier<DateFormat> UTC_MILLIS = () -> {
        DateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); // TODO java.time
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f;
    };

    private static final long serialVersionUID = 1;

    GlobalDecorator() {}

    @Override
    public OutputStream decorate(final OutputStream logger) throws IOException, InterruptedException {
        final DateFormat f = UTC_MILLIS.get();
        return new LineTransformationOutputStream() {
            @Override
            protected void eol(byte[] b, int len) throws IOException {
                synchronized (logger) { // typically this will be a PrintStream
                    logger.write('[');
                    logger.write(f.format(new Date()).getBytes(StandardCharsets.US_ASCII));
                    logger.write(']');
                    logger.write(' ');
                    logger.write(b, 0, len);
                }
            }
            @Override
            public void flush() throws IOException {
                logger.flush();
            }
            @Override
            public void close() throws IOException {
                super.close();
                logger.close();
            }
        };
    }

    @Extension
    public static final class Factory implements TaskListenerDecorator.Factory {

        @Override
        public TaskListenerDecorator of(FlowExecutionOwner owner) {
            if (!TimestamperConfig.get().isAllPipelines()) {
                return null;
            }
            try {
                Queue.Executable executable = owner.getExecutable();
                if (executable instanceof Run) { // we need at least getStartTimeInMillis
                    return new GlobalDecorator();
                }
            } catch (IOException x) {
                LOGGER.log(Level.WARNING, null, x);
            }
            return null;
        }

    }

}
