package com.symplifica.symplifica_news.service.logging;

import com.symplifica.symplifica_news.entity.NewsLog;
import com.symplifica.symplifica_news.repository.NewsLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;

@Service
public class NewsLogService {
  private final NewsLogRepository newsLogRepository;

    public NewsLogService(NewsLogRepository newsLogRepository) {
        this.newsLogRepository = newsLogRepository;
    }

    @Transactional
    public NewsLog iniciarEjecucion() {
        NewsLog log = new NewsLog();

        log.setFechaInicio(OffsetDateTime.now());
        log.setEstado("RUNNING");
        log.setCantidadNoticias(0);

        return newsLogRepository.save(log);
    }

    @Transactional
    public NewsLog finalizarEjecucion(
            NewsLog log,
            int cantidadNoticias,
            String resumen,
            String destinatarios) {

        log.setFechaFin(OffsetDateTime.now());
        log.setEstado("SUCCESS");
        log.setCantidadNoticias(cantidadNoticias);
        log.setResumen(resumen);
        log.setDestinatarios(destinatarios);

        return newsLogRepository.save(log);
    }

    @Transactional
    public NewsLog registrarError(NewsLog log, String mensajeError) {

        log.setFechaFin(OffsetDateTime.now());
        log.setEstado("FAILED");
        log.setMensajeError(mensajeError);

        return newsLogRepository.save(log);
    }
}
