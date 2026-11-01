package com.symplifica.symplifica_news.repository;

import com.symplifica.symplifica_news.entity.NewsLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsLogRepository extends JpaRepository<NewsLog, Long> {
}
