package com.symplifica.symplifica_news.service;

import com.symplifica.symplifica_news.entity.NewsItem;
import com.symplifica.symplifica_news.entity.NewsLog;
import com.symplifica.symplifica_news.repository.NewsItemRepository;
import com.symplifica.symplifica_news.service.ai.AiSummaryService;
import com.symplifica.symplifica_news.service.email.EmailService;
import com.symplifica.symplifica_news.service.logging.NewsLogService;
import com.symplifica.symplifica_news.service.rss.RssItem;
import com.symplifica.symplifica_news.service.rss.RssService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NewsOrchestrationService {
  private final RssService rssService;
  private final AiSummaryService aiSummaryService;
  private final EmailService emailService;
  private final NewsLogService newsLogService;
  private final NewsItemRepository newsItemRepository;
  private final String destinatarios;

	public NewsOrchestrationService(
					RssService rssService,
					AiSummaryService aiSummaryService,
					EmailService emailService,
					NewsLogService newsLogService,
					NewsItemRepository newsItemRepository,
					@Value("${app.news.recipients}") String destinatarios) {
			this.rssService = rssService;
			this.aiSummaryService = aiSummaryService;
			this.emailService = emailService;
			this.newsLogService = newsLogService;
			this.newsItemRepository = newsItemRepository;
			this.destinatarios = destinatarios;
	}

	/** Ejecuta el flujo y devuelve el registro para facilitar la consulta desde REST. */
	public NewsLog ejecutarProcesoNoticias() {
			NewsLog log = newsLogService.iniciarEjecucion();
			try {
					List<RssItem> noticiasFeed = rssService.obtenerNoticias();
					if (noticiasFeed.isEmpty()) {
							return newsLogService.finalizarEjecucion(log, 0, null, null);
					}

					List<String> guids = noticiasFeed.stream()
									.map(RssItem::guid)
									.filter(guid -> guid != null && !guid.isBlank())
									.distinct()
									.toList();

					Set<String> guidsProcesados = newsItemRepository.findAllByGuidIn(guids).stream()
									.map(NewsItem::getGuid)
									.collect(Collectors.toSet());

					List<RssItem> noticiasNuevas = noticiasFeed.stream()
									.filter(noticia -> !guidsProcesados.contains(noticia.guid()))
									.collect(Collectors.toMap(
													RssItem::guid,
													noticia -> noticia,
													(primera, duplicada) -> primera,
													java.util.LinkedHashMap::new
									))
									.values().stream().toList();

					if (noticiasNuevas.isEmpty()) {
							return newsLogService.finalizarEjecucion(log, 0, null, null);
					}

					String resumen = aiSummaryService.generarResumen(noticiasNuevas);
					emailService.enviarResumen(resumen);

					List<NewsItem> entidades = noticiasNuevas.stream()
									.map(noticia -> convertir(noticia, log))
									.toList();
					newsItemRepository.saveAll(entidades);

					return newsLogService.finalizarEjecucion(
									log, noticiasNuevas.size(), resumen, destinatarios);
			} catch (RuntimeException e) {
					try {
							newsLogService.registrarError(
											log,
											e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
					} catch (RuntimeException errorRegistro) {
							e.addSuppressed(errorRegistro);
					}
					throw e;
			}
	}

	private NewsItem convertir(RssItem noticia, NewsLog log) {
			NewsItem entity = new NewsItem();
			entity.setGuid(noticia.guid());
			entity.setTitle(noticia.title());
			entity.setLink(noticia.link());
			entity.setDescription(noticia.description());
			entity.setPublicationDate(noticia.publicationDate());
			entity.setCategory(noticia.category());
			entity.setImageUrl(noticia.imageUrl());
			entity.setNewsLog(log);
			return entity;
	}
}
