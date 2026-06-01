package recommendationService.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public class RecommendationKafkaProperties {

    private Topics topics = new Topics();
    private Consumer consumer = new Consumer();
    private Retry retry = new Retry();
    private Dlt dlt = new Dlt();

    public Topics getTopics() {
        return topics;
    }

    public void setTopics(Topics topics) {
        this.topics = topics;
    }

    public Consumer getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer consumer) {
        this.consumer = consumer;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    public Dlt getDlt() {
        return dlt;
    }

    public void setDlt(Dlt dlt) {
        this.dlt = dlt;
    }

    public static class Topics {
        private String podcastActivityEvents = "podcast.activity.events.v1";
        private String podcastContentEvents = "podcast.content.events.v1";
        private String podcastSearchEvents = "podcast.search.events.v1";

        public String getPodcastActivityEvents() {
            return podcastActivityEvents;
        }

        public void setPodcastActivityEvents(String podcastActivityEvents) {
            this.podcastActivityEvents = podcastActivityEvents;
        }

        public String getPodcastContentEvents() {
            return podcastContentEvents;
        }

        public void setPodcastContentEvents(String podcastContentEvents) {
            this.podcastContentEvents = podcastContentEvents;
        }

        public String getPodcastSearchEvents() {
            return podcastSearchEvents;
        }

        public void setPodcastSearchEvents(String podcastSearchEvents) {
            this.podcastSearchEvents = podcastSearchEvents;
        }
    }

    public static class Consumer {
        private int concurrency = 1;

        public int getConcurrency() {
            return concurrency;
        }

        public void setConcurrency(int concurrency) {
            this.concurrency = concurrency;
        }
    }

    public static class Retry {
        private long attempts = 5;
        private long backoffMs = 1000;

        public long getAttempts() {
            return attempts;
        }

        public void setAttempts(long attempts) {
            this.attempts = attempts;
        }

        public long getBackoffMs() {
            return backoffMs;
        }

        public void setBackoffMs(long backoffMs) {
            this.backoffMs = backoffMs;
        }
    }

    public static class Dlt {
        private boolean enabled = true;
        private String podcastActivityEvents = "podcast.activity.events.v1.DLT";
        private String podcastContentEvents = "podcast.content.events.v1.DLT";
        private String podcastSearchEvents = "podcast.search.events.v1.DLT";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getPodcastActivityEvents() {
            return podcastActivityEvents;
        }

        public void setPodcastActivityEvents(String podcastActivityEvents) {
            this.podcastActivityEvents = podcastActivityEvents;
        }

        public String getPodcastContentEvents() {
            return podcastContentEvents;
        }

        public void setPodcastContentEvents(String podcastContentEvents) {
            this.podcastContentEvents = podcastContentEvents;
        }

        public String getPodcastSearchEvents() {
            return podcastSearchEvents;
        }

        public void setPodcastSearchEvents(String podcastSearchEvents) {
            this.podcastSearchEvents = podcastSearchEvents;
        }
    }
}
