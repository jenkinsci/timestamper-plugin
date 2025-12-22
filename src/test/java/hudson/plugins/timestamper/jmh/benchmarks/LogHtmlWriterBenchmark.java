package hudson.plugins.timestamper.jmh.benchmarks;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Functions;
import hudson.console.AnnotatedLargeText;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.plugins.timestamper.TimestamperBuildWrapper;
import hudson.tasks.BatchFile;
import hudson.tasks.Shell;
import java.io.Writer;
import java.util.Objects;
import jenkins.benchmark.jmh.JmhBenchmark;
import jenkins.benchmark.jmh.JmhBenchmarkState;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;

@JmhBenchmark
public class LogHtmlWriterBenchmark {

    public static class JenkinsState extends JmhBenchmarkState {
        FreeStyleBuild freestyleBuild = null;
        WorkflowRun pipelineBuild = null;
        BlackholeWriter blackholeWriter = null;

        @Override
        public void setup() throws Exception {
            Jenkins jenkins = getJenkins();

            FreeStyleProject freestyleProject = jenkins.createProject(FreeStyleProject.class, "timestamper-freestyle");
            freestyleProject
                    .getBuildersList()
                    .add(
                            Functions.isWindows()
                                    ? new BatchFile("FOR /L %%n IN (1,1,10000) DO ECHO %%n")
                                    : new Shell("for i in $(seq 1 1 10000); do echo $i; done"));
            freestyleProject.getBuildWrappersList().add(new TimestamperBuildWrapper());
            freestyleBuild = freestyleProject.scheduleBuild2(0).get();

            WorkflowJob pipelineProject = jenkins.createProject(WorkflowJob.class, "timestamper-pipeline");
            pipelineProject.setDefinition(new CpsFlowDefinition("""
                    "node {
                        if (isUnix()) {
                            sh 'for i in $(seq 1 1 10000); do echo $i; done'
                        } else {
                            bat 'FOR /L %%n IN (1,1,10000) DO ECHO %%n'
                        }
                    }
                    """, true));
            pipelineBuild = pipelineProject.scheduleBuild2(0).get();
        }

        @Setup
        public void setupBlackholeWriter(Blackhole blackhole) {
            blackholeWriter = new BlackholeWriter(blackhole);
        }
    }

    public static class BlackholeWriter extends Writer {
        private final Blackhole blackhole;

        public BlackholeWriter(Blackhole blackhole) {
            super();
            this.blackhole = Objects.requireNonNull(blackhole);
        }

        @Override
        public void write(int c) {
            blackhole.consume(c);
        }

        @Override
        public void write(@NonNull char[] cbuf) {
            blackhole.consume(cbuf);
        }

        @Override
        public void write(@NonNull char[] cbuf, int off, int len) {
            blackhole.consume(cbuf);
            blackhole.consume(off);
            blackhole.consume(len);
        }

        @Override
        public void write(@NonNull String str) {
            blackhole.consume(str);
        }

        @Override
        public void write(@NonNull String str, int off, int len) {
            blackhole.consume(str);
            blackhole.consume(off);
            blackhole.consume(len);
        }

        @Override
        public Writer append(CharSequence csq) {
            blackhole.consume(csq);
            return this;
        }

        @Override
        public Writer append(CharSequence csq, int start, int end) {
            blackhole.consume(csq);
            blackhole.consume(start);
            blackhole.consume(end);
            return this;
        }

        @Override
        public Writer append(char c) {
            blackhole.consume(c);
            return this;
        }

        @Override
        public void flush() {}

        @Override
        public void close() {}
    }

    @Benchmark
    public void logHtmlWriterFreestyleBenchmark(JenkinsState state, Blackhole blackhole) throws Exception {
        AnnotatedLargeText<?> logText = state.freestyleBuild.getLogText();
        long r = logText.writeHtmlTo(0, state.blackholeWriter);
        blackhole.consume(r);
    }

    @Benchmark
    public void logHtmlWriterPipelineBenchmark(JenkinsState state, Blackhole blackhole) throws Exception {
        AnnotatedLargeText<?> logText = state.pipelineBuild.getLogText();
        long r = logText.writeHtmlTo(0, state.blackholeWriter);
        blackhole.consume(r);
    }
}
