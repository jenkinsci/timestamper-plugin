package hudson.plugins.timestamper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import hudson.Functions;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.htmlunit.WebClientUtil;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlPreformattedText;
import org.htmlunit.html.HtmlSpan;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class TimestamperIntegrationTest {

    @BeforeEach
    void setUp() {
        System.clearProperty(TimestampNote.getSystemProperty());
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(TimestampNote.getSystemProperty());
    }

    @Test
    void buildWrapper(JenkinsRule r) throws Exception {
        FreeStyleProject project = r.createFreeStyleProject();
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo foo") : new Shell("echo foo"));
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
                new BufferedReader(new StringReader(consoleText)).lines().toList();
        assertEquals(unannotatedLines.size(), annotatedLines.size(), consoleText);

        // Ensure that each line of the annotated console output has a timestamp.
        List<String> annotatedTimestamps = getTimestamps(consoleOutput, "//span[@class='timestamp']");
        assertEquals(annotatedLines.size(), annotatedTimestamps.size(), consoleText);
        for (String annotatedTimestamp : annotatedTimestamps) {
            assertEquals(8, annotatedTimestamp.length(), annotatedTimestamp);
        }

        // Ensure that each line is annotated with nothing other than the timestamp.
        for (int i = 0; i < annotatedLines.size(); i++) {
            String annotatedLine = annotatedLines.get(i);
            String prefix = annotatedTimestamps.get(i) + ' ';
            String unannotatedLine = unannotatedLines.get(i);
            assertEquals(annotatedLine, prefix + unannotatedLine);
        }
    }

    private static List<String> getTimestamps(HtmlPreformattedText consoleOutput, String xpathExpr) {
        List<String> timestamps = new ArrayList<>();

        List<HtmlSpan> nodes = consoleOutput.getByXPath(xpathExpr);
        for (HtmlSpan node : nodes) {
            timestamps.add(node.getTextContent().trim());
        }

        return timestamps;
    }

    @Test
    void timestamperApi(JenkinsRule r) throws Exception {
        FreeStyleProject project = r.createFreeStyleProject();
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo foo") : new Shell("echo foo"));
        project.getBuildWrappersList().add(new TimestamperBuildWrapper());
        FreeStyleBuild build = r.buildAndAssertSuccess(project);
        r.assertLogContains("foo", build);

        List<String> unstampedLines = build.getLog(Integer.MAX_VALUE);
        TimestamperApiTestUtil.timestamperApi(build, unstampedLines);
    }

    @Test
    void timestampNote(JenkinsRule r) throws Exception {
        System.setProperty(TimestampNote.getSystemProperty(), "true");

        FreeStyleProject project = r.createFreeStyleProject();
        project.getBuildersList().add(Functions.isWindows() ? new BatchFile("echo foo") : new Shell("echo foo"));
        project.getBuildWrappersList().add(new TimestamperBuildWrapper());
        FreeStyleBuild build = r.buildAndAssertSuccess(project);
        r.assertLogContains("foo", build);

        List<String> unstampedLines = build.getLog(Integer.MAX_VALUE);
        TimestamperApiTestUtil.timestamperApi(build, unstampedLines);
    }
}
