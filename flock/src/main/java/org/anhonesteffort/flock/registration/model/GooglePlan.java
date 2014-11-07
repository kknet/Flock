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
public class GooglePlan extends SubscriptionPlan {

  public static final String STATUS_PURCHASED = "0";
  public static final String STATUS_CANCELLED = "1";

  @JsonProperty
  protected String productKind;

  @JsonProperty
  protected String purchaseToken;

  public GooglePlan() { }

  public GooglePlan(String  accountId,
                    String  productKind,
                    String  productId,
                    String  purchaseToken,
                    Integer purchaseState)
  {
    super(accountId, productId, purchaseState.toString());

    this.productKind   = productKind;
    this.purchaseToken = purchaseToken;
  }

  @Override
  public boolean isActive() {
    return this.status.equals(STATUS_PURCHASED);
  }

  @Override
  public boolean equals(Object other) {
    if (other == null)                  return false;
    if (!(other instanceof GooglePlan)) return false;
    if (!super.equals(other))           return false;

    GooglePlan that = (GooglePlan)other;

    return this.productKind.equals(that.productKind) &&
           this.purchaseToken.equals(that.purchaseToken);
  }

  @Override
  public int hashCode() {
    return super.hashCode() ^ productKind.hashCode() ^ purchaseToken.hashCode();
  }

}
