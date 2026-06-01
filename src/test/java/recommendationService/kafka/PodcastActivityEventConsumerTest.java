package recommendationService.kafka;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;
import recommendationService.profile.ActivityEventHandlingResult;
import recommendationService.profile.PodcastActivityEventService;

class PodcastActivityEventConsumerTest {

    private final PodcastActivityEventService service = mock(PodcastActivityEventService.class);
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final PodcastActivityEventConsumer consumer = new PodcastActivityEventConsumer(service, meterRegistry);

    @Test
    void ignoredEventIsAcknowledged() {
        ConsumerRecord<String, String> record = record("unknown-event-json");
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        when(service.handle(record.value())).thenReturn(ActivityEventHandlingResult.IGNORED);

        consumer.onMessage(record, acknowledgment);

        verify(acknowledgment).acknowledge();
    }

    @Test
    void failedEventIncrementsMetricAndDoesNotAcknowledge() {
        ConsumerRecord<String, String> record = record("invalid-event-json");
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        when(service.handle(record.value())).thenThrow(new IllegalArgumentException("invalid"));

        assertThatThrownBy(() -> consumer.onMessage(record, acknowledgment))
                .isInstanceOf(IllegalArgumentException.class);

        verify(acknowledgment, never()).acknowledge();
        org.assertj.core.api.Assertions.assertThat(meterRegistry.counter("recommendation.events.failed").count())
                .isEqualTo(1.0);
    }

    private ConsumerRecord<String, String> record(String value) {
        return new ConsumerRecord<>("podcast.activity.events.v1", 0, 10L, "key", value);
    }
}
