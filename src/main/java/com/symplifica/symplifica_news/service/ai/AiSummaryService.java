package com.symplifica.symplifica_news.service.ai;

import com.symplifica.symplifica_news.service.rss.RssItem;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiSummaryService {
  private final ChatModel chatModel;

  public AiSummaryService(ChatModel chatModel) {
      this.chatModel = chatModel;
  }

  public String generarResumen(List<RssItem> noticias) {

      if (noticias == null || noticias.isEmpty()) {
          throw new IllegalArgumentException(
                  "No hay noticias disponibles para generar el resumen"
          );
      }

      /* String titulos = noticias.stream()
              .map(RssItem::title)
              .collect(Collectors.joining("\n- ", "- ", "")); */
        String contenidoNoticias = noticias.stream()
        .map(noticia -> """
                Titular: %s
                Descripción: %s
                Fecha: %s
                """
                .formatted(
                        noticia.title(),
                        noticia.description(),
                        noticia.publicationDate()
                ))
        .collect(Collectors.joining("\n"));

      String prompt = """
              Actúa como un periodista especializado en noticias
              de entretenimiento.

              A partir de los siguientes titulares, redacta un resumen
              informativo en español.

              Instrucciones:
              - Utiliza únicamente la información proporcionada.
              - No inventes datos ni acontecimientos.
              - Identifica los temas más relevantes.
              - Redacta de manera clara, natural y coherente.
              - El resumen debe tener máximo tres párrafos.
              - No incluyas introducciones sobre tu función como IA.

              Información de las noticias:
              %s
              """.formatted(contenidoNoticias);

      String resumen = chatModel.chat(prompt);

      if (resumen == null || resumen.isBlank()) {
          throw new IllegalStateException(
                  "El modelo de IA no generó un resumen"
          );
      }

      return resumen.trim();
  }
}
