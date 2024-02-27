package com.cjmckenzie.esjoin.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch.indices.GetMappingRequest;
import co.elastic.clients.elasticsearch.indices.GetMappingResponse;
import co.elastic.clients.elasticsearch.indices.get_mapping.IndexMappingRecord;
import com.cjmckenzie.esjoin.exception.EsException;
import com.cjmckenzie.esjoin.model.MappingType;
import io.micrometer.common.util.StringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
public class MappingService {

  private final ElasticsearchClient client;

  public MappingService(ElasticsearchClient client) {
    this.client = client;
  }

  public Map<String, Map<String, String>> getFields(List<String> indices) throws EsException {
    GetMappingRequest request = GetMappingRequest.of(builder -> builder.index(indices));

    try {
      GetMappingResponse response = client.indices().getMapping(request);

      Map<String, Map<String, String>> indexMappings = new HashMap<>();

      for (String index : response.result().keySet()) {
        IndexMappingRecord mappings = response.get(index);

        if (mappings == null) {
          continue;
        }

        indexMappings.put(index, getFieldsFromMapping(mappings.mappings().properties()));
      }

      return indexMappings;
    } catch (IOException e) {
      throw new EsException(e);
    }
  }

  private Map<String, String> getFieldsFromMapping(Map<String, Property> fieldMapping) {
    Map<String, String> fieldsWithResolvedAliases = new HashMap<>();
    Map<String, MappingType> fields = getFieldsFromMapping("", fieldMapping);
    fields.forEach((key, value) -> {
      if (value.isAliasType()) {
        fieldsWithResolvedAliases.put(key, resolveAliasType(value, fields));
      } else {
        fieldsWithResolvedAliases.put(key, value.getType());
      }
    });

    return fieldsWithResolvedAliases;
  }

  private String resolveAliasType(MappingType type, Map<String, MappingType> fields) {
    MappingType aliasType = fields.get(type.getAliasPath());
    if (aliasType == null) {
      log.error("Could not find true type for aliased field {}: path {}", type.getType(), type.getAliasPath());
      throw new EsException();
    }
    return aliasType.getType();
  }

  private Map<String, MappingType> getFieldsFromMapping(String parent, Map<String, Property> fieldMapping) {
    Map<String, MappingType> fields = new HashMap<>();
    for (Map.Entry<String, Property> field : fieldMapping.entrySet()) {
      String fieldName = field.getKey();
      if (StringUtils.isNotEmpty(parent)) {
        fieldName = String.format("%s.%s", parent, fieldName);
      }

      Property property = field.getValue();
      if (property.isObject()) {
        fields.putAll(getFieldsFromMapping(fieldName, field.getValue().object().properties()));
      } else if (property.isAlias()) {
        fields.put(fieldName, MappingType.builder()
                .type(property._kind().jsonValue())
                .aliasPath(property.alias().path())
            .build());
      } else {
        fields.put(fieldName, MappingType.builder().type(property._kind().jsonValue()).build());
      }

      if (property.isText()) {
        TextProperty textProperty = property.text();
        fields.putAll(getFieldsFromMapping(fieldName, textProperty.fields()));
      } else if (property.isKeyword()) {
        KeywordProperty keywordProperty = property.keyword();
        fields.putAll(getFieldsFromMapping(fieldName, keywordProperty.fields()));
      }
    }

    return fields;
  }
}
