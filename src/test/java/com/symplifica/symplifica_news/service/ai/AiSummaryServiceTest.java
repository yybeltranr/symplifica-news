package com.symplifica.symplifica_news.service.ai;

import com.symplifica.symplifica_news.service.rss.RssItem;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
public class AiSummaryServiceTest {
	@Mock
  private ChatModel chatModel;

  @InjectMocks
  private AiSummaryService aiSummaryService;

  @Test
  void debeGenerarResumenConLosTitulares() {

    List<RssItem> noticias = List.of(
            new RssItem(
                    "Noticia de entretenimiento uno",
                    "https://ejemplo.com/1",
                    "Descripción uno",
                    "01 Nov 2026"
            ),
            new RssItem(
                    "Noticia de entretenimiento dos",
                    "https://ejemplo.com/2",
                    "Descripción dos",
                    "01 Nov 2026"
            )
    );

    String resumenEsperado = "Este es un resumen de prueba.";

    when(chatModel.chat(anyString()))
            .thenReturn(resumenEsperado);

    String resultado = aiSummaryService.generarResumen(noticias);

    assertEquals(resumenEsperado, resultado);

    ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

    verify(chatModel).chat(promptCaptor.capture());

    String promptEnviado = promptCaptor.getValue();

    assertTrue(promptEnviado.contains("Noticia de entretenimiento uno"));
    assertTrue(promptEnviado.contains("Noticia de entretenimiento dos"));
    assertTrue(promptEnviado.contains("máximo tres párrafos"));
    assertTrue(promptEnviado.contains("No inventes nombres, lugares, fechas, cifras"));
    assertTrue(promptEnviado.contains("Descripción: Descripción uno"));
    assertTrue(promptEnviado.contains("sin Markdown"));
  }

  @Test
  void debeRechazarListaDeNoticiasVacia() {

      assertThrows(
              IllegalArgumentException.class,
              () -> aiSummaryService.generarResumen(List.of())
      );

      verifyNoInteractions(chatModel);
  }

  @Test
  void debeLanzarErrorSiElModeloNoGeneraRespuesta() {

      List<RssItem> noticias = List.of(
              new RssItem(
                      "Noticia de prueba",
                      "https://ejemplo.com",
                      "Descripción",
                      "01 Nov 2026"
              )
      );

      when(chatModel.chat(anyString())).thenReturn(" ");

      assertThrows(
              IllegalStateException.class,
              () -> aiSummaryService.generarResumen(noticias)
      );

      verify(chatModel).chat(anyString());
  }
}
