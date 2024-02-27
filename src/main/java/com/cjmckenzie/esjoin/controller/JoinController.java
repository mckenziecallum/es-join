package com.cjmckenzie.esjoin.controller;

import com.cjmckenzie.esjoin.model.JoinRequest;
import com.cjmckenzie.esjoin.model.JoinResponse;
import com.cjmckenzie.esjoin.service.DataService;
import com.cjmckenzie.esjoin.service.JoinService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class JoinController {

  private final JoinService joinService;
  private final DataService dataService;

  public JoinController(JoinService joinService, DataService dataService) {
    this.joinService = joinService;
    this.dataService = dataService;
  }

  @PostMapping
  public JoinResponse join(@RequestBody JoinRequest joinRequest) {
    return joinService.join(joinRequest);
  }

  @PostMapping("/data")
  public void addData() throws IOException {
    dataService.generateCustomers(1000);
    dataService.generateOrders(100_000, 1000);
  }
}
