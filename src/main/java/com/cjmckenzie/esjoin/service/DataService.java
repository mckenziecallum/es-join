package com.cjmckenzie.esjoin.service;

import com.cjmckenzie.esjoin.model.data.Customer;
import com.cjmckenzie.esjoin.model.data.Order;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.thedeanda.lorem.Lorem;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class DataService {

  private final EsService esService;
  private final ResourceLoader resourceLoader;
  private final ObjectMapper objectMapper;
  private final Lorem lorem;

  public DataService(EsService esService, ResourceLoader resourceLoader, ObjectMapper objectMapper, Lorem lorem) {
    this.esService = esService;
    this.resourceLoader = resourceLoader;
    this.objectMapper = objectMapper;
    this.lorem = lorem;
  }

  public void indexData(String index, String resourceLocation) throws IOException {
    Resource resource = resourceLoader.getResource("classpath:" + resourceLocation);
    ArrayNode arrayNode = objectMapper.readValue(resource.getInputStream(), ArrayNode.class);

    for (Iterator<JsonNode> it = arrayNode.elements(); it.hasNext(); ) {
      JsonNode node = it.next();
      if (node.isObject()) {
        esService.indexDocument(index, (ObjectNode) node);
      }
    }
  }

  public void generateCustomers(int count) {
    List<ObjectNode> objects = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Customer customer = new Customer(i + 1, lorem);
      objects.add(objectMapper.convertValue(customer, ObjectNode.class));
    }

    esService.bulkIndex("customers", objects);
  }

  public void generateOrders(int count, int customerCount) {
    List<ObjectNode> objects = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Order order = new Order(i + 1, customerCount);
      objects.add(objectMapper.convertValue(order, ObjectNode.class));
    }

    esService.bulkIndex("orders", objects);
  }

}
