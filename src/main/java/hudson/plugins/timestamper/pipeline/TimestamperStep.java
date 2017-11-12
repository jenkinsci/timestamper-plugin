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
package hudson.plugins.timestamper.pipeline;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.BodyInvoker;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.plugins.timestamper.Messages;
import hudson.plugins.timestamper.TimestampNote;
import hudson.plugins.timestamper.TimestampNotesOutputStream;
import jenkins.YesNoMaybe;

/**
 * Pipeline plug-in step for recording time-stamps.
 *
 * @author Steven G. Brown
 */
public class TimestamperStep extends AbstractStepImpl {

  /** Constructor. */
  @DataBoundConstructor
  public TimestamperStep() {}

  /** Execution for {@link TimestamperStep}. */
  public static class ExecutionImpl extends AbstractStepExecutionImpl {

    private static final long serialVersionUID = 1L;

    /** {@inheritDoc} */
    @Override
    public boolean start() throws Exception {
      StepContext context = getContext();
      context
          .newBodyInvoker()
          .withContext(createConsoleLogFilter(context))
          .withCallback(BodyExecutionCallback.wrap(context))
          .start();
      return false;
    }

    private ConsoleLogFilter createConsoleLogFilter(StepContext context)
        throws IOException, InterruptedException {
      ConsoleLogFilter original = context.get(ConsoleLogFilter.class);
      Run<?, ?> build = context.get(Run.class);
      ConsoleLogFilter subsequent = new TimestampNotesConsoleLogFilter(build);
      return BodyInvoker.mergeConsoleLogFilters(original, subsequent);
    }

    /** {@inheritDoc} */
    @Override
    public void stop(@Nonnull Throwable cause) throws Exception {
      getContext().onFailure(cause);
    }
  }

  /** Descriptor for {@link TimestamperStep}. */
  @Extension(dynamicLoadable = YesNoMaybe.YES, optional = true)
  public static class DescriptorImpl extends AbstractStepDescriptorImpl {

    /** Constructor. */
    public DescriptorImpl() {
      super(ExecutionImpl.class);
    }

    /** {@inheritDoc} */
    @Override
    public String getDisplayName() {
      return Messages.Timestamps();
    }

    /** {@inheritDoc} */
    @Override
    public String getFunctionName() {
      return "timestamps";
    }

    /** {@inheritDoc} */
    @Override
    public boolean takesImplicitBlockArgument() {
      return true;
    }

    /** {@inheritDoc} */
    @Override
    public String getHelpFile() {
      return getDescriptorFullUrl() + "/help";
    }

    /**
     * Serve the help file.
     *
     * @param request
     * @param response
     * @throws IOException
     */
    public void doHelp(StaplerRequest request, StaplerResponse response) throws IOException {
      response.setContentType("text/html;charset=UTF-8");
      PrintWriter writer = response.getWriter();
      writer.println(Messages.Description());
      writer.flush();
    }
  }

  /** {@link ConsoleLogFilter} that adds a {@link TimestampNote} to each line. */
  private static class TimestampNotesConsoleLogFilter extends ConsoleLogFilter
      implements Serializable {

    private static final long serialVersionUID = 1;

    private final long startTime;

    /**
     * Create a new {@link TimestampNotesConsoleLogFilter} for the given build.
     *
     * @param build
     */
    TimestampNotesConsoleLogFilter(Run<?, ?> build) {
      this.startTime = build.getStartTimeInMillis();
    }

    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    @Override
    public OutputStream decorateLogger(AbstractBuild _ignore, OutputStream logger)
        throws IOException, InterruptedException {
      return new TimestampNotesOutputStream(logger, startTime);
    }
  }
}
