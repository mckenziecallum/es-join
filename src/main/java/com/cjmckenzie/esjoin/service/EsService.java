package com.cjmckenzie.esjoin.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Service
public class EsService {

  private final ElasticsearchClient client;

  public EsService(ElasticsearchClient client) {
    this.client = client;
  }

  public String indexDocument(String index, ObjectNode object) {
    IndexRequest<ObjectNode> indexRequest = IndexRequest.of(
        request -> request.index(index).document(object)
    );

    try {
      IndexResponse indexResponse = client.index(indexRequest);

      return indexResponse.id();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void bulkIndex(String index, List<ObjectNode> objects) {
    List<BulkOperation> requests = objects.stream()
        .map(
            object -> BulkOperation.of(operation ->
                operation.<ObjectNode>index(
                    indexOperation -> indexOperation.index(index).document(object)
                )
            )
        )
        .toList();

    BulkRequest bulkRequest = BulkRequest.of(request ->
        request.index(index)
            .operations(requests)
    );

    try {
      BulkResponse response = client.bulk(bulkRequest);
      if(response.errors()) {
        log.error("Errors found");
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public <T> List<Hit<T>> scrollSearch(List<String> indices,
                                       SearchRequest.Builder searchRequestBuilder,
                                       Class<T> responseType, Integer limit) {
    Time scrollTime = Time.of(time -> time.time("5m"));
    SearchRequest searchRequest = searchRequestBuilder
        .index(indices)
        .scroll(scrollTime)
        .build();

    SearchResponse<T> response;
    try {
      response = client.search(searchRequest, responseType);
    } catch (IOException ex) {
      throw new RuntimeException("Error occurred while searching OpenSearch", ex);
    }

    String scrollId = response.scrollId();

    List<Hit<T>> allHits = new ArrayList<>();

    HitsMetadata<T> hits = response.hits();

    while (hits != null && !hits.hits().isEmpty()) {
      String finalScrollId = scrollId;
      allHits.addAll(hits.hits());

      ScrollRequest scrollRequest = ScrollRequest.of(scroll -> scroll.scrollId(finalScrollId).scroll(scrollTime));

      try {
        ScrollResponse<T> scrollResponse = client.scroll(scrollRequest, responseType);
        hits = scrollResponse.hits();
        scrollId = scrollResponse.scrollId();

        if (limit != null && allHits.size() >= limit) {
          break;
        }
      } catch (IOException ex) {
        throw new RuntimeException("Error occurred while scrolling results with scroll ID: " + scrollId);
      }
    }

    String finalScrollId = scrollId;
    ClearScrollRequest clearScrollRequest = ClearScrollRequest.of(scroll -> scroll.scrollId(finalScrollId));

    int attempts = 0;
    do {
      ClearScrollResponse clearScrollResponse;
      try {
        clearScrollResponse = client.clearScroll(clearScrollRequest);
      } catch (IOException ex) {
        throw new RuntimeException("Error occurred while clearing scroll with ID: " + scrollId, ex);
      }
      if (clearScrollResponse.succeeded()) {
        break;
      }

      attempts++;
    } while (attempts < 3);

    if (limit != null) {
      return allHits.subList(0, limit);
    }
    return allHits;
  }

}
