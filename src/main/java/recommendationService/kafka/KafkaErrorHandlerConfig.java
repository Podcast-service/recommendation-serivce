package recommendationService.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandlerConfig.class);

    @Bean
    DefaultErrorHandler kafkaErrorHandler(
            KafkaOperations<String, String> kafkaOperations,
            RecommendationKafkaProperties properties,
            MeterRegistry meterRegistry
    ) {
        DeadLetterPublishingRecoverer dltRecoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, exception) -> new TopicPartition(dltTopic(record.topic(), properties), record.partition())
        );
        dltRecoverer.setVerifyPartition(false);
        ConsumerRecordRecoverer recoverer = (record, exception) -> {
            meterRegistry.counter("recommendation.events.failed").increment();
            if (!properties.getDlt().isEnabled()) {
                log.error(
                        "recommendation_event_retry_exhausted_dlt_disabled topic={} partition={} offset={} key={}",
                        record.topic(), record.partition(), record.offset(), record.key(), exception
                );
                return;
            }
            meterRegistry.counter("recommendation.events.dlt").increment();
            log.error(
                    "recommendation_event_sent_to_dlt topic={} dltTopic={} partition={} offset={} key={}",
                    record.topic(), dltTopic(record.topic(), properties), record.partition(), record.offset(), record.key(), exception
            );
            dltRecoverer.accept(record, exception);
        };
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(properties.getRetry().getBackoffMs(), properties.getRetry().getAttempts())
        );
        errorHandler.setCommitRecovered(true);
        errorHandler.setRetryListeners((record, exception, deliveryAttempt) -> log.warn(
                "recommendation_event_retry topic={} partition={} offset={} key={} attempt={}",
                record.topic(), record.partition(), record.offset(), record.key(), deliveryAttempt, exception
        ));
        return errorHandler;
    }

    static String dltTopic(String sourceTopic, RecommendationKafkaProperties properties) {
        if (sourceTopic.equals(properties.getTopics().getPodcastActivityEvents())) {
            return properties.getDlt().getPodcastActivityEvents();
        }
        if (sourceTopic.equals(properties.getTopics().getPodcastContentEvents())) {
            return properties.getDlt().getPodcastContentEvents();
        }
        if (sourceTopic.equals(properties.getTopics().getPodcastSearchEvents())) {
            return properties.getDlt().getPodcastSearchEvents();
        }
        return sourceTopic + ".DLT";
    }
}
