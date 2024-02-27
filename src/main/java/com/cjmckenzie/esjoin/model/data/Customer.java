package com.cjmckenzie.esjoin.model.data;

import com.thedeanda.lorem.Lorem;
import lombok.Data;

@Data
public class Customer {

  private int id;
  private String firstName;
  private String lastName;
  private String phoneNumber;
  private String city;
  private String country;

  public Customer(int id, Lorem lorem) {
    this.id = id;
    this.firstName = lorem.getFirstName();
    this.lastName = lorem.getLastName();
    this.phoneNumber = lorem.getPhone();
    this.city = lorem.getCity();
    this.country = lorem.getCountry();
  }

}
