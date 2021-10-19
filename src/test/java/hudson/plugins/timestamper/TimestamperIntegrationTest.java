package hudson.plugins.timestamper;

import static org.junit.Assert.assertEquals;

import com.gargoylesoftware.htmlunit.WebClientUtil;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlPreformattedText;
import com.gargoylesoftware.htmlunit.html.HtmlSpan;

import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TimestamperIntegrationTest {

    @Rule public JenkinsRule r = new JenkinsRule();

    @Before
    public void setUp() {
        System.clearProperty(TimestampNote.getSystemProperty());
    }

    @After
    public void tearDown() {
        System.clearProperty(TimestampNote.getSystemProperty());
    }

    @Test
    public void buildWrapper() throws Exception {
        FreeStyleProject project = r.createFreeStyleProject();
        project.getBuildersList()
                .add(Functions.isWindows() ? new BatchFile("echo foo") : new Shell("echo foo"));
        project.getBuildWrappersList().add(new TimestamperBuildWrapper());
        FreeStyleBuild build = r.buildAndAssertSuccess(project);
        r.assertLogContains("foo", build);

        // Fetch the unannotated console output.
        List<String> unannotatedLines = build.getLog(Integer.MAX_VALUE);

        // Fetch the annotated console output.
        HtmlPage page = r.createWebClient().getPage(build, "consoleFull");
        WebClientUtil.waitForJSExec(page.getWebClient());
        HtmlPreformattedText consoleOutput = page.getFirstByXPath("//pre[@class='console-output']");
        String consoleText = consoleOutput.asNormalizedText();
        List<String> annotatedLines =
                new BufferedReader(new StringReader(consoleText))
                        .lines()
                        .collect(Collectors.toList());
        assertEquals(consoleText, unannotatedLines.size(), annotatedLines.size());

        // Ensure that each line of the annotated console output has a timestamp.
        List<String> annotatedTimestamps =
                getTimestamps(consoleOutput, "//span[@class='timestamp']");
        assertEquals(consoleText, annotatedLines.size(), annotatedTimestamps.size());
        for (String annotatedTimestamp : annotatedTimestamps) {
            assertEquals(annotatedTimestamp, 8, annotatedTimestamp.length());
        }

        // Ensure that each line is annotated with nothing other than the timestamp.
        for (int i = 0; i < annotatedLines.size(); i++) {
            String annotatedLine = annotatedLines.get(i);
            String prefix = annotatedTimestamps.get(i) + ' ';
            String unannotatedLine = unannotatedLines.get(i);
            assertEquals(annotatedLine, prefix + unannotatedLine);
        }
    }

    private static List<String> getTimestamps(
            HtmlPreformattedText consoleOutput, String xpathExpr) {
        List<String> timestamps = new ArrayList<>();

        List<HtmlSpan> nodes = consoleOutput.getByXPath(xpathExpr);
        for (HtmlSpan node : nodes) {
            timestamps.add(node.getTextContent().trim());
        }

        return timestamps;
    }

    @Test
    public void timestamperApi() throws Exception {
        FreeStyleProject project = r.createFreeStyleProject();
        project.getBuildersList()
                .add(Functions.isWindows() ? new BatchFile("echo foo") : new Shell("echo foo"));
        project.getBuildWrappersList().add(new TimestamperBuildWrapper());
        FreeStyleBuild build = r.buildAndAssertSuccess(project);
        r.assertLogContains("foo", build);

        List<String> unstampedLines = build.getLog(Integer.MAX_VALUE);
        TimestamperApiTestUtil.timestamperApi(build, unstampedLines);
    }

    @Test
    public void timestampNote() throws Exception {
        System.setProperty(TimestampNote.getSystemProperty(), "true");

        FreeStyleProject project = r.createFreeStyleProject();
        project.getBuildersList()
                .add(Functions.isWindows() ? new BatchFile("echo foo") : new Shell("echo foo"));
        project.getBuildWrappersList().add(new TimestamperBuildWrapper());
        FreeStyleBuild build = r.buildAndAssertSuccess(project);
        r.assertLogContains("foo", build);

        List<String> unstampedLines = build.getLog(Integer.MAX_VALUE);
        TimestamperApiTestUtil.timestamperApi(build, unstampedLines);
    }
}
