package com.symplifica.symplifica_news.service;

import com.symplifica.symplifica_news.entity.NewsLog;
import com.symplifica.symplifica_news.entity.NewsItem;
import com.symplifica.symplifica_news.repository.NewsItemRepository;
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

  @Mock
  private NewsItemRepository newsItemRepository;

  private NewsOrchestrationService orchestrationService;

  private NewsLog log;

  @BeforeEach
  void setUp() {
      orchestrationService = new NewsOrchestrationService(
              rssService,
              aiSummaryService,
              emailService,
              newsLogService,
              newsItemRepository,
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
      when(newsItemRepository.findAllByGuidIn(anyCollection())).thenReturn(List.of());

      assertDoesNotThrow(
              () -> orchestrationService.ejecutarProcesoNoticias()
      );

      InOrder orden = inOrder(
              newsLogService,
              rssService,
              aiSummaryService,
              emailService,
              newsItemRepository
      );

      orden.verify(newsLogService).iniciarEjecucion();
      orden.verify(rssService).obtenerNoticias();
      orden.verify(newsItemRepository).findAllByGuidIn(anyCollection());
      orden.verify(aiSummaryService).generarResumen(noticias);
      orden.verify(emailService).enviarResumen(resumen);
      orden.verify(newsItemRepository).saveAll(anyList());

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

      verifyNoInteractions(aiSummaryService, emailService, newsItemRepository);
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

      verifyNoInteractions(aiSummaryService, emailService, newsItemRepository);
  }

  @Test
  void noDebeEnviarNoticiasQueYaFueronProcesadas() {
      RssItem noticia = new RssItem(
              "Noticia repetida",
              "https://ejemplo.com/repetida",
              "Descripción",
              "02 Nov 2026",
              "guid-repetido",
              "Entretenimiento",
              ""
      );
      NewsItem existente = new NewsItem();
      existente.setGuid("guid-repetido");

      when(newsLogService.iniciarEjecucion()).thenReturn(log);
      when(rssService.obtenerNoticias()).thenReturn(List.of(noticia));
      when(newsItemRepository.findAllByGuidIn(anyCollection()))
              .thenReturn(List.of(existente));

      orchestrationService.ejecutarProcesoNoticias();

      verifyNoInteractions(aiSummaryService, emailService);
      verify(newsItemRepository, never()).saveAll(anyList());
      verify(newsLogService).finalizarEjecucion(log, 0, null, null);
  }

  @Test
  void debeProcesarSoloLasNoticiasNuevas() {
      RssItem noticiaNueva = new RssItem(
              "Noticia nueva",
              "https://ejemplo.com/nueva",
              "Descripción nueva",
              "02 Nov 2026",
              "guid-nuevo",
              "Entretenimiento",
              ""
      );
      RssItem noticiaExistente = new RssItem(
              "Noticia existente",
              "https://ejemplo.com/existente",
              "Descripción existente",
              "01 Nov 2026",
              "guid-existente",
              "Entretenimiento",
              ""
      );
      NewsItem existente = new NewsItem();
      existente.setGuid("guid-existente");
      List<RssItem> feed = List.of(noticiaExistente, noticiaNueva);

      when(newsLogService.iniciarEjecucion()).thenReturn(log);
      when(rssService.obtenerNoticias()).thenReturn(feed);
      when(newsItemRepository.findAllByGuidIn(anyCollection()))
              .thenReturn(List.of(existente));
      when(aiSummaryService.generarResumen(List.of(noticiaNueva)))
              .thenReturn("Resumen de la noticia nueva");

      orchestrationService.ejecutarProcesoNoticias();

      verify(aiSummaryService).generarResumen(List.of(noticiaNueva));
      verify(emailService).enviarResumen("Resumen de la noticia nueva");
      verify(newsItemRepository).saveAll(argThat(items ->
              items instanceof List<?> lista
                      && lista.size() == 1
                      && "guid-nuevo".equals(((NewsItem) lista.get(0)).getGuid())
      ));
      verify(newsLogService).finalizarEjecucion(
              log, 1, "Resumen de la noticia nueva", "pruebas@example.com");
  }
  @Test
  void noDebeMarcarNoticiasComoProcesadasSiFallaElCorreo() {
      RssItem noticia = new RssItem(
              "Noticia nueva",
              "https://ejemplo.com/nueva",
              "Descripción nueva",
              "02 Nov 2026",
              "guid-nuevo-fallo",
              "Entretenimiento",
              ""
      );
      when(newsLogService.iniciarEjecucion()).thenReturn(log);
      when(rssService.obtenerNoticias()).thenReturn(List.of(noticia));
      when(newsItemRepository.findAllByGuidIn(anyCollection())).thenReturn(List.of());
      when(aiSummaryService.generarResumen(List.of(noticia))).thenReturn("Resumen");
      doThrow(new IllegalStateException("Fallo SMTP"))
              .when(emailService).enviarResumen("Resumen");

      IllegalStateException error = assertThrows(
              IllegalStateException.class,
              () -> orchestrationService.ejecutarProcesoNoticias());

      assertEquals("Fallo SMTP", error.getMessage());
      verify(newsItemRepository, never()).saveAll(anyList());
      verify(newsLogService).registrarError(log, "Fallo SMTP");
  }

}
