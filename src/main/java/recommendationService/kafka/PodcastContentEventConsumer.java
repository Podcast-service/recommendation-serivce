package recommendationService.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import recommendationService.catalog.ContentEventHandlingResult;
import recommendationService.catalog.PodcastContentEventService;

@Component
@ConditionalOnProperty(prefix = "app.features", name = "kafka-consumers-enabled", havingValue = "true")
public class PodcastContentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PodcastContentEventConsumer.class);

    private final PodcastContentEventService contentEventService;
    private final MeterRegistry meterRegistry;

    public PodcastContentEventConsumer(
            PodcastContentEventService contentEventService,
            MeterRegistry meterRegistry
    ) {
        this.contentEventService = contentEventService;
        this.meterRegistry = meterRegistry;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.podcast-content-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            ContentEventHandlingResult result = contentEventService.handle(record.value());
            acknowledgment.acknowledge();
            log.info(
                    "recommendation_content_event_ack topic={} partition={} offset={} key={} result={}",
                    record.topic(), record.partition(), record.offset(), record.key(), result
            );
        } catch (RuntimeException exception) {
            meterRegistry.counter("recommendation.events.failed").increment();
            log.error(
                    "recommendation_content_event_failed topic={} partition={} offset={} key={}",
                    record.topic(), record.partition(), record.offset(), record.key(), exception
            );
            throw exception;
        }
    }
}
