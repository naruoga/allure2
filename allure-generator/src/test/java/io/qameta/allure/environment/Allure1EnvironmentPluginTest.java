package io.qameta.allure.environment;

import io.qameta.allure.ConfigurationBuilder;
import io.qameta.allure.DefaultResultsVisitor;
import io.qameta.allure.allure1.Allure1Plugin;
import io.qameta.allure.core.Configuration;
import io.qameta.allure.core.LaunchResults;
import io.qameta.allure.entity.EnvironmentItem;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.allurefw.allure1.AllureUtils.generateTestSuiteJsonName;
import static org.allurefw.allure1.AllureUtils.generateTestSuiteXmlName;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Egor Borisov ehborisov@gmail.com
 */
public class Allure1EnvironmentPluginTest {

    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void shouldReadEnvironmentProperties() throws Exception {

        EnvironmentItem[] expected = new EnvironmentItem[]{
                new EnvironmentItem().withName("allure.test.run.id").withValues("some-id"),
                new EnvironmentItem().withName("allure.test.run.name").withValues("some-name"),
                new EnvironmentItem().withName("allure.test.property").withValues("1")
        };

        List<EnvironmentItem> environment = process(
                Arrays.asList(
                        "allure1/sample-testsuite.json", generateTestSuiteJsonName(),
                        "allure1/environment.properties", "environment.properties"
                )
        );

        assertThat(environment)
                .as("Unexpected environment properties have been read from properties file")
                .hasSize(3)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expected);
    }

    @Test
    public void shouldReadEnvironmentXml() throws Exception {
        EnvironmentItem[] expected = new EnvironmentItem[]{
                new EnvironmentItem().withName("my.properties.browser").withValues("Firefox"),
                new EnvironmentItem().withName("my.properties.url").withValues("http://yandex.ru"),
                new EnvironmentItem().withName("allure.test.property").withValues("3"),
        };

        List<EnvironmentItem> environment = process(
                Arrays.asList(
                        "allure1/sample-testsuite.json", generateTestSuiteJsonName(),
                        "allure1/environment.xml", "environment.xml"
                )
        );

        assertThat(environment)
                .as("Unexpected environment properties have been read from xml file")
                .hasSize(3)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expected);
    }

    @Test
    public void shouldStackParameterValues() throws Exception {
        EnvironmentItem[] expected = new EnvironmentItem[]{
                new EnvironmentItem().withName("my.properties.browser").withValues("Firefox"),
                new EnvironmentItem().withName("my.properties.url").withValues("http://yandex.ru"),
                new EnvironmentItem().withName("allure.test.run.id").withValues("some-id"),
                new EnvironmentItem().withName("allure.test.run.name").withValues("some-name"),
                new EnvironmentItem().withName("allure.test.property").withValues("2", "3"),
                new EnvironmentItem().withName("allure.test.other.property").withValues("value")
        };

        List<EnvironmentItem> environment = process(
                Arrays.asList(
                        "allure1/environment-variables-testsuite.xml", generateTestSuiteXmlName(),
                        "allure1/environment.properties", "environment.properties"
                ),
                Arrays.asList(
                        "allure1/sample-testsuite.xml", generateTestSuiteXmlName(),
                        "allure1/environment.xml", "environment.xml"
                )
        );


        assertThat(environment)
                .as("Unexpected environment properties have been read from test results and properties file")
                .hasSize(6)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrder(expected);
    }

    @SafeVarargs
    private final List<EnvironmentItem> process(List<String>... results) throws IOException {
        List<LaunchResults> launches = new ArrayList<>();
        final Configuration configuration = new ConfigurationBuilder().useDefault().build();
        Allure1Plugin reader = new Allure1Plugin();
        for (List<String> result : results) {
            Path resultsDirectory = folder.newFolder().toPath();
            Iterator<String> iterator = result.iterator();
            while (iterator.hasNext()) {
                String first = iterator.next();
                String second = iterator.next();
                copyFile(resultsDirectory, first, second);
            }
            final DefaultResultsVisitor resultsVisitor = new DefaultResultsVisitor(configuration);
            reader.readResults(configuration, resultsVisitor, resultsDirectory);
            launches.add(resultsVisitor.getLaunchResults());
        }
        Allure1EnvironmentPlugin envPlugin = new Allure1EnvironmentPlugin();
        return envPlugin.getData(configuration, launches);
    }

    private void copyFile(Path dir, String resourceName, String fileName) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            Files.copy(is, dir.resolve(fileName));
        }
    }
}
