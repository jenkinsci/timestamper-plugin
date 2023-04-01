package hudson.plugins.timestamper;

import static io.jenkins.plugins.casc.misc.Util.getUnclassifiedRoot;
import static io.jenkins.plugins.casc.misc.Util.toStringFromYamlFile;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.model.CNode;
import org.junit.Rule;
import org.junit.Test;

public class ConfigurationAsCodeTest {

    @Rule
    public JenkinsConfiguredWithCodeRule j = new JenkinsConfiguredWithCodeRule();

    @Test
    @ConfiguredWithCode("Default.yml")
    public void testDefault() {
        TimestamperConfig timestamperConfig = TimestamperConfig.get();
        assertThat(timestamperConfig.getSystemTimeFormat(), containsString("HH:mm:ss"));
        assertThat(timestamperConfig.getElapsedTimeFormat(), containsString("HH:mm:ss.S"));
        assertFalse(timestamperConfig.isAllPipelines());
    }

    @Test
    @ConfiguredWithCode("Customized.yml")
    public void testCustomTimeFormat() {
        TimestamperConfig timestamperConfig = TimestamperConfig.get();
        assertThat(timestamperConfig.getSystemTimeFormat(), containsString("yyyy-MM-dd HH:mm:ss.SSS"));
        assertThat(timestamperConfig.getElapsedTimeFormat(), containsString("HH:mm:ss.SSS"));
        assertTrue(timestamperConfig.isAllPipelines());
    }

    @Test
    @ConfiguredWithCode("EmptyTimeFormat.yml")
    public void testEmptyTimeFormat() {
        TimestamperConfig timestamperConfig = TimestamperConfig.get();
        assertThat(timestamperConfig.getSystemTimeFormat(), is(emptyString()));
        assertThat(timestamperConfig.getElapsedTimeFormat(), is(emptyString()));
        assertFalse(timestamperConfig.isAllPipelines());
    }

    @Test
    @ConfiguredWithCode("Customized.yml")
    public void testConfigAsCodeExport() throws Exception {
        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode timestamperConfig = getUnclassifiedRoot(context).get("timestamper");
        String exported = toYamlString(timestamperConfig);
        String expected = toStringFromYamlFile(this, "CustomizedExport.yml");
        assertEquals(expected, exported);
    }
}
