package com.symplifica.symplifica_news.controller;

import com.symplifica.symplifica_news.dto.NewsResponse;
import com.symplifica.symplifica_news.dto.NewsRunResponse;
import com.symplifica.symplifica_news.entity.NewsItem;
import com.symplifica.symplifica_news.entity.NewsLog;
import com.symplifica.symplifica_news.repository.NewsItemRepository;
import com.symplifica.symplifica_news.repository.NewsLogRepository;
import com.symplifica.symplifica_news.service.NewsOrchestrationService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/news")
public class NewsController {
  private final NewsOrchestrationService orchestrationService;
  private final NewsLogRepository newsLogRepository;
  private final NewsItemRepository newsItemRepository;

  public NewsController(NewsOrchestrationService orchestrationService,
                        NewsLogRepository newsLogRepository,
                        NewsItemRepository newsItemRepository) {
      this.orchestrationService = orchestrationService;
      this.newsLogRepository = newsLogRepository;
      this.newsItemRepository = newsItemRepository;
  }

  /** Historial de las últimas 50 ejecuciones. */
  @GetMapping("/logs")
  public List<NewsRunResponse> obtenerHistorial() {
      return newsLogRepository.findTop50ByOrderByFechaInicioDesc().stream()
              .map(this::toRunResponse)
              .toList();
  }

  @GetMapping("/logs/{id}")
  public NewsRunResponse obtenerEjecucion(@PathVariable Long id) {
      NewsLog log = newsLogRepository.findById(id)
              .orElseThrow(() -> new EntityNotFoundException(
                      "No existe la ejecución con id " + id));
      return toRunResponse(log);
  }

  /** Últimas 100 noticias procesadas, ordenadas por fecha de procesamiento. */
  @GetMapping("/items")
  public List<NewsResponse> obtenerNoticiasProcesadas() {
      return newsItemRepository.findTop100ByOrderByFechaProcesamientoDesc().stream()
              .map(this::toNewsResponse)
              .toList();
  }

  /** Ejecuta el flujo de forma síncrona; puede enviar un correo a NEWS_EMAIL_TO. */
  @PostMapping("/run")
  public ResponseEntity<NewsRunResponse> ejecutarManualmente() {
      NewsLog log = orchestrationService.ejecutarProcesoNoticias();
      return ResponseEntity.ok(toRunResponse(log));
  }

  private NewsRunResponse toRunResponse(NewsLog log) {
      String message;
      if ("FAILED".equals(log.getEstado())) {
          message = "La ejecución terminó con error.";
      } else if (log.getCantidadNoticias() == null || log.getCantidadNoticias() == 0) {
          message = "No se encontraron noticias nuevas para enviar.";
      } else {
          message = "Resumen generado y enviado correctamente.";
      }
      return new NewsRunResponse(log.getId(), log.getEstado(), log.getCantidadNoticias(),
              log.getFechaInicio(), log.getFechaFin(), log.getResumen(),
              log.getMensajeError(), message);
  }

  private NewsResponse toNewsResponse(NewsItem item) {
      return new NewsResponse(item.getId(), item.getGuid(), item.getTitle(), item.getLink(),
              item.getDescription(), item.getPublicationDate(), item.getCategory(),
              item.getImageUrl(), item.getFechaProcesamiento(),
              item.getNewsLogId());
  }
}
