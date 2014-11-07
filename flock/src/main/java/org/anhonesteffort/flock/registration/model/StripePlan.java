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

/**
 * rhodey.
 */
public class StripePlan extends SubscriptionPlan {

  public static final String STATUS_TRIALING = "trialing";
  public static final String STATUS_ACTIVE   = "active";
  public static final String STATUS_PAST_DUE = "past_due";
  public static final String STATUS_CANCELED = "canceled";
  public static final String STATUS_UNPAID   = "unpaid";

  public StripePlan() { }

  public StripePlan(String accountId, String planId, String status) {
    super(accountId, planId, status);
  }

  @Override
  public boolean isActive() {
    return !(this.status.equals(STATUS_CANCELED) || this.status.equals(STATUS_UNPAID));
  }

}