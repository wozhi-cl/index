package com.company.index.common.model;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 索引目标标准模型
 */
public class IndexDocument {
    private String id;
    private String type; // INSERT, UPDATE, DELETE
    private LocalDateTime timestamp;
    private Map<String, Object> data;
    private String source;
    private Long version;

    public IndexDocument() {}

    public IndexDocument(String id, String type, LocalDateTime timestamp, Map<String, Object> data, String source, Long version) {
        this.id = id;
        this.type = type;
        this.timestamp = timestamp;
        this.data = data;
        this.source = source;
        this.version = version;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexDocument that = (IndexDocument) o;
        return java.util.Objects.equals(id, that.id) &&
                java.util.Objects.equals(type, that.type) &&
                java.util.Objects.equals(timestamp, that.timestamp) &&
                java.util.Objects.equals(source, that.source) &&
                java.util.Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, type, timestamp, source, version);
    }

    @Override
    public String toString() {
        return "IndexDocument{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", timestamp=" + timestamp +
                ", source='" + source + '\'' +
                ", version=" + version +
                '}';
    }
}
