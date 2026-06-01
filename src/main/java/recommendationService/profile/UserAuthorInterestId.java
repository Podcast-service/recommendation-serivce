package recommendationService.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserAuthorInterestId implements Serializable {

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "author_id", nullable = false, length = 128)
    private String authorId;

    protected UserAuthorInterestId() {
    }

    public UserAuthorInterestId(String userId, String authorId) {
        this.userId = userId;
        this.authorId = authorId;
    }

    public String getUserId() {
        return userId;
    }

    public String getAuthorId() {
        return authorId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserAuthorInterestId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId)
                && Objects.equals(authorId, that.authorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, authorId);
    }
}
