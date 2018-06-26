package org.example.domain;

import io.ebean.annotation.Cache;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Cache(enableQueryCache = true)
@Entity
public class EOrder extends EBase {

  @ManyToOne(optional = false)
  private ECustomer customer;

  private String description;

  public EOrder(ECustomer customer) {
    this.customer = customer;
  }


  public ECustomer getCustomer() {
    return customer;
  }

  public void setCustomer(ECustomer customer) {
    this.customer = customer;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
