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

import hudson.model.Action;
import hudson.model.Run;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Action which serves a page of time-stamps. The format of this page will not change, so it can be
 * safely parsed by scripts.
 *
 * <p>See {@link TimestampsActionOutput} for the format of this page.
 *
 * @author Steven G. Brown
 */
public final class TimestampsAction implements Action {

    private static final Logger LOGGER = Logger.getLogger(TimestampsAction.class.getName());

    /** The build to inspect. */
    private final Run<?, ?> build;

    /**
     * Create a {@link TimestampsAction} for the given build.
     *
     * @param build the build to inspect
     */
    TimestampsAction(Run<?, ?> build) {
        this.build = Objects.requireNonNull(build);
    }

    /** {@inheritDoc} */
    @Override
    public String getIconFileName() {
        return null; // do not display this action
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
        return null; // do not display this action
    }

    /** {@inheritDoc} */
    @Override
    public String getUrlName() {
        return "timestamps";
    }

    /** Serve a page at this URL. */
    @SuppressWarnings({"lgtm[jenkins/csrf]", "lgtm[jenkins/no-permission-check]"})
    public void doIndex(StaplerRequest request, StaplerResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");

        PrintWriter writer = response.getWriter();

        try {
            // throws RuntimeException for invalid query
            TimestampsActionQuery query = TimestampsActionQuery.create(request.getQueryString());

            try (BufferedReader reader = TimestampsActionOutput.open(build, query)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.println(line);
                }
            }

        } catch (RuntimeException | IOException e) {
            String urlWithQueryString = request.getRequestURLWithQueryString().toString();
            writer.println(urlWithQueryString);
            String exceptionMessage = e.getMessage() == null ? "" : e.getMessage();
            writer.println(e.getClass().getSimpleName() + (exceptionMessage.isEmpty() ? "" : ": " + exceptionMessage));
            LOGGER.log(Level.WARNING, urlWithQueryString, e);
        } finally {
            writer.flush();
        }
    }
}
