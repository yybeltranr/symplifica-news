package com.symplifica.symplifica_news.service.rss;

public record RssItem(
        String title,
        String link,
        String description,
        String publicationDate
) {
}
