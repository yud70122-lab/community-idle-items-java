package com.community.idle.repository;

import com.community.idle.entity.ItemDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemRepository extends ElasticsearchRepository<ItemDoc, Long> {
}
