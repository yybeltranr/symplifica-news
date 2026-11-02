package com.symplifica.symplifica_news.service.ai;

import com.symplifica.symplifica_news.service.rss.RssItem;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;
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
        /* String contenidoNoticias = noticias.stream()
        .map(noticia -> """
                Titular: %s
                Descripción: %s
                Fecha: %s
                """ */
        String fuentes = IntStream.range(0, noticias.size())
                .mapToObj(i -> {
                    RssItem noticia = noticias.get(i);
                    return """
                            Fuente %d
                            Titular: %s
                            Descripción: %s
                            Fecha de publicación: %s
                            """.formatted(i + 1, noticia.title(), noticia.description(),
                            noticia.publicationDate());
                })
        .collect(Collectors.joining("\n"));

      String prompt = """
              Actúa como editor periodístico de un boletín de entretenimiento.
                Redacta en español un resumen fiel de las noticias proporcionadas.

                Reglas obligatorias:
                - Usa exclusivamente los datos presentes en los titulares y descripciones.
                - No inventes nombres, lugares, fechas, cifras, causas, citas ni consecuencias.
                - No completes información que la fuente no especifique.
                - No atribuyas a una noticia cifras que pertenecen a otra.
                - Si dos noticias no tienen una relación clara, resúmelas por separado.
                - Prioriza los hechos y cifras expresamente indicados en las fuentes.
                - Escribe como máximo tres párrafos, con redacción clara y objetiva.
                - No agregues introducciones sobre tu función ni conclusiones especulativas.
                - Devuelve texto plano, sin Markdown, asteriscos, títulos con # ni etiquetas HTML.
                - Si la información disponible es insuficiente, omite el detalle en lugar de inferirlo.

              Información de las noticias:
              %s
              """.formatted(fuentes);

      String resumen = chatModel.chat(prompt);

      if (resumen == null || resumen.isBlank()) {
          throw new IllegalStateException(
                  "El modelo de IA no generó un resumen"
          );
      }

      return resumen.trim();
  }
}
