package com.biorxiv.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Article retrieved from the bioRxiv API. Uniquely identified by DOI + version.
 */
@Entity
@Table(
        name = "articles",
        uniqueConstraints = @UniqueConstraint(name = "uk_articles_doi_version", columnNames = {"doi", "version"}),
        indexes = {
                @Index(name = "idx_articles_category", columnList = "category"),
                @Index(name = "idx_articles_publication_date", columnList = "publication_date")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @ToString.Include
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "doi", nullable = false)
    @ToString.Include
    private String doi;

    @NotNull
    @Positive
    @Column(name = "version", nullable = false)
    @ToString.Include
    private Integer version;

    @NotBlank
    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    @ToString.Include
    private String title;

    @Column(name = "authors", columnDefinition = "TEXT")
    private String authors;

    // "abstract" is a Java keyword, hence the different field name.
    @Column(name = "abstract", columnDefinition = "TEXT")
    private String abstractText;

    @NotBlank
    @Size(max = 255)
    @Column(name = "category", nullable = false)
    @ToString.Include
    private String category;

    /** Date this version was posted, as labelled by bioRxiv (not converted to any time zone). */
    @NotNull
    @Column(name = "publication_date", nullable = false)
    @ToString.Include
    private LocalDate publicationDate;

    /** The {@code jatsxml} field exactly as returned by the bioRxiv API. */
    @Column(name = "jatsxml", columnDefinition = "TEXT")
    private String jatsXml;

    /** Normalized lower case, e.g. "new results", "confirmatory results", "withdrawn". */
    @Size(max = 64)
    @Column(name = "type", length = 64)
    @ToString.Include
    private String type;

    /** License code as returned by the API, e.g. "cc_by", "cc_no". */
    @Size(max = 64)
    @Column(name = "license", length = 64)
    private String license;

    /** Journal DOI once the preprint is published; {@code null} while unpublished (API "NA"). */
    @Size(max = 255)
    @Column(name = "published_doi")
    private String publishedDoi;

    @Column(name = "corresponding_author", columnDefinition = "TEXT")
    private String correspondingAuthor;

    @Column(name = "corresponding_author_institution", columnDefinition = "TEXT")
    private String correspondingAuthorInstitution;

    /** Preprint server as returned by the API ("bioRxiv"). */
    @Size(max = 32)
    @Column(name = "server", length = 32)
    private String server;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @ToString.Include
    private LocalDateTime createdAt;

    @Builder.Default
    @Setter(AccessLevel.NONE)
    @OneToMany(mappedBy = "article")
    private List<Notification> notifications = new ArrayList<>();
}
