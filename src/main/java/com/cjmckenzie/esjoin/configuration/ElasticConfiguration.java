package com.cjmckenzie.esjoin.configuration;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticConfiguration {

  @Bean
  public ElasticsearchClient getClient(ObjectMapper objectMapper) {
    RestClient restClient = RestClient.builder(HttpHost.create("http://localhost:9200")).build();
    ElasticsearchTransport transport = new RestClientTransport(
        restClient, new JacksonJsonpMapper(objectMapper)
    );

    return new ElasticsearchClient(transport);
  }
}
