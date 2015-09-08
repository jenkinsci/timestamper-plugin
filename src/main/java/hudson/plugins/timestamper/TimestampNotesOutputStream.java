/*
 * The MIT License
 * 
 * Copyright (c) 2015 Steven G. Brown
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

import static com.google.common.base.Preconditions.checkNotNull;
import hudson.console.LineTransformationOutputStream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Output stream that writes each line to the provided delegate output stream
 * after inserting a {@link TimestampNote}.
 * 
 * @author Steven G. Brown
 */
class TimestampNotesOutputStream extends LineTransformationOutputStream {

  /**
   * The delegate output stream.
   */
  protected final OutputStream delegate;

  /**
   * Create a new {@link TimestampNotesOutputStream}.
   * 
   * @param delegate
   *          the delegate output stream
   */
  TimestampNotesOutputStream(OutputStream delegate) {
    this.delegate = checkNotNull(delegate);
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
