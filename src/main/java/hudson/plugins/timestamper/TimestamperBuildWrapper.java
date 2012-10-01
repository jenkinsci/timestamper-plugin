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
import hudson.console.LineTransformationOutputStream;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;

import java.io.IOException;
import java.io.OutputStream;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Build wrapper that decorates the build's logger to insert a
 * {@link TimestampNote} on each output line.
 * 
 * @author Steven G. Brown
 * @since 1.0
 */
public final class TimestamperBuildWrapper extends BuildWrapper {

  /**
   * Create a new {@link TimestamperBuildWrapper}.
   */
  @DataBoundConstructor
  public TimestamperBuildWrapper() {
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("rawtypes")
  @Override
  public Environment setUp(AbstractBuild build, Launcher launcher,
      BuildListener listener) throws IOException, InterruptedException {
    // Jenkins requires this method to be overridden.
    return new Environment() {
    };
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("rawtypes")
  @Override
  public OutputStream decorateLogger(AbstractBuild build, OutputStream logger) {
    if (Boolean.getBoolean(TimestampNote.getSystemProperty())) {
      return new TimestampNotesOutputStream(logger);
    }
    return new TimestamperOutputStream(logger, build);
  }

  /**
   * Output stream that writes each line to the provided delegate output stream
   * after inserting a {@link TimestampNote}.
   */
  private static class TimestampNotesOutputStream extends
      LineTransformationOutputStream {

    /**
     * The delegate output stream.
     */
    private final OutputStream delegate;

    /**
     * Create a new {@link TimestampNotesOutputStream}.
     * 
     * @param delegate
     *          the delegate output stream
     */
    TimestampNotesOutputStream(OutputStream delegate) {
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
   * Output stream that records time-stamps into a separate file while
   * inspecting the delegate output stream.
   */
  private static class TimestamperOutputStream extends OutputStream {

    /**
     * The delegate output stream.
     */
    private final OutputStream delegate;

    /**
     * Writer for the time-stamps.
     */
    private final TimestampsIO.Writer timestampsWriter;

    /**
     * Byte array that is re-used each time the {@link #write(int)} method is
     * called.
     */
    private final byte[] oneElementByteArray = new byte[1];

    /**
     * The last processed character, or {@code -1} for the start of the stream.
     */
    private int previousCharacter = -1;

    /**
     * Create a new {@link TimestamperOutputStream}.
     * 
     * @param delegate
     *          the delegate output stream
     * @param build
     *          the build
     */
    TimestamperOutputStream(OutputStream delegate, Run<?, ?> build) {
      this.delegate = delegate;
      this.timestampsWriter = new TimestampsIO.Writer(build);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(int b) throws IOException {
      oneElementByteArray[0] = (byte) b;
      writeTimestamps(oneElementByteArray, 0, 1);
      delegate.write(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(byte[] b) throws IOException {
      writeTimestamps(b, 0, b.length);
      delegate.write(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      writeTimestamps(b, off, len);
      delegate.write(b, 0, len);
    }

    private void writeTimestamps(byte[] b, int off, int len) throws IOException {
      byte newlineCharacter = (byte) 0x0A;
      int lineStartCount = 0;
      for (int i = off; i < off + len; i++) {
        if (previousCharacter == -1 || previousCharacter == newlineCharacter) {
          lineStartCount++;
        }
        previousCharacter = b[i];
      }

      if (lineStartCount > 0) {
        long nanoTime = System.nanoTime();
        long currentTimeMillis = System.currentTimeMillis();
        timestampsWriter.write(nanoTime, currentTimeMillis, lineStartCount);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
      super.close();
      delegate.close();
      timestampsWriter.close();
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
