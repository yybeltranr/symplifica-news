package com.symplifica.symplifica_news.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.OffsetDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ProblemDetail> manejarNoEncontrado(
          EntityNotFoundException ex, HttpServletRequest request) {
      return respuesta(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDetail> manejarSolicitudInvalida(
          IllegalArgumentException ex, HttpServletRequest request) {
      return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> manejarErrorGeneral(
          Exception ex, HttpServletRequest request) {
      log.error("Error procesando la solicitud {}", request.getRequestURI(), ex);
      return respuesta(HttpStatus.INTERNAL_SERVER_ERROR,
              "Ocurrió un error inesperado al procesar la solicitud.", request);
  }

  private ResponseEntity<ProblemDetail> respuesta(
    HttpStatus status, String detail, HttpServletRequest request) {
      ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
      problem.setTitle(status.getReasonPhrase());
      problem.setInstance(URI.create(request.getRequestURI()));
      problem.setProperty("timestamp", OffsetDateTime.now());
      return ResponseEntity.status(status).body(problem);
  }
}
