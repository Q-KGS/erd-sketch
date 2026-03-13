package com.erdsketch.document;

import com.erdsketch.project.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "erd_documents")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ErdDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false)
    private String name;

    @Column(name = "yjs_state")
    private byte[] yjsState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_snapshot", columnDefinition = "jsonb")
    private String schemaSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "canvas_settings", columnDefinition = "jsonb")
    private String canvasSettings;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
