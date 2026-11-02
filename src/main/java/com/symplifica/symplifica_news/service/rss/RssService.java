package com.symplifica.symplifica_news.service.rss;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service 
public class RssService {
  private final RestClient restClient;
  private final String rssUrl;

	// Constructor principal que recibe el RestClient y la URL del RSS
  @Autowired
  public RssService(
          @Value("${app.news.rss-url}") String rssUrl) {

      this(RestClient.builder().build(), rssUrl);
  }

  //Constructor de pruebas (no depende de internet)
  RssService(RestClient restClient, String rssUrl) {
    this.restClient = restClient;
    this.rssUrl = rssUrl;
  }

  public List<RssItem> obtenerNoticias() {

      String xml = restClient.get()
              .uri(rssUrl)
              .retrieve()
              .body(String.class);

      if (xml == null || xml.isBlank()) {
          return List.of();
      }

      return analizarXml(xml);
  }

  List<RssItem> analizarXml(String xml) {

      List<RssItem> noticias = new ArrayList<>();

      try {
          DocumentBuilderFactory factory =
                  DocumentBuilderFactory.newInstance();

          factory.setFeature(
                  "http://apache.org/xml/features/disallow-doctype-decl",
                  true
          );

          factory.setFeature(
                  "http://xml.org/sax/features/external-general-entities",
                  false
          );

          factory.setFeature(
                  "http://xml.org/sax/features/external-parameter-entities",
                  false
          );

          factory.setAttribute(
                  XMLConstants.ACCESS_EXTERNAL_DTD,
                  ""
          );

          factory.setAttribute(
                  XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                  ""
          );

          Document document = factory.newDocumentBuilder()
                  .parse(new ByteArrayInputStream(
                          xml.getBytes(StandardCharsets.UTF_8)
                  ));

          NodeList items = document.getElementsByTagName("item");

          for (int i = 0; i < items.getLength(); i++) {

              Element item = (Element) items.item(i);

              String title = obtenerTexto(item, "title");
              String link = obtenerTexto(item, "link");
              String description = obtenerTexto(item, "description");
              String publicationDate = obtenerTexto(item, "pubDate");

              if (!title.isBlank()) {
                  noticias.add(new RssItem(
                          title,
                          link,
                          description,
                          publicationDate
                  ));
              }
          }

      } catch (Exception e) {
          throw new IllegalStateException(
                  "No fue posible analizar el XML del RSS", e
          );
      }

      return noticias;
  }

  private String obtenerTexto(Element elemento, String etiqueta) {

      NodeList nodos = elemento.getElementsByTagName(etiqueta);

      if (nodos.getLength() == 0
              || nodos.item(0).getTextContent() == null) {
          return "";
      }
      return nodos.item(0).getTextContent().trim();
  }
}