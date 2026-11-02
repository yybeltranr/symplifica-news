package com.symplifica.symplifica_news.service.rss;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.http.HttpMethod.GET;

public class RssServiceTest {
  private static final String RSS_URL = "http://localhost/rss";

  private MockRestServiceServer mockServer;
  private RssService rssService;

  @BeforeEach
  void configurar() {

      RestClient.Builder builder = RestClient.builder();

      mockServer = MockRestServiceServer
              .bindTo(builder)
              .build();

      rssService = new RssService(
              builder.build(),
              RSS_URL
      );
  }

  @Test
  void debeObtenerNoticiasDesdeXml() {

      String xml = """
              <?xml version="1.0" encoding="UTF-8"?>
              <rss version="2.0">
                  <channel>
                      <item>
                          <title>Noticia de prueba</title>
                          <link>https://ejemplo.com/noticia</link>
                          <description>Descripción de prueba</description>
                          <guid>guid-noticia-1</guid>
                          <category>Entretenimiento</category>
                          <enclosure type="image/jpeg" url="https://ejemplo.com/imagen.jpg"/>
                          <pubDate>Sun, 01 Nov 2026 10:00:00 GMT</pubDate>
                      </item>
                  </channel>
              </rss>
              """;

      mockServer.expect(requestTo(RSS_URL))
              .andExpect(method(GET))
              .andRespond(withSuccess(
        xml,
        MediaType.parseMediaType("application/xml;charset=UTF-8")
));

      List<RssItem> noticias = rssService.obtenerNoticias();

      assertEquals(1, noticias.size());
      assertEquals("Noticia de prueba", noticias.get(0).title());
      assertEquals(
              "https://ejemplo.com/noticia",
              noticias.get(0).link()
      );
      assertEquals(
              "Descripción de prueba",
              noticias.get(0).description()
      );
			assertEquals("guid-noticia-1", noticias.get(0).guid());
      assertEquals("Entretenimiento", noticias.get(0).category());
      assertEquals("https://ejemplo.com/imagen.jpg", noticias.get(0).imageUrl());

      mockServer.verify();
  }

  @Test
  void debeRetornarListaVaciaCuandoXmlNoTieneNoticias() {

      String xml = """
              <?xml version="1.0" encoding="UTF-8"?>
              <rss version="2.0">
                  <channel></channel>
              </rss>
              """;

      mockServer.expect(requestTo(RSS_URL))
              .andExpect(method(GET))
              .andRespond(withSuccess(
        xml,
        MediaType.parseMediaType("application/xml;charset=UTF-8")
));

      List<RssItem> noticias = rssService.obtenerNoticias();

      assertTrue(noticias.isEmpty());

      mockServer.verify();
  }

  @Test
  void debeLanzarErrorCuandoFallaLaPeticion() {

      mockServer.expect(requestTo(RSS_URL))
              .andExpect(method(GET))
              .andRespond(withServerError());

      assertThrows(
              RuntimeException.class,
              () -> rssService.obtenerNoticias()
      );

      mockServer.verify();
  }

	@Test
  void debeRechazarUnaRespuestaHtmlEnLugarDeXml() {
      mockServer.expect(requestTo(RSS_URL))
              .andExpect(method(GET))
              .andRespond(withSuccess(
                      "<html><body><hr></body></html>",
                      MediaType.TEXT_HTML
              ));

      IllegalStateException error = assertThrows(
              IllegalStateException.class,
              () -> rssService.obtenerNoticias()
      );

      assertTrue(error.getMessage().contains("tipo de contenido no XML"));
      mockServer.verify();
  }
}
