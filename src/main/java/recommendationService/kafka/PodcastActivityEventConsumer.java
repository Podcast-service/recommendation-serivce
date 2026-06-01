package recommendationService.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import recommendationService.profile.ActivityEventHandlingResult;
import recommendationService.profile.PodcastActivityEventService;

@Component
@ConditionalOnProperty(prefix = "app.features", name = "kafka-consumers-enabled", havingValue = "true")
public class PodcastActivityEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PodcastActivityEventConsumer.class);

    private final PodcastActivityEventService activityEventService;
    private final MeterRegistry meterRegistry;

    public PodcastActivityEventConsumer(
            PodcastActivityEventService activityEventService,
            MeterRegistry meterRegistry
    ) {
        this.activityEventService = activityEventService;
        this.meterRegistry = meterRegistry;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.podcast-activity-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        try {
            ActivityEventHandlingResult result = activityEventService.handle(record.value());
            acknowledgment.acknowledge();
            log.info(
                    "recommendation_activity_event_ack topic={} partition={} offset={} key={} result={}",
                    record.topic(), record.partition(), record.offset(), record.key(), result
            );
        } catch (RuntimeException exception) {
            meterRegistry.counter("recommendation.events.failed").increment();
            log.error(
                    "recommendation_activity_event_failed topic={} partition={} offset={} key={}",
                    record.topic(), record.partition(), record.offset(), record.key(), exception
            );
            throw exception;
        }
    }
}
