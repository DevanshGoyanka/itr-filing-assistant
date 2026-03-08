package com.itr.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "client_year_data",
       uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "assessment_year"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClientYearData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "assessment_year", nullable = false, length = 10)
    private String assessmentYear;

    @Column(name = "raw_prefill_json", columnDefinition = "TEXT")
    private String rawPrefillJson;

    @Column(name = "computed_itr1_json", columnDefinition = "TEXT")
    private String computedItr1Json;

    @Column(length = 20)
    @Builder.Default
    private String status = "draft";

    @Column(name = "itr_type", length = 10)
    @Builder.Default
    private String itrType = "ITR1";

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
