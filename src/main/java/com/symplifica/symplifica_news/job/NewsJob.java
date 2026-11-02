package com.symplifica.symplifica_news.job;

import com.symplifica.symplifica_news.service.NewsOrchestrationService;

import org.jobrunr.jobs.annotations.Recurring;
import org.springframework.stereotype.Component;

@Component
public class NewsJob {
  private final NewsOrchestrationService orchestrationService;

  public NewsJob(NewsOrchestrationService orchestrationService) {
      this.orchestrationService = orchestrationService;
  }

  @Recurring(
    // id del job, debe ser único para cada job recurrente
    id = "symplifica-news-hourly",
    // se programa para que se ejecute cada hora en el minuto 0, zona horaria de Bogotá, para pruebas cada minuto
    cron = "${app.news.job-cron:0 * * * *}",
    zoneId = "America/Bogota"
  )
  public void ejecutarNoticiasCadaHora() {
      orchestrationService.ejecutarProcesoNoticias();
  }
}
