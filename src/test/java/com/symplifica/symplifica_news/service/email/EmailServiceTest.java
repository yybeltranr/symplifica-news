
package com.symplifica.symplifica_news.service.email;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

  @Mock
  private JavaMailSender mailSender;

  private EmailService emailService;

  @BeforeEach
  void setUp() {
      emailService = new EmailService(
              mailSender,
              "remitente@gmail.com",
              "destino1@example.com,destino2@example.com",
              "Resumen de noticias"
      );
  }

  private MimeMessage prepararMensaje() {
      MimeMessage mensaje = new MimeMessage(
              Session.getInstance(new Properties())
      );

      when(mailSender.createMimeMessage()).thenReturn(mensaje);

      return mensaje;
  }

  @Test
  void debeConstruirYEnviarCorreoCorrectamente() throws Exception {

    MimeMessage mensaje = prepararMensaje();

    String resumen = "Primera noticia.\nSegunda noticia.";

    emailService.enviarResumen(resumen);
    mensaje.saveChanges();
    verify(mailSender, times(1)).send(mensaje);

    assertEquals(
            "Resumen de noticias",
            mensaje.getSubject()
    );

    assertEquals(
            "remitente@gmail.com",
            mensaje.getFrom()[0].toString()
    );

    assertEquals(
            2,
            mensaje.getAllRecipients().length
    );

    assertTrue(
      mensaje.isMimeType("text/html"),
      "El mensaje debería ser HTML. Tipo obtenido: "
              + mensaje.getContentType()
    );

    String contenido = (String) mensaje.getContent();

    assertTrue(contenido.contains("Primera noticia."));
    assertTrue(contenido.contains("Segunda noticia."));
    assertTrue(contenido.contains("Resumen de noticias de entretenimiento"));
  }

  @Test
  void debeAceptarUnSoloDestinatario() throws Exception {

      MimeMessage mensaje = prepararMensaje();

      emailService = new EmailService(
              mailSender,
              "remitente@gmail.com",
              "destino@example.com",
              "Resumen de noticias"
      );

      emailService.enviarResumen("Resumen de prueba");

      assertEquals(
              1,
              mensaje.getAllRecipients().length
      );

      verify(mailSender).send(mensaje);
  }

  @Test
  void debeRechazarResumenVacio() {

      assertThrows(
              IllegalArgumentException.class,
              () -> emailService.enviarResumen("")
      );

      verify(mailSender, never()).send(any(MimeMessage.class));
  }
}