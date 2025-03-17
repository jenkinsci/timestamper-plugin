package hudson.plugins.timestamper;

import static io.jenkins.plugins.casc.misc.Util.getUnclassifiedRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import io.jenkins.plugins.casc.model.CNode;
import org.junit.jupiter.api.Test;

@WithJenkinsConfiguredWithCode
class ConfigurationAsCodeTest {

    @Test
    @ConfiguredWithCode("Default.yml")
    void testDefault(JenkinsConfiguredWithCodeRule j) {
        TimestamperConfig timestamperConfig = TimestamperConfig.get();
        assertThat(timestamperConfig.getSystemTimeFormat(), containsString("HH:mm:ss"));
        assertThat(timestamperConfig.getElapsedTimeFormat(), containsString("HH:mm:ss.S"));
        assertFalse(timestamperConfig.isAllPipelines());
    }

    @Test
    @ConfiguredWithCode("Customized.yml")
    void testCustomTimeFormat(JenkinsConfiguredWithCodeRule j) {
        TimestamperConfig timestamperConfig = TimestamperConfig.get();
        assertThat(timestamperConfig.getSystemTimeFormat(), containsString("yyyy-MM-dd HH:mm:ss.SSS"));
        assertThat(timestamperConfig.getElapsedTimeFormat(), containsString("HH:mm:ss.SSS"));
        assertTrue(timestamperConfig.isAllPipelines());
    }

    @Test
    @ConfiguredWithCode("EmptyTimeFormat.yml")
    void testEmptyTimeFormat(JenkinsConfiguredWithCodeRule j) {
        TimestamperConfig timestamperConfig = TimestamperConfig.get();
        assertThat(timestamperConfig.getSystemTimeFormat(), is(emptyString()));
        assertThat(timestamperConfig.getElapsedTimeFormat(), is(emptyString()));
        assertFalse(timestamperConfig.isAllPipelines());
    }

    @Test
    @ConfiguredWithCode("Customized.yml")
    void testConfigAsCodeExport(JenkinsConfiguredWithCodeRule j) throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode timestamperConfig = getUnclassifiedRoot(context).get("timestamper");
        String exported = toYamlString(timestamperConfig);
        String expected = toStringFromYamlFile(this, "CustomizedExport.yml");
        assertEquals(expected, exported);
    }
}
