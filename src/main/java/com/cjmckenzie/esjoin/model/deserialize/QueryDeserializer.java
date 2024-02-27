package com.cjmckenzie.esjoin.model.deserialize;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import jakarta.json.Json;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class QueryDeserializer extends JsonDeserializer<Query> {
  @Override
  public Query deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
    return Query._DESERIALIZER.deserialize(
        Json.createParser(new ByteArrayInputStream(jsonParser.readValueAsTree().toString().getBytes())),
        new JacksonJsonpMapper()
    );
  }
}
