package hudson.plugins.timestamper.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.plugins.timestamper.TimestamperApiTestUtil;
import hudson.plugins.timestamper.TimestamperConfig;
import java.io.BufferedReader;
import java.io.StringReader;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.htmlunit.WebClientUtil;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlPreformattedText;
import org.htmlunit.html.HtmlSpan;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class PipelineTest {

    private JenkinsRule r;

    @BeforeEach
    void setAllPipelines(JenkinsRule r) {
        this.r = r;
        TimestamperConfig config = TimestamperConfig.get();
        config.setAllPipelines(true);
        config.save();
    }

    @Issue("JENKINS-58102")
    @Test
    void globalDecoratorAnnotator() throws Exception {
        WorkflowJob project = r.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition("""
                    node {
                          ansiColor('xterm') {
                            echo 'foo'
                            echo "\\u001B[31mBeginning multi-line color"
                            echo "More color"
                            echo "Ending multi-line color\\u001B[39m"
                          }
                    }""", true));
        WorkflowRun build = r.buildAndAssertSuccess(project);
        r.assertLogContains("foo", build);
        r.assertLogContains("Beginning multi-line color", build);
        r.assertLogContains("More color", build);
        r.assertLogContains("Ending multi-line color", build);

        /*
         * Ensure that each line of the console log is decorated with a valid timestamp decoration.
         * While doing so, save the raw timestamps for later comparison with the annotated console
         * output.
         */
        List<String> rawTimestamps = new ArrayList<>();
        for (String line : build.getLog(Integer.MAX_VALUE)) {
            assertTrue(line.startsWith("["), line);
            int end = line.indexOf(']');
            assertEquals(25, end, line);
            assertNotNull(ZonedDateTime.parse(line.substring(1, end), GlobalDecorator.UTC_MILLIS), line);

            rawTimestamps.add(line.substring(0, 26));
        }

        // Fetch the annotated console output.
        HtmlPage page = r.createWebClient().getPage(build, "consoleFull");
        WebClientUtil.waitForJSExec(page.getWebClient());
        HtmlPreformattedText consoleOutput = page.getFirstByXPath("//pre[@class='console-output']");
        String consoleText = consoleOutput.asNormalizedText();

        /*
         * Ensure that each line of the console output is annotated with a timestamp and a raw
         * timestamp. While doing so, save the raw timestamps for later comparison with the
         * decorated console log.
         */
        List<String> annotatedLines =
                new BufferedReader(new StringReader(consoleText)).lines().collect(Collectors.toList());

        List<String> annotatedTimestamps = getTimestamps(consoleOutput, "//span[@class='timestamp']");
        assertEquals(annotatedLines.size(), annotatedTimestamps.size(), consoleText);

        List<String> annotatedRawTimestamps = getTimestamps(consoleOutput, "//span[contains(@style, 'display: none')]");
        assertEquals(annotatedLines.size(), annotatedRawTimestamps.size(), consoleText);

        for (int i = 0; i < annotatedLines.size(); i++) {
            String annotatedLine = annotatedLines.get(i);
            String prefix = annotatedTimestamps.get(i) + annotatedRawTimestamps.get(i) + ' ';
            assertTrue(
                    annotatedLine.startsWith(prefix),
                    String.format("annotatedLine: '%s', prefix: '%s'", annotatedLine, prefix));

            /*
             * The annotated console output contains "Terminated" lines which don't appear in the
             * decorated console log. In order to do the raw timestamp comparison below, we ignore
             * such lines.
             */
            if (annotatedLine.substring(prefix.length()).equals("Terminated")) {
                annotatedTimestamps.remove(i);
                annotatedRawTimestamps.remove(i);
                annotatedLines.remove(i);
            }
        }

        /*
         * Ensure that the raw timestamps were correctly propagated from the decorated console log
         * to the annotated console output.
         */
        assertEquals(rawTimestamps, annotatedRawTimestamps);
    }

    private static List<String> getTimestamps(HtmlPreformattedText consoleOutput, String xpathExpr) {
        List<String> timestamps = new ArrayList<>();

        List<HtmlSpan> nodes = consoleOutput.getByXPath(xpathExpr);
        for (HtmlSpan node : nodes) {
            timestamps.add(node.getTextContent());
        }

        return timestamps;
    }

    @Issue("JENKINS-60007")
    @Test
    void timestamperApi() throws Exception {
        WorkflowJob project = r.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition("echo 'foo'\n", true));
        WorkflowRun build = r.buildAndAssertSuccess(project);
        r.assertLogContains("foo", build);
        List<String> unstampedLines = new ArrayList<>();
        for (String line : build.getLog(Integer.MAX_VALUE)) {
            assertTrue(GlobalAnnotator.parseTimestamp(line, build.getStartTimeInMillis())
                    .isPresent());
            unstampedLines.add(line.substring(27));
        }
        TimestamperApiTestUtil.timestamperApi(build, unstampedLines);
    }

    @Test
    void timestamperStep() throws Exception {
        TimestamperConfig config = TimestamperConfig.get();
        config.setAllPipelines(false);
        config.save();

        WorkflowJob project = r.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition("timestamps {\n  echo 'foo'\n}", true));

        WorkflowRun build = r.buildAndAssertSuccess(project);
        r.assertLogContains("foo", build);
        for (String line : build.getLog(Integer.MAX_VALUE)) {
            assertEquals(
                    line.contains("foo"),
                    GlobalAnnotator.parseTimestamp(line, build.getStartTimeInMillis())
                            .isPresent(),
                    line);
        }
    }
}
