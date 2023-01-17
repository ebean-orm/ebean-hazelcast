package org.example.domain;

import io.ebean.annotation.Cache;

import javax.persistence.Entity;

@Cache(enableQueryCache = true, nearCache = true)
@Entity
public class EConfig extends EBase {
  private boolean option1;

  public boolean isOption1() {
    return option1;
  }

  public void setOption1(boolean option1) {
    this.option1 = option1;
  }
}
