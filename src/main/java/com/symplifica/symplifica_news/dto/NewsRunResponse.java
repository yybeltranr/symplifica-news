package com.symplifica.symplifica_news.dto;

import java.time.OffsetDateTime;

public record NewsRunResponse(
        Long id,
        String status,
        Integer newsCount,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String summary,
        String errorMessage,
        String message
) {}
