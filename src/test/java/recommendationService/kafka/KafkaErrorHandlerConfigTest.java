package recommendationService.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.SendResult;

class KafkaErrorHandlerConfigTest {

    @Test
    void routesKnownTopicsToConfiguredDlt() {
        RecommendationKafkaProperties properties = properties();

        assertThat(KafkaErrorHandlerConfig.dltTopic("podcast.activity.events.v1", properties))
                .isEqualTo("activity.DLT");
        assertThat(KafkaErrorHandlerConfig.dltTopic("podcast.content.events.v1", properties))
                .isEqualTo("content.DLT");
        assertThat(KafkaErrorHandlerConfig.dltTopic("podcast.search.events.v1", properties))
                .isEqualTo("search.DLT");
    }

    @Test
    @SuppressWarnings("unchecked")
    void retryExhaustionPublishesToDlt() {
        RecommendationKafkaProperties properties = properties();
        properties.getRetry().setAttempts(0);
        KafkaOperations<String, String> kafkaOperations = mock(KafkaOperations.class);
        when(kafkaOperations.send(any(ProducerRecord.class))).thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        DefaultErrorHandler errorHandler = new KafkaErrorHandlerConfig()
                .kafkaErrorHandler(kafkaOperations, properties, meterRegistry);

        errorHandler.handleOne(
                new IllegalStateException("invalid payload"),
                new ConsumerRecord<>("podcast.activity.events.v1", 0, 10L, "key", "bad-json"),
                mock(Consumer.class),
                mock(MessageListenerContainer.class)
        );

        verify(kafkaOperations).send(any(ProducerRecord.class));
        assertThat(meterRegistry.counter("recommendation.events.dlt").count()).isEqualTo(1.0);
    }

    private RecommendationKafkaProperties properties() {
        RecommendationKafkaProperties properties = new RecommendationKafkaProperties();
        properties.getDlt().setPodcastActivityEvents("activity.DLT");
        properties.getDlt().setPodcastContentEvents("content.DLT");
        properties.getDlt().setPodcastSearchEvents("search.DLT");
        return properties;
    }
}
