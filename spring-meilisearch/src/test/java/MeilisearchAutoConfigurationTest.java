import com.github.asm0dey.opdsko_spring.meilisearch.MeilisearchAutoConfiguration;
import com.github.asm0dey.opdsko_spring.meilisearch.MeilisearchConnectionDetails;
import com.meilisearch.sdk.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MeilisearchAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MeilisearchAutoConfiguration.class));

    @Test
    void shouldNotConfigureWhenClientClassIsMissing() {
        new ApplicationContextRunner()
                .run(context ->
                        assertThat(context).doesNotHaveBean(Client.class));
    }

    @Test
    void shouldConfigureDefaultConnectionDetails() {
        contextRunner
                .withPropertyValues(
                        "meilisearch.host=localhost",
                        "meilisearch.port=7700",
                        "meilisearch.api-key=test-key"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(MeilisearchConnectionDetails.class);
                    assertThat(context).hasSingleBean(Client.class);

                    MeilisearchConnectionDetails details = context.getBean(MeilisearchConnectionDetails.class);
                    assertThat(details.address()).isEqualTo("localhost:7700");
                    assertThat(details.key()).isEqualTo("test-key");
                });
    }

    @Test
    void shouldUseCustomConnectionDetails() {
        contextRunner
                .withBean(MeilisearchConnectionDetails.class, () ->
                        new MeilisearchConnectionDetails() {
                            @Override
                            public String address() {
                                return "custom-host:" + 7701;
                            }

                            @Override
                            public String key() {
                                return "custom-key";
                            }
                        })
                .run(context -> {
                    assertThat(context).hasSingleBean(MeilisearchConnectionDetails.class);
                    assertThat(context).hasSingleBean(Client.class);

                    MeilisearchConnectionDetails details = context.getBean(MeilisearchConnectionDetails.class);
                    assertThat(details.address()).isEqualTo("custom-host:" + 7701);
                    assertThat(details.key()).isEqualTo("custom-key");
                });
    }

    @Test
    void shouldFailWithoutRequiredProperties() {
        contextRunner
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(BeanCreationException.class)
                            .rootCause()
                            .hasSameClassAs(new IllegalStateException("Meilisearch host is blank"))
                            .hasMessage("Meilisearch host is blank");
                });
    }
}