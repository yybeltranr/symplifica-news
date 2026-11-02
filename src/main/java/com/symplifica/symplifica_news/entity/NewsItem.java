package com.symplifica.symplifica_news.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "news_items",
        uniqueConstraints = @UniqueConstraint(name = "uq_news_items_guid", columnNames = "guid"),
        indexes = {
                @Index(name = "idx_news_items_fecha_procesamiento", columnList = "fecha_procesamiento DESC"),
                @Index(name = "idx_news_items_news_log", columnList = "news_log_id")
        })
public class NewsItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "guid", nullable = false, length = 1000)
  private String guid;

  @Column(name = "title", nullable = false, length = 1000)
  private String title;

  @Column(name = "link", columnDefinition = "TEXT")
  private String link;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "publication_date", length = 120)
  private String publicationDate;

  @Column(name = "category", length = 120)
  private String category;

  @Column(name = "image_url", columnDefinition = "TEXT")
  private String imageUrl;

  @Column(name = "fecha_procesamiento", nullable = false)
  private OffsetDateTime fechaProcesamiento;

  @Column(name = "news_log_id", insertable = false, updatable = false)
  private Long newsLogId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "news_log_id", nullable = false,
          foreignKey = @ForeignKey(name = "fk_news_items_news_log"))
  private NewsLog newsLog;

  @PrePersist
  void antesDeGuardar() {
      if (fechaProcesamiento == null) {
          fechaProcesamiento = OffsetDateTime.now();
      }
  }

  public Long getId() { return id; }
  public String getGuid() { return guid; }
  public void setGuid(String guid) { this.guid = guid; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getLink() { return link; }
  public void setLink(String link) { this.link = link; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getPublicationDate() { return publicationDate; }
  public void setPublicationDate(String publicationDate) { this.publicationDate = publicationDate; }
  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }
  public String getImageUrl() { return imageUrl; }
  public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
  public OffsetDateTime getFechaProcesamiento() { return fechaProcesamiento; }
  public void setFechaProcesamiento(OffsetDateTime fechaProcesamiento) { this.fechaProcesamiento = fechaProcesamiento; }
  public Long getNewsLogId() { return newsLogId; }
  public NewsLog getNewsLog() { return newsLog; }
  public void setNewsLog(NewsLog newsLog) { this.newsLog = newsLog; }
}
