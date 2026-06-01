package recommendationService.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserCategoryInterestId implements Serializable {

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "category_id", nullable = false, length = 128)
    private String categoryId;

    protected UserCategoryInterestId() {
    }

    public UserCategoryInterestId(String userId, String categoryId) {
        this.userId = userId;
        this.categoryId = categoryId;
    }

    public String getUserId() {
        return userId;
    }

    public String getCategoryId() {
        return categoryId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserCategoryInterestId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId)
                && Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, categoryId);
    }
}
