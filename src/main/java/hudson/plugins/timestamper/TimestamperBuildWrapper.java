/*
 * The MIT License
 * 
 * Copyright (c) 2011 Steven G. Brown
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

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.timestamper.io.TimestampsWriter;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.base.Optional;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.console.ConsoleLogFilter;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.timestamper.io.TimestamperPaths;
import java.io.File;
import java.io.Serializable;
import jenkins.tasks.SimpleBuildWrapper;

/**
 * Build wrapper that decorates the build's logger to record time-stamps as each
 * line is logged.
 * <p>
 * Note: The Configuration Slicing Plugin depends on this class.
 * 
 * @author Steven G. Brown
 */
public final class TimestamperBuildWrapper extends SimpleBuildWrapper {

  private static final Logger LOGGER = Logger
      .getLogger(TimestamperBuildWrapper.class.getName());

  /**
   * Create a new {@link TimestamperBuildWrapper}.
   */
  @DataBoundConstructor
  public TimestamperBuildWrapper() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener, EnvVars initialEnvironment) throws IOException, InterruptedException {
    // nothing to do
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConsoleLogFilter createLoggerDecorator(Run<?,?> build) {
    return new ConsoleLogFilterImpl(build);
  }

  private static class ConsoleLogFilterImpl extends ConsoleLogFilter implements Serializable {
    private static final long serialVersionUID = 1;
    private final File timestampsFile;
    private final long buildStartTime;
    private final boolean useTimestampNotes;

    ConsoleLogFilterImpl(Run<?,?> build) {
      this.timestampsFile = TimestamperPaths.timestampsFile(build);
      this.buildStartTime = build.getTimeInMillis();
      useTimestampNotes = !(build instanceof AbstractBuild) || Boolean.getBoolean(TimestampNote.getSystemProperty());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public OutputStream decorateLogger(AbstractBuild _ignore, OutputStream logger) throws IOException, InterruptedException {
      if (useTimestampNotes) {
        return new TimestampNotesOutputStream(logger);
      }
      Optional<MessageDigest> digest = Optional.absent();
      try {
        digest = Optional.of(MessageDigest.getInstance("SHA-1"));
      } catch (NoSuchAlgorithmException ex) {
        LOGGER.log(Level.WARNING, ex.getMessage(), ex);
      }
      try {
        TimestampsWriter timestampsWriter = new TimestampsWriter(timestampsFile, buildStartTime, digest);
        logger = new TimestamperOutputStream(logger, timestampsWriter);
      } catch (IOException ex) {
        LOGGER.log(Level.WARNING, ex.getMessage(), ex);
      }
      return logger;
    }
  }

  /**
   * Registers {@link TimestamperBuildWrapper} as a {@link BuildWrapper}.
   */
  @Extension
  public static final class DescriptorImpl extends BuildWrapperDescriptor {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
      return Messages.DisplayName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicable(AbstractProject<?, ?> item) {
      return true;
    }
  }
}
