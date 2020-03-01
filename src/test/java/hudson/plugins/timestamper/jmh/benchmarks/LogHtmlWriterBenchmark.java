package hudson.plugins.timestamper.jmh.benchmarks;

import hudson.Functions;
import hudson.console.AnnotatedLargeText;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.timestamper.TimestamperBuildWrapper;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import jenkins.benchmark.jmh.JmhBenchmark;
import jenkins.benchmark.jmh.JmhBenchmarkState;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.infra.Blackhole;

@JmhBenchmark
public class LogHtmlWriterBenchmark {

    public static class JenkinsState extends JmhBenchmarkState {
        FreeStyleBuild freestyleBuild = null;
        WorkflowRun pipelineBuild = null;

        @Override
        public void setup() throws Exception {
            Jenkins jenkins = getJenkins();

            FreeStyleProject freestyleProject =
                    jenkins.createProject(FreeStyleProject.class, "timestamper-freestyle");
            freestyleProject
                    .getBuildersList()
                    .add(
                            Functions.isWindows()
                                    ? new BatchFile("FOR /L %%n IN (1,1,10000) DO ECHO %%n")
                                    : new Shell("for i in $(seq 1 1 10000); do echo $i; done"));
            freestyleProject.getBuildWrappersList().add(new TimestamperBuildWrapper());
            freestyleBuild = freestyleProject.scheduleBuild2(0).get();

            WorkflowJob pipelineProject =
                    jenkins.createProject(WorkflowJob.class, "timestamper-pipeline");
            pipelineProject.setDefinition(
                    new CpsFlowDefinition(
                            "node {\n"
                                    + "  if (isUnix()) {\n"
                                    + "    sh 'for i in $(seq 1 1 10000); do echo $i; done'\n"
                                    + "  } else {\n"
                                    + "    bat 'FOR /L %%n IN (1,1,10000) DO ECHO %%n'\n"
                                    + "  }\n"
                                    + "}\n",
                            true));
            pipelineBuild = pipelineProject.scheduleBuild2(0).get();
        }
    }

    public static class BlackholeWriter extends Writer {
        private final Blackhole blackhole;

        public BlackholeWriter(Blackhole blackhole) {
            super();
            this.blackhole = Objects.requireNonNull(blackhole);
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            blackhole.consume(cbuf);
            blackhole.consume(off);
            blackhole.consume(len);
        }

        @Override
        public void flush() throws IOException {}

        @Override
        public void close() throws IOException {}
    }

    @Benchmark
    public void logHtmlWriterFreestyleBenchmark(JenkinsState state, Blackhole blackhole)
            throws Exception {
        AnnotatedLargeText logText = state.freestyleBuild.getLogText();
        long r = logText.writeHtmlTo(0, new BlackholeWriter(blackhole));
        blackhole.consume(r);
    }

    @Benchmark
    public void logHtmlWriterPipelineBenchmark(JenkinsState state, Blackhole blackhole)
            throws Exception {
        AnnotatedLargeText logText = state.pipelineBuild.getLogText();
        long r = logText.writeHtmlTo(0, new BlackholeWriter(blackhole));
        blackhole.consume(r);
    }
}
