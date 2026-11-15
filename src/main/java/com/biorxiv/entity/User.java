package com.biorxiv.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Telegram subscriber. {@code dailyArticleLimit} is the overall per-day delivery limit
 * across all of the user's subscriptions.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_telegram_id", columnNames = "telegram_id")
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class User {

    public static final int DEFAULT_DAILY_ARTICLE_LIMIT = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @ToString.Include
    private Long id;

    @NotNull
    @Column(name = "telegram_id", nullable = false, unique = true)
    @ToString.Include
    private Long telegramId;

    @Size(max = 255)
    @Column(name = "username")
    @ToString.Include
    private String username;

    @NotNull
    @PositiveOrZero
    @Builder.Default
    @Column(name = "daily_article_limit", nullable = false)
    @ToString.Include
    private Integer dailyArticleLimit = DEFAULT_DAILY_ARTICLE_LIMIT;

    @NotNull
    @Builder.Default
    @Column(name = "active", nullable = false)
    @ToString.Include
    private Boolean active = Boolean.TRUE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @ToString.Include
    private LocalDateTime createdAt;

    @Builder.Default
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "user")
    private List<Subscription> subscriptions = new ArrayList<>();

    @Builder.Default
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "user")
    private List<Notification> notifications = new ArrayList<>();

}