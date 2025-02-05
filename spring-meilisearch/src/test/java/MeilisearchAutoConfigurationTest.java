import com.github.asm0dey.opdsko_spring.meilisearch.MeilisearchAutoConfiguration;
import com.github.asm0dey.opdsko_spring.meilisearch.MeilisearchConnectionDetails;
import com.meilisearch.sdk.Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.net.InetSocketAddress;

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
                    assertThat(details.address().getHostName()).isEqualTo("localhost");
                    assertThat(details.address().getPort()).isEqualTo(7700);
                    assertThat(details.key()).isEqualTo("test-key");
                });
    }

    @Test
    void shouldUseCustomConnectionDetails() {
        contextRunner
                .withBean(MeilisearchConnectionDetails.class, () ->
                        new MeilisearchConnectionDetails() {
                            @Override
                            public InetSocketAddress address() {
                                return new InetSocketAddress("custom-host", 7701);
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
                    assertThat(details.address().getHostName()).isEqualTo("custom-host");
                    assertThat(details.address().getPort()).isEqualTo(7701);
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

    @Test
    void shouldFailWithMeaningfulErrorWhenPortOutOfRange() {
        contextRunner
                .withPropertyValues(
                        "meilisearch.host=localhost",
                        "meilisearch.port=99999" // invalid port
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(BeanCreationException.class)
                            .hasMessageContaining("99999")
                            .hasMessageContaining("port");
                });
    }

}