package recommendationService.trends;

import org.springframework.stereotype.Component;

@Component
public class TrendScoreCalculator {

    public String podcastSql(String alias) {
        return alias + ".play_count + "
                + alias + ".play_finished_count * 2 + "
                + alias + ".like_count * 3 - "
                + alias + ".dislike_count";
    }

    public String authorSql(String alias) {
        return alias + ".play_count + "
                + alias + ".play_finished_count * 2 + "
                + alias + ".like_count * 3 + "
                + alias + ".followed_count * 4 - "
                + alias + ".unfollowed_count * 2 - "
                + alias + ".dislike_count";
    }

    public String playlistSql(String alias) {
        return alias + ".view_count + "
                + alias + ".play_count * 2 + "
                + alias + ".follower_count * 3";
    }
}
