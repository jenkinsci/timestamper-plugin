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

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.console.LineTransformationOutputStream;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run.RunnerAbortedException;
import hudson.remoting.Channel;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.RobustReflectionConverter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.kohsuke.stapler.DataBoundConstructor;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * Build wrapper that decorates the build's launcher to (once
 * {@link #setUp(AbstractBuild, Launcher, BuildListener)} has been called)
 * insert a {@link TimestampNote} on each output line.
 * 
 * @author Steven G. Brown
 */
public final class TimestamperBuildWrapper extends BuildWrapper {

  /**
   * Map containing the {@link TimestamperLauncher} for each build.
   */
  @SuppressWarnings("unchecked")
  private transient ConcurrentMap<AbstractBuild, TimestamperLauncher> launchers;

  /**
   * Create a new {@link TimestamperBuildWrapper}.
   */
  @DataBoundConstructor
  @SuppressWarnings("unchecked")
  public TimestamperBuildWrapper() {
    launchers = new ConcurrentHashMap<AbstractBuild, TimestamperLauncher>();
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Environment setUp(AbstractBuild build, Launcher launcher,
      BuildListener listener) throws IOException, InterruptedException {
    TimestamperLauncher timestamperLauncher = launchers.get(build);
    if (timestamperLauncher != null) {
      timestamperLauncher.startInsertingTimestamps();
    }
    return new Environment() {
      @Override
      public boolean tearDown(AbstractBuild tearDownBuild,
          BuildListener tearDownListener) throws IOException,
          InterruptedException {
        launchers.remove(tearDownBuild);
        return true;
      }
    };
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  @Override
  public Launcher decorateLauncher(AbstractBuild build, Launcher launcher,
      BuildListener listener) throws IOException, InterruptedException,
      RunnerAbortedException {
    TimestamperLauncher timestamperLauncher = new TimestamperLauncher(launcher);
    launchers.put(build, timestamperLauncher);
    return timestamperLauncher;
  }

  /**
   * Delegating launcher that, once {@link #startInsertingTimestamps()} has been
   * called, replaces the STDOUT {@link OutputStream} with a
   * {@link TimestamperOutputStream}.
   */
  private static class TimestamperLauncher extends Launcher {

    /**
     * The delegate launcher.
     */
    private final Launcher delegate;

    /**
     * Flag that determines whether to insert time-stamps. Initially set to
     * false to prevent time-stamps being inserted during the SCM checkout (see
     * HUDSON-7111). Will be set to {@code true} by the
     * {@link #setUp(AbstractBuild, Launcher, BuildListener)} method.
     */
    private boolean insertTimestamps;

    /**
     * Create a new {@link TimestamperLauncher}.
     * 
     * @param delegate
     *          the delegate launcher
     */
    private TimestamperLauncher(Launcher delegate) {
      super(delegate);
      this.delegate = delegate;
    }

    /**
     * Start inserting time-stamps in the STDOUT stream of newly created
     * launchers.
     */
    void startInsertingTimestamps() {
      insertTimestamps = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Proc launch(ProcStarter starter) throws IOException {
      ProcStarter replacementProcStarter = starter.copy();
      if (insertTimestamps) {
        OutputStream replacementOutputStream = new TimestamperOutputStream(
            replacementProcStarter.stdout());
        replacementProcStarter.stdout(replacementOutputStream);
      }
      return delegate.launch(replacementProcStarter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Channel launchChannel(String[] cmd, OutputStream out,
        FilePath workDir, Map<String, String> envVars) throws IOException,
        InterruptedException {
      return delegate.launchChannel(cmd, out, workDir, envVars);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnix() {
      return delegate.isUnix();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void kill(Map<String, String> modelEnvVars) throws IOException,
        InterruptedException {
      delegate.kill(modelEnvVars);
    }
  }

  /**
   * Output stream that writes each line to the provided delegate output stream
   * after inserting a {@link TimestampNote}.
   */
  private static class TimestamperOutputStream extends
      LineTransformationOutputStream {

    /**
     * The delegate output stream.
     */
    private final OutputStream delegate;

    /**
     * Create a new {@link TimestamperOutputStream}.
     * 
     * @param delegate
     *          the delegate output stream
     */
    private TimestamperOutputStream(OutputStream delegate) {
      this.delegate = delegate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void eol(byte[] b, int len) throws IOException {
      new TimestampNote(System.currentTimeMillis()).encodeTo(delegate);
      delegate.write(b, 0, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
      super.close();
      delegate.close();
    }
  }

  /**
   * {@link Converter} implementation for XStream. This converter uses the
   * {@link PureJavaReflectionProvider}, which ensures that the
   * {@link TimestamperBuildWrapper#TimestamperBuildWrapper()} constructor is
   * called.
   */
  public static class ConverterImpl extends RobustReflectionConverter {

    /**
     * Class constructor.
     * 
     * @param mapper
     *          the mapper
     */
    public ConverterImpl(Mapper mapper) {
      super(mapper, new PureJavaReflectionProvider());
    }
  }

  /**
   * Registers {@link TimestamperBuildWrapper} as a {@link BuildWrapper}.
   */
  @Extension
  public static class DescriptorImpl extends BuildWrapperDescriptor {

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
