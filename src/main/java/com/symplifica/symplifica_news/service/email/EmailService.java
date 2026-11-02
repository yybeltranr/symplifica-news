package com.symplifica.symplifica_news.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Service
public class EmailService {
  private final JavaMailSender mailSender;
  private final String remitente;
  private final String destinatarios;
  private final String asunto;

  public EmailService(
          JavaMailSender mailSender,
          @Value("${spring.mail.username}") String remitente,
          @Value("${app.news.recipients}") String destinatarios,
          @Value("${app.news.subject}") String asunto) {

      this.mailSender = mailSender;
      this.remitente = remitente;
      this.destinatarios = destinatarios;
      this.asunto = asunto;
  }

  public void enviarResumen(String resumen) {
      if (resumen == null || resumen.isBlank()) {
          throw new IllegalArgumentException(
                  "El resumen del correo no puede estar vacío");
      }

      try {
          MimeMessage mensaje = mailSender.createMimeMessage();

          MimeMessageHelper helper = new MimeMessageHelper(
                  mensaje,
                  false,
                  StandardCharsets.UTF_8.name()
          );

          helper.setFrom(remitente);

          String[] correos = Arrays.stream(
                  destinatarios.split("[,;]")
          )
          .map(String::trim)
          .filter(correo -> !correo.isBlank())
          .toArray(String[]::new);

          if (correos.length == 0) {
              throw new IllegalStateException(
                      "No hay destinatarios configurados");
          }

          helper.setTo(correos);
          helper.setSubject(asunto);
          helper.setText(construirHtml(resumen), true);

          mailSender.send(mensaje);

      } catch (MessagingException e) {
          throw new IllegalStateException(
                  "No fue posible construir el correo electrónico", e);
      }
  }

  private String construirHtml(String resumen) {
      String contenido = resumen
              .replace("&", "&amp;")
              .replace("<", "&lt;")
              .replace(">", "&gt;")
              .replace("\"", "&quot;")
              .replace("'", "&#39;")
              .replace("\n", "<br>");

      return """
              <!DOCTYPE html>
              <html lang="es">
              <head>
                  <meta charset="UTF-8">
                  <title>Resumen de noticias</title>
              </head>
              <body style="font-family: Arial, sans-serif;
                            color: #333;
                            line-height: 1.6;">
                  <h2>Resumen de noticias de entretenimiento</h2>
                  <p>%s</p>
                  <hr>
                  <small>Correo generado automáticamente por Symplifica News.</small>
              </body>
              </html>
              """.formatted(contenido);
  }
}