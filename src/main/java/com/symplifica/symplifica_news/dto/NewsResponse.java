package com.symplifica.symplifica_news.dto;

import java.time.OffsetDateTime;

public record NewsResponse(
        Long id,
        String guid,
        String title,
        String link,
        String description,
        String publicationDate,
        String category,
        String imageUrl,
        OffsetDateTime processedAt,
        Long newsLogId
) {}
