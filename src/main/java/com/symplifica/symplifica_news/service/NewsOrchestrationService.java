package com.symplifica.symplifica_news.service;

import com.symplifica.symplifica_news.entity.NewsLog;
import com.symplifica.symplifica_news.service.ai.AiSummaryService;
import com.symplifica.symplifica_news.service.email.EmailService;
import com.symplifica.symplifica_news.service.logging.NewsLogService;
import com.symplifica.symplifica_news.service.rss.RssItem;
import com.symplifica.symplifica_news.service.rss.RssService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NewsOrchestrationService {
  private final RssService rssService;
  private final AiSummaryService aiSummaryService;
  private final EmailService emailService;
  private final NewsLogService newsLogService;
  private final String destinatarios;

  public NewsOrchestrationService(
          RssService rssService,
          AiSummaryService aiSummaryService,
          EmailService emailService,
          NewsLogService newsLogService,
          @Value("${app.news.recipients}") String destinatarios) {

      this.rssService = rssService;
      this.aiSummaryService = aiSummaryService;
      this.emailService = emailService;
      this.newsLogService = newsLogService;
      this.destinatarios = destinatarios;
  }

  public void ejecutarProcesoNoticias() {

      // 1. Registrar el inicio de la ejecución.
      NewsLog log = newsLogService.iniciarEjecucion();

      try {

          // 2. Obtener noticias desde el RSS.
          List<RssItem> noticias = rssService.obtenerNoticias();

          // 3. Evitar procesar un feed vacío.
          if (noticias.isEmpty()) {
              newsLogService.finalizarEjecucion(
                      log,
                      0,
                      null,
                      null
              );
              return;
          }

          // 4. Generar el resumen mediante LangChain4j y Ollama.
          String resumen = aiSummaryService.generarResumen(noticias);

          // 5. Enviar el resumen por correo.
          emailService.enviarResumen(resumen);

          // 6. Registrar el resultado exitoso.
          newsLogService.finalizarEjecucion(
                  log,
                  noticias.size(),
                  resumen,
                  destinatarios
          );

      } catch (RuntimeException e) {

          // 7. Registrar el error de la ejecución.
          try {
              newsLogService.registrarError(
                      log,
                      e.getMessage() != null
                              ? e.getMessage()
                              : e.getClass().getSimpleName()
              );

          } catch (RuntimeException errorRegistro) {
              e.addSuppressed(errorRegistro);
          }

          // Permitir que JobRunr conozca el fallo.
          throw e;
      }
  }
}
