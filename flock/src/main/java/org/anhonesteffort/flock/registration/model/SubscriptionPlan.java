/*
 * Copyright (C) 2014 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.anhonesteffort.flock.registration.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * rhodey
 */
public abstract class SubscriptionPlan {

  @JsonProperty
  protected String accountId;

  @JsonProperty
  protected String productId;

  @JsonProperty
  protected String status;

  public SubscriptionPlan() { }

  public SubscriptionPlan(String accountId, String productId, String status) {
    this.accountId = accountId;
    this.productId = productId;
    this.status    = status;
  }

  public String getAccountId() {
    return accountId;
  }

  public String getProductId() {
    return productId;
  }

  public String getStatus() {
    return status;
  }

  public abstract boolean isActive();

  @Override
  public boolean equals(Object other) {
    if (other == null)                        return false;
    if (!(other instanceof SubscriptionPlan)) return false;

    SubscriptionPlan that = (SubscriptionPlan)other;

    return this.accountId.equals(that.accountId) &&
           this.productId.equals(that.productId) &&
           this.status.equals(that.status)       &&
           this.isActive() == that.isActive();
  }

  @Override
  public int hashCode() {
    return accountId.hashCode()  ^ productId.hashCode() ^ status.hashCode();
  }

}
