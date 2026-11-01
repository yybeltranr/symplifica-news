package com.symplifica.symplifica_news.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
@Entity
@Table(name = "news_log")
public class NewsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_inicio", nullable = false)
    private OffsetDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private OffsetDateTime fechaFin;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "cantidad_noticias", nullable = false)
    private Integer cantidadNoticias = 0;

    @Column(name = "resumen", columnDefinition = "TEXT")
    private String resumen;

    @Column(name = "destinatarios", columnDefinition = "TEXT")
    private String destinatarios;

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;

    public NewsLog() {
    }

    // Getters y setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OffsetDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(OffsetDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public OffsetDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(OffsetDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Integer getCantidadNoticias() {
        return cantidadNoticias;
    }

    public void setCantidadNoticias(Integer cantidadNoticias) {
        this.cantidadNoticias = cantidadNoticias;
    }

    public String getResumen() {
        return resumen;
    }

    public void setResumen(String resumen) {
        this.resumen = resumen;
    }

    public String getDestinatarios() {
        return destinatarios;
    }

    public void setDestinatarios(String destinatarios) {
        this.destinatarios = destinatarios;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public void setMensajeError(String mensajeError) {
        this.mensajeError = mensajeError;
    }
}
