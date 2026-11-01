package com.symplifica.symplifica_news.service.logging;

import com.symplifica.symplifica_news.entity.NewsLog;
import com.symplifica.symplifica_news.repository.NewsLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NewsLogServiceTest {
      @Mock
    private NewsLogRepository newsLogRepository;

    @InjectMocks
    private NewsLogService newsLogService;

    @Test
    void debeIniciarEjecucion() {
        when(newsLogRepository.save(any(NewsLog.class)))
                .thenAnswer(invocation -> {
                    NewsLog log = invocation.getArgument(0);
                    log.setId(1L);
                    return log;
                });

        NewsLog resultado = newsLogService.iniciarEjecucion();

        assertNotNull(resultado.getId());
        assertNotNull(resultado.getFechaInicio());
        assertEquals("RUNNING", resultado.getEstado());
        assertEquals(0, resultado.getCantidadNoticias());

        verify(newsLogRepository).save(any(NewsLog.class));
    }

    @Test
    void debeFinalizarEjecucion() {
        NewsLog log = new NewsLog();
        log.setEstado("RUNNING");

        when(newsLogRepository.save(any(NewsLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NewsLog resultado = newsLogService.finalizarEjecucion(
                log,
                5,
                "Resumen de prueba",
                "prueba@example.com"
        );

        assertEquals("SUCCESS", resultado.getEstado());
        assertEquals(5, resultado.getCantidadNoticias());
        assertEquals("Resumen de prueba", resultado.getResumen());
        assertEquals("prueba@example.com", resultado.getDestinatarios());
        assertNotNull(resultado.getFechaFin());

        verify(newsLogRepository).save(log);
    }

    @Test
    void debeRegistrarError() {
        NewsLog log = new NewsLog();
        log.setEstado("RUNNING");

        when(newsLogRepository.save(any(NewsLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NewsLog resultado = newsLogService.registrarError(
                log,
                "Error de conexión RSS"
        );

        assertEquals("FAILED", resultado.getEstado());
        assertEquals("Error de conexión RSS", resultado.getMensajeError());
        assertNotNull(resultado.getFechaFin());

        verify(newsLogRepository).save(log);
    }
}
