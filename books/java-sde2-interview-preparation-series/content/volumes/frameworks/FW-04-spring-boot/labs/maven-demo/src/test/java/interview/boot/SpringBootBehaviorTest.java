package interview.boot;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.annotation.Validated;

import static org.assertj.core.api.Assertions.assertThat;

class SpringBootBehaviorTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DemoAutoConfiguration.class))
            .withPropertyValues(
                    "demo.endpoint=https://default.example",
                    "demo.max-attempts=3");

    @Test
    void commandLineWinsAndApplicationBecomesAvailable() {
        SpringApplication application = new SpringApplication(DemoApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setRegisterShutdownHook(false);
        application.setDefaultProperties(java.util.Map.of(
                "demo.endpoint", "https://default.example",
                "demo.max-attempts", "2"));

        try (ConfigurableApplicationContext context = application.run(
                "--demo.endpoint=https://cli.example")) {
            DemoProperties properties = context.getBean(DemoProperties.class);
            ApplicationAvailability availability =
                    context.getBean(ApplicationAvailability.class);

            assertThat(properties.endpoint()).isEqualTo("https://cli.example");
            assertThat(availability.getLivenessState()).isEqualTo(LivenessState.CORRECT);
            assertThat(availability.getReadinessState())
                    .isEqualTo(ReadinessState.ACCEPTING_TRAFFIC);
        }
    }

    @Test
    void autoConfigurationCreatesDefaultClient() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(DemoClient.class);
            assertThat(context.getBean(DemoClient.class).endpoint())
                    .isEqualTo("https://default.example");
        });
    }

    @Test
    void propertyCanDisableAutoConfiguration() {
        runner.withPropertyValues("demo.feature.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(DemoClient.class));
    }

    @Test
    void userBeanMakesDefaultBackOff() {
        runner.withBean(DemoClient.class, () -> new DemoClient("user"))
                .run(context -> {
                    assertThat(context).hasSingleBean(DemoClient.class);
                    assertThat(context.getBean(DemoClient.class).endpoint())
                            .isEqualTo("user");
                });
    }

    @Test
    void invalidConfigurationFailsFast() {
        new ApplicationContextRunner()
                .withUserConfiguration(DemoPropertiesConfiguration.class)
                .withPropertyValues("demo.endpoint=", "demo.max-attempts=0")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("demo");
                });
    }

    @Test
    void conditionReportExplainsAutoConfiguration() {
        runner.run(context -> {
            ConditionEvaluationReport report = ConditionEvaluationReport
                    .get(context.getBeanFactory());
            assertThat(report.getConditionAndOutcomesBySource().keySet())
                    .anyMatch(source -> source.contains("DemoAutoConfiguration"));
        });
    }

    @SpringBootApplication
    @EnableConfigurationProperties(DemoProperties.class)
    static class DemoApplication {
    }

    @AutoConfiguration
    @ConditionalOnProperty(
            prefix = "demo.feature",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    @EnableConfigurationProperties(DemoProperties.class)
    static class DemoAutoConfiguration {
        @Bean
        @ConditionalOnMissingBean
        DemoClient demoClient(DemoProperties properties) {
            return new DemoClient(properties.endpoint());
        }
    }

    @EnableConfigurationProperties(DemoProperties.class)
    static class DemoPropertiesConfiguration {
    }

    @Validated
    @ConfigurationProperties("demo")
    record DemoProperties(@NotBlank String endpoint, @Min(1) int maxAttempts) {
    }

    record DemoClient(String endpoint) {
    }
}
