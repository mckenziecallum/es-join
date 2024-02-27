package com.cjmckenzie.esjoin.configuration;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.cjmckenzie.esjoin.model.deserialize.QueryDeserializer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class JacksonConfiguration {

  @Bean
  @Primary
  public ObjectMapper getObjectMapper() {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(Query.class, new QueryDeserializer());

    return new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .registerModule(module)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true);
  }

}
