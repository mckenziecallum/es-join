package com.cjmckenzie.esjoin.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JoinResponse {
  private List<ObjectNode> hits;
}
