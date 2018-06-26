package org.example.domain;


import io.ebean.annotation.Cache;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.List;

@Cache(enableQueryCache = true)
@Entity
public class ECustomer extends EBase {

  public enum Status {
    NEW,
    ACTIVE,
    INACTIVE
  }

  Status status;

  String name;

  String notes;

  @ManyToOne(cascade = CascadeType.ALL)
  EAddress address;

  @OneToMany
  List<EOrder> orders;

  public ECustomer(String name) {
    this.name = name;
    this.status = Status.NEW;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public EAddress getAddress() {
    return address;
  }

  public void setAddress(EAddress address) {
    this.address = address;
  }

  public List<EOrder> getOrders() {
    return orders;
  }

  public void setOrders(List<EOrder> orders) {
    this.orders = orders;
  }
}
