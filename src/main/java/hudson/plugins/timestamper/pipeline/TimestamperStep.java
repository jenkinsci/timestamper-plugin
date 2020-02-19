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

import hudson.Extension;
import hudson.console.ConsoleLogFilter;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.plugins.timestamper.Messages;
import hudson.plugins.timestamper.TimestamperConfig;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nonnull;
import jenkins.YesNoMaybe;
import org.jenkinsci.plugins.workflow.log.TaskListenerDecorator;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.BodyInvoker;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

/**
 * Pipeline plug-in step for recording time-stamps.
 */
public class TimestamperStep extends Step {

  /** Constructor. */
  @DataBoundConstructor
  public TimestamperStep() {}

  @Override
  public StepExecution start(StepContext context) throws Exception {
    return new ExecutionImpl(context);
  }

  /** Execution for {@link TimestamperStep}. */
  private static class ExecutionImpl extends AbstractStepExecutionImpl {
      
    ExecutionImpl(StepContext context) {
      super(context);
    }

    private static final long serialVersionUID = 1L;

    /** {@inheritDoc} */
    @Override
    public boolean start() throws Exception {
      StepContext context = getContext();
        BodyInvoker invoker = context.newBodyInvoker().withCallback(BodyExecutionCallback.wrap(context));
        if (TimestamperConfig.get().isAllPipelines()) {
            context.get(TaskListener.class).getLogger().println("The timestamps step is unnecessary when timestamps are enabled for all Pipeline builds.");
        } else {
            invoker.withContext(TaskListenerDecorator.merge(context.get(TaskListenerDecorator.class), new GlobalDecorator()));
        }
        invoker.start();
      return false;
    }

    /** {@inheritDoc} */
    @Override
    public void stop(@Nonnull Throwable cause) throws Exception {
      getContext().onFailure(cause);
    }
  }

  /** Descriptor for {@link TimestamperStep}. */
  @Extension(dynamicLoadable = YesNoMaybe.YES, optional = true)
  public static class DescriptorImpl extends StepDescriptor {

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
     */
    public void doHelp(StaplerRequest request, StaplerResponse response) throws IOException {
      response.setContentType("text/html;charset=UTF-8");
      PrintWriter writer = response.getWriter();
      writer.println(Messages.Description());
      writer.flush();
    }
    
    @Override
    public Set<? extends Class<?>> getRequiredContext() {
      return Collections.singleton(TaskListener.class);
    }

  }

  /** @deprecated Only here for serial compatibility. */
  @Deprecated
  private static class TimestampNotesConsoleLogFilter extends ConsoleLogFilter
      implements Serializable {

    private static final long serialVersionUID = 1;


    /** {@inheritDoc} */
    @SuppressWarnings("rawtypes")
    @Override
    public OutputStream decorateLogger(AbstractBuild _ignore, OutputStream logger)
        throws IOException, InterruptedException {
      return logger;
    }
  }
}
