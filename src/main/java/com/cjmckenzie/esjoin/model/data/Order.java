package com.cjmckenzie.esjoin.model.data;

import lombok.Data;

import java.util.Random;

@Data
public class Order {

  private int id;
  private int customerId;
  private int quantity;
  private String item;

  public Order(int id, int maxCustomerId) {
    Random random = new Random();

    this.id = id;
    this.customerId = random.nextInt(maxCustomerId) + 1;
    this.quantity = random.nextInt();
    this.item = "Item from Order " + this.id;
  }

}
