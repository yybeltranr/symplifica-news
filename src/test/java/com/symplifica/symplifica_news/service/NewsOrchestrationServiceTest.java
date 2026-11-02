package com.symplifica.symplifica_news.service;

import com.symplifica.symplifica_news.entity.NewsLog;
import com.symplifica.symplifica_news.service.ai.AiSummaryService;
import com.symplifica.symplifica_news.service.email.EmailService;
import com.symplifica.symplifica_news.service.logging.NewsLogService;
import com.symplifica.symplifica_news.service.rss.RssItem;
import com.symplifica.symplifica_news.service.rss.RssService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NewsOrchestrationServiceTest {
  @Mock
  private RssService rssService;

  @Mock
  private AiSummaryService aiSummaryService;

  @Mock
  private EmailService emailService;

  @Mock
  private NewsLogService newsLogService;

  private NewsOrchestrationService orchestrationService;

  private NewsLog log;

  @BeforeEach
  void setUp() {
      orchestrationService = new NewsOrchestrationService(
              rssService,
              aiSummaryService,
              emailService,
              newsLogService,
              "pruebas@example.com"
      );

      log = new NewsLog();
  }

  @Test
  void debeEjecutarProcesoCompletoCuandoHayNoticias() {

      List<RssItem> noticias = List.of(
              new RssItem(
                      "Noticia de entretenimiento",
                      "https://ejemplo.com/noticia",
                      "Descripción de prueba",
                      "Mon, 02 Nov 2026 10:00:00 GMT"
              )
      );

      String resumen = "Este es el resumen de las noticias.";

      when(newsLogService.iniciarEjecucion()).thenReturn(log);
      when(rssService.obtenerNoticias()).thenReturn(noticias);
      when(aiSummaryService.generarResumen(noticias)).thenReturn(resumen);

      assertDoesNotThrow(
              () -> orchestrationService.ejecutarProcesoNoticias()
      );

      InOrder orden = inOrder(
              newsLogService,
              rssService,
              aiSummaryService,
              emailService
      );

      orden.verify(newsLogService).iniciarEjecucion();
      orden.verify(rssService).obtenerNoticias();
      orden.verify(aiSummaryService).generarResumen(noticias);
      orden.verify(emailService).enviarResumen(resumen);

      orden.verify(newsLogService).finalizarEjecucion(
              log,
              1,
              resumen,
              "pruebas@example.com"
      );
  }

  @Test
  void debeFinalizarSinEnviarCorreoCuandoNoHayNoticias() {

      when(newsLogService.iniciarEjecucion()).thenReturn(log);
      when(rssService.obtenerNoticias()).thenReturn(List.of());

      assertDoesNotThrow(
              () -> orchestrationService.ejecutarProcesoNoticias()
      );

      verify(newsLogService).finalizarEjecucion(
              log,
              0,
              null,
              null
      );

      verifyNoInteractions(aiSummaryService, emailService);
  }

  @Test
  void debeRegistrarErrorCuandoFallaElProceso() {

      when(newsLogService.iniciarEjecucion()).thenReturn(log);

      when(rssService.obtenerNoticias()).thenThrow(
              new IllegalStateException("Fallo al consultar el RSS")
      );

      IllegalStateException excepcion = assertThrows(
              IllegalStateException.class,
              () -> orchestrationService.ejecutarProcesoNoticias()
      );

      assertEquals(
              "Fallo al consultar el RSS",
              excepcion.getMessage()
      );

      verify(newsLogService).registrarError(
              log,
              "Fallo al consultar el RSS"
      );

      verifyNoInteractions(aiSummaryService, emailService);
  }
}
