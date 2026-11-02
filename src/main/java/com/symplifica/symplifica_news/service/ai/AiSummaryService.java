
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

			// Enviar únicamente los titulares al modelo.
			String titulos = noticias.stream()
							.map(RssItem::title)
							.filter(titulo -> titulo != null && !titulo.isBlank())
							.map(titulo -> "- " + titulo.trim())
							.collect(Collectors.joining("\n"));

			if (titulos.isBlank()) {
					throw new IllegalArgumentException(
									"No hay títulos disponibles para generar el resumen"
					);
			}

			String prompt = """
							Actúa como editor periodístico de un boletín de noticias de entretenimiento.

							A partir exclusivamente de los titulares proporcionados,
							redacta un resumen en español de las noticias más relevantes.

							Reglas obligatorias:

							- Escribe un máximo de tres párrafos.
							- Selecciona y prioriza las noticias de mayor relevancia.
							- Puedes agrupar noticias que estén claramente relacionadas.
							- No es necesario mencionar todos los titulares.
							- No inventes información ni agregues contexto que no aparezca
								expresamente en los titulares.
							- Conserva el significado original de cada titular.
							- Respeta estrictamente los tiempos verbales y la modalidad
								de las afirmaciones.
							- No conviertas planes en hechos consumados.
							- No conviertas estimaciones o proyecciones en resultados definitivos.
							- Conserva expresiones de incertidumbre como "podría", "planea",
								"se estima", "movería" o "atraería", cuando correspondan.
							- No exageres los acontecimientos ni agregues conclusiones.
							- No atribuyas características, logros o consecuencias que
								los titulares no mencionen.
							- Cuando un titular no permita desarrollar un detalle,
								omite ese detalle en lugar de inferirlo.
							- Utiliza una redacción clara, objetiva y natural.
							- No agregues introducciones ni conclusiones especulativas.
							- Devuelve únicamente los tres párrafos como máximo.
							- No incluyas títulos, encabezados, numeración, viñetas,
								Markdown ni etiquetas HTML.

							Titulares de las noticias:

							%s
							""".formatted(titulos);

			String resumen = chatModel.chat(prompt);

			if (resumen == null || resumen.isBlank()) {
					throw new IllegalStateException(
									"El modelo de IA no generó un resumen"
					);
			}

			return resumen.trim();
	}
}
