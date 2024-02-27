package com.cjmckenzie.esjoin.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.ExistsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.cjmckenzie.esjoin.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class JoinService {
  private final EsService esService;
  private final MappingService mappingService;
  private final ObjectMapper objectMapper;

  public JoinService(EsService esService, MappingService mappingService, ObjectMapper objectMapper) {
    this.esService = esService;
    this.mappingService = mappingService;
    this.objectMapper = objectMapper;
  }

  public JoinResponse join(JoinRequest request) {
    if (request.getSize() == 0) {
      return JoinResponse.builder().hits(Collections.emptyList()).build();
    }

    JoinRequestParams join = request.getJoin();
    List<String> indices = List.of(join.getLeft().getIndex(), join.getRight().getIndex());

    Map<String, Map<String, String>> mapping = mappingService.getFields(indices);

    List<String> leftFieldTypes = getFieldTypes(mapping, join.getLeft());
    if (leftFieldTypes.size() > 1) {
      throw new RuntimeException(
          String.format("More than one field type for left found. Unable to perform join (%s)",
              String.join(",", leftFieldTypes)));
    } else if (leftFieldTypes.isEmpty()) {
      throw new RuntimeException("No field type for left found. Unable to perform join");
    }

    List<String> rightFieldTypes = getFieldTypes(mapping, join.getRight());
    if (rightFieldTypes.size() > 1) {
      throw new RuntimeException(
          String.format("More than one field type for right found. Unable to perform join (%s)",
              String.join(",", rightFieldTypes)));
    } else if (rightFieldTypes.isEmpty()) {
      throw new RuntimeException("No field type for right found. Unable to perform join");
    }

    Map<String, Long> fieldCounts = Stream.concat(
            join.getLeft().getInclude().stream(), join.getRight().getInclude().stream()
        )
        .collect(Collectors.groupingBy(FieldInclude::getOutputField, Collectors.counting()));

    Set<String> duplicateFields = fieldCounts.entrySet()
        .stream()
        .filter(entry -> entry.getValue() > 1)
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());

    if (!duplicateFields.isEmpty()) {
      throw new RuntimeException(
          String.format(
              "Duplicate fields in output. Use \"alias\" to add these values to the output. Duplicates: (%s)",
              String.join(",", duplicateFields)
          )
      );
    }

    List<ObjectNode> hits;
    switch (join.getType()) {
      case INNER -> hits = performInnerJoin(join.getLeft(), join.getRight(), rightFieldTypes.getFirst());
      case LEFT -> hits = performLeftJoin(request.getSize(), request.getOffset(),
          join.getLeft(), join.getRight(), rightFieldTypes.getFirst());
      default -> hits = new ArrayList<>();
    }

    return JoinResponse.builder().hits(hits).build();
  }

  private List<ObjectNode> performInnerJoin(JoinSide left, JoinSide right, String rightFieldType) {
    Query leftQuery = QueryBuilders.bool()
        .must(ExistsQuery.of(exists -> exists.field(left.getField()))._toQuery())
        .must(left.getQuery())
        .build()._toQuery();

    SearchRequest.Builder leftRequestBuilder = new SearchRequest.Builder()
        .query(leftQuery)
        .source(
            sourceConfig -> sourceConfig.filter(
                filter -> filter.includes(left.getFields())
            )
        );

    List<Hit<ObjectNode>> leftHits = esService.scrollSearch(List.of(left.getIndex()), leftRequestBuilder, ObjectNode.class, null);

    Set<String> values = leftHits.stream()
        .map(Hit::source)
        .filter(Objects::nonNull)
        .map(node -> node.get(left.getField()).asText())
        .collect(Collectors.toSet());

    Query rightQuery = QueryBuilders.bool()
        .must(ExistsQuery.of(exists -> exists.field(right.getField()))._toQuery())
        .must(TermsQuery.of(terms ->
            terms.field(right.getAggregateType(rightFieldType))
                .terms(fieldTerms -> fieldTerms.value(values.stream().map(FieldValue::of).toList()))
            )._toQuery()
        )
        .must(right.getQuery())
        .build()._toQuery();

    SearchRequest.Builder rightRequestBuilder = new SearchRequest.Builder()
        .query(rightQuery)
        .source(
            sourceConfig -> sourceConfig.filter(
                filter -> filter.includes(right.getFields())
            )
        );

    List<Hit<ObjectNode>> rightHits = esService.scrollSearch(List.of(right.getIndex()), rightRequestBuilder, ObjectNode.class, null);

    Map<String, List<ObjectNode>> mappedRightHits = rightHits.stream()
        .map(Hit::source)
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(node -> node.get(right.getField()).asText()));

    List<ObjectNode> response = new ArrayList<>();
    for (Hit<ObjectNode> hit : leftHits) {
      ObjectNode leftObject = hit.source();
      if (leftObject == null) continue;
      String leftFieldValue = leftObject.get(left.getField()).asText();

      List<ObjectNode> rightObjects = mappedRightHits.getOrDefault(leftFieldValue, Collections.emptyList());
      for (ObjectNode rightObject : rightObjects) {
        ObjectNode output = objectMapper.createObjectNode();
        output.set("left", leftObject);
        output.set("right", rightObject);

        response.add(output);
      }

      // Do we cache and paginate
    }

    return response;
  }

  private List<ObjectNode> performLeftJoin(int size, int offset, JoinSide left,
                                           JoinSide right, String rightFieldType) {
    Query leftQuery = QueryBuilders.bool()
        .must(ExistsQuery.of(exists -> exists.field(left.getField()))._toQuery())
        .must(left.getQuery())
        .build()._toQuery();

    SearchRequest.Builder leftRequestBuilder = new SearchRequest.Builder()
        .query(leftQuery)
        .source(
            sourceConfig -> sourceConfig.filter(
                filter -> filter.includes(left.getFields())
            )
        );

    List<Hit<ObjectNode>> leftHits = esService.scrollSearch(List.of(left.getIndex()), leftRequestBuilder, ObjectNode.class, null);
    Set<String> values = leftHits.stream()
        .map(Hit::source)
        .filter(Objects::nonNull)
        .map(node -> node.get(left.getField()).asText())
        .collect(Collectors.toSet());

    Query rightQuery = QueryBuilders.bool()
        .must(ExistsQuery.of(exists -> exists.field(right.getField()))._toQuery())
        .must(TermsQuery.of(terms ->
                terms.field(right.getAggregateType(rightFieldType))
                    .terms(fieldTerms -> fieldTerms.value(values.stream().map(FieldValue::of).toList()))
            )._toQuery()
        )
        .must(right.getQuery())
        .build()._toQuery();

    SearchRequest.Builder rightRequestBuilder = new SearchRequest.Builder()
        .query(rightQuery)
        .source(
            sourceConfig -> sourceConfig.filter(
                filter -> filter.includes(right.getFields())
            )
        )
        .size(size)
        .from(offset);

    List<Hit<ObjectNode>> rightHits = esService.scrollSearch(List.of(right.getIndex()), rightRequestBuilder, ObjectNode.class, size);

    Map<String, List<ObjectNode>> mappedRightHits = rightHits.stream()
        .map(Hit::source)
        .filter(Objects::nonNull)
        .collect(Collectors.groupingBy(node -> node.get(right.getField()).asText()));

    List<ObjectNode> response = new ArrayList<>();
    for (Hit<ObjectNode> hit : leftHits) {
      ObjectNode leftObject = hit.source();
      if (leftObject == null) continue;
      String leftFieldValue = leftObject.get(left.getField()).asText();

      List<ObjectNode> rightObjects = mappedRightHits.getOrDefault(leftFieldValue, Collections.emptyList());
      if (rightObjects.isEmpty()) {
        ObjectNode output = objectMapper.createObjectNode();
        output.set("left", leftObject);
        output.set("right", NullNode.getInstance());

        response.add(output);
      }
      for (ObjectNode rightObject : rightObjects) {
        ObjectNode output = objectMapper.createObjectNode();
        output.set("left", leftObject);
        output.set("right", rightObject);

        response.add(output);
      }
    }

    return response;
  }

  private List<String> getFieldTypes(Map<String, Map<String, String>> mapping, JoinSide joinSide) {
    List<String> indices = getIndicesContainingField(mapping, joinSide);

    return mapping.entrySet()
        .stream()
        .filter((entry) -> indices.contains(entry.getKey()))
        .map(Map.Entry::getValue)
        .map(fields -> fields.get(joinSide.getField()))
        .distinct()
        .toList();
  }

  private List<String> getIndicesContainingField(Map<String, Map<String, String>> mapping, JoinSide joinSide) {
    if (!joinSide.getIndex().contains("*")) {
      Map<String, String> fields = mapping.getOrDefault(joinSide.getIndex(), Collections.emptyMap());

      if (fields.containsKey(joinSide.getField())) {
        return List.of(joinSide.getIndex());
      }

      return Collections.emptyList();
    }

    String indexRegex = joinSide.getIndex().replace("*", ".*");
    Pattern pattern = Pattern.compile(indexRegex);

    return mapping.entrySet()
        .stream()
        .filter((entry) -> {
          Matcher matcher = pattern.matcher(entry.getKey());
          return matcher.matches();
        })
        .filter((entry) -> entry.getValue().containsKey(joinSide.getField()))
        .map(Map.Entry::getKey)
        .toList();
  }

}
