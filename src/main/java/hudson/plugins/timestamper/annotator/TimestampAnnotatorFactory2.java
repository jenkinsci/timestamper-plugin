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
package hudson.plugins.timestamper.annotator;

import hudson.Extension;
import hudson.Util;
import hudson.PluginWrapper;
import hudson.console.ConsoleAnnotator;
import hudson.console.ConsoleAnnotatorFactory;
import hudson.plugins.timestamper.format.TimestampFormat;
import hudson.plugins.timestamper.format.TimestampFormatProvider;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import jenkins.YesNoMaybe;
import jenkins.model.Jenkins;

import org.kohsuke.stapler.ResponseImpl;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebMethod;

import com.google.common.net.HttpHeaders;

/**
 * Provides the initial {@link TimestampAnnotator} for an annotated console
 * output.
 * 
 * @author Steven G. Brown
 */
@Extension(dynamicLoadable = YesNoMaybe.YES)
public final class TimestampAnnotatorFactory2 extends
    ConsoleAnnotatorFactory<Object> {

  /**
   * {@inheritDoc}
   */
  @Override
  public ConsoleAnnotator<Object> newInstance(Object context) {
    StaplerRequest request = Stapler.getCurrentRequest();
    // JENKINS-16778: The request can be null when the slave goes off-line.
    if (request == null) {
      return null; // do not annotate
    }
    long offset = getOffset(request);
    ConsoleLogParser logParser = new ConsoleLogParser(offset);
    return new TimestampAnnotator(logParser);
  }

  /**
   * Get the current offset for viewing the console log. A non-negative offset
   * is from the start of the file, and a negative offset is back from the end
   * of the file.
   * 
   * @param request
   * @return the offset in bytes
   */
  private static long getOffset(StaplerRequest request) {
    String path = request.getPathInfo();
    if (path == null) {
      // JENKINS-16438
      path = request.getServletPath();
    }
    if (path.endsWith("/consoleFull")) {
      // Displaying the full log of a completed build.
      return 0;
    }
    if (path.endsWith("/console")) {
      // Displaying the tail of the log of a completed build.
      // This duplicates code found in /hudson/model/Run/console.jelly
      // TODO: Ask Jenkins for the console tail size instead.
      String threshold = System.getProperty("hudson.consoleTailKB", "150");
      return -(Long.parseLong(threshold) * 1024);
    }
    // Displaying the log of a build in progress.
    // The start parameter is documented on the build's remote API page.
    String startParameter = request.getParameter("start");
    return startParameter == null ? 0 : Long.parseLong(startParameter);
  }

  /**
   * Get the URL for displaying the plain text console and time-stamps in the
   * current format.
   * 
   * @return the plain text URL
   */
  public String getPlainTextUrl() {
    TimestampFormat format = TimestampFormatProvider.get();
    return format.getPlainTextUrl();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasScript() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasStylesheet() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @WebMethod(name = "script.js")
  public void doScriptJs(StaplerRequest req, StaplerResponse rsp)
      throws IOException, ServletException {

    // This URL is cached for one day. Redirect to a URL which includes the
    // plug-in version and is cached for 1 year. The script will be downloaded
    // again when the plug-in version changes.
    String url = req.getContextPath() + getResourcePath()
        + "/plugin/timestamper/annotator.js";

    // Send the redirect manually and allow the relative URL to be handled by
    // the browser. Do not use the sendRedirect method, which resolves the
    // relative URL to an absolute URL within the servlet container and so
    // gives the wrong result when running behind a proxy. Redirects to
    // relative URLs are allowed by RFC 7231 and the most popular web browsers.
    // https://en.wikipedia.org/wiki/HTTP_location
    rsp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    rsp.setHeader(HttpHeaders.LOCATION, ResponseImpl.encode(url));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @WebMethod(name = "style.css")
  public void doStyleCss(StaplerRequest req, StaplerResponse rsp)
      throws IOException, ServletException {
    // See the comments in doScriptJs for more details.
    String url = req.getContextPath() + getResourcePath()
        + "/plugin/timestamper/style.css";
    rsp.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
    rsp.setHeader(HttpHeaders.LOCATION, ResponseImpl.encode(url));
  }

  private String getResourcePath() {
    Jenkins jenkins = Jenkins.getInstance();
    if (jenkins != null) {
      PluginWrapper plugin = jenkins.getPluginManager()
          .getPlugin("timestamper");
      if (plugin != null) {
        return "/static/" + Util.rawEncode(plugin.getVersion());
      }
    }
    return Jenkins.RESOURCE_PATH;
  }
}
