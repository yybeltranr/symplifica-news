package com.symplifica.symplifica_news.repository;

import com.symplifica.symplifica_news.entity.NewsItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {
    List<NewsItem> findAllByGuidIn(Collection<String> guids);
    List<NewsItem> findTop100ByOrderByFechaProcesamientoDesc();
}
