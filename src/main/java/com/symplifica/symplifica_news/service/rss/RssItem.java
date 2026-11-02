package com.symplifica.symplifica_news.service.rss;

public record RssItem(
        String title,
        String link,
        String description,
        String publicationDate,
        String guid,
        String category,
        String imageUrl
) {
    public RssItem {
        title = title == null ? "" : title.trim();
        link = link == null ? "" : link.trim();
        description = description == null ? "" : description.trim();
        publicationDate = publicationDate == null ? "" : publicationDate.trim();
        category = category == null ? "" : category.trim();
        imageUrl = imageUrl == null ? "" : imageUrl.trim();

        if (guid == null || guid.isBlank()) {
            guid = !link.isBlank() ? link : title + "|" + publicationDate;
        } else {
            guid = guid.trim();
        }
    }

    public RssItem(String title, String link, String description, String publicationDate) {
        this(title, link, description, publicationDate, link, "", "");
    }
}