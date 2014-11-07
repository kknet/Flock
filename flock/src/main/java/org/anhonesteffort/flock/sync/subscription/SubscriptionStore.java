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

package org.anhonesteffort.flock.sync.subscription;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.common.base.Optional;

import org.anhonesteffort.flock.registration.model.FlockCardInformation;
import org.anhonesteffort.flock.registration.model.GooglePlan;
import org.anhonesteffort.flock.registration.model.StripePlan;
import org.anhonesteffort.flock.registration.model.SubscriptionPlan;

import java.util.Date;

/**
 * rhodey
 */
public class SubscriptionStore {

  public static final int PLAN_TYPE_NONE   = 0;
  public static final int PLAN_TYPE_GOOGLE = 1;
  public static final int PLAN_TYPE_STRIPE = 2;

  private static final String KEY_LAST_CHARGE_FAILED          = "SubscriptionStore.KEY_LAST_CHARGE_FAILED";
  private static final String KEY_DAYS_REMAINING_TIMESTAMP    = "SubscriptionStore.KEY_DAYS_REMAINING_TIMESTAMP";
  private static final String KEY_DAYS_REMAINING              = "SubscriptionStore.KEY_DAYS_REMAINING";
  private static final String KEY_ACTIVE_SUBSCRIPTION_PLAN    = "SubscriptionStore.KEY_ACTIVE_SUBSCRIPTION_PLAN";
  private static final String KEY_CARD_INFORMATION_ACCOUNT_ID = "SubscriptionStore.KEY_CARD_INFORMATION_ACCOUNT_ID";
  private static final String KEY_CARD_INFORMATION_LAST_FOUR  = "SubscriptionStore.KEY_CARD_INFORMATION_LAST_FOUR";
  private static final String KEY_CARD_INFORMATION_EXPIRATION = "SubscriptionStore.KEY_CARD_INFORMATION_EXPIRATION";

  private static SharedPreferences getStore(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context);
  }

  public static boolean getLastChargeFailed(Context context) {
    return getStore(context).getBoolean(KEY_LAST_CHARGE_FAILED, false);
  }

  public static void setLastChargeFailed(Context context, boolean failed) {
    getStore(context).edit()
        .putBoolean(KEY_LAST_CHARGE_FAILED, failed)
        .apply();
  }

  public static Optional<Long> getDaysRemaining(Context context) {
    Long timestamp     = getStore(context).getLong(KEY_DAYS_REMAINING_TIMESTAMP, -1L);
    Long daysRemaining = getStore(context).getLong(KEY_DAYS_REMAINING,           -1L);

    if (timestamp <= 0L || daysRemaining <= 0L)
      return Optional.absent();

    long daysSinceStore = (new Date().getTime() - timestamp) / 1000L / 60L / 60L / 24L;

    return Optional.of((daysRemaining - daysSinceStore));
  }

  public static void setDaysRemaining(Context context, long daysRemaining) {
    getStore(context).edit()
        .putLong(KEY_DAYS_REMAINING,           daysRemaining)
        .putLong(KEY_DAYS_REMAINING_TIMESTAMP, new Date().getTime())
        .apply();
  }

  public static Optional<FlockCardInformation> getCardInformation(Context context) {
    String accountId  = getStore(context).getString(KEY_CARD_INFORMATION_ACCOUNT_ID, null);
    String lastFour   = getStore(context).getString(KEY_CARD_INFORMATION_LAST_FOUR,  null);
    String expiration = getStore(context).getString(KEY_CARD_INFORMATION_EXPIRATION, null);

    if (accountId == null || lastFour == null || expiration == null)
      return Optional.absent();

    return Optional.of(
        new FlockCardInformation(accountId, lastFour, expiration)
    );
  }

  public static void setCardInformation(Context                        context,
                                        Optional<FlockCardInformation> cardInformation)
  {
    if (cardInformation.isPresent()) {
      getStore(context).edit()
          .putString(KEY_CARD_INFORMATION_ACCOUNT_ID, cardInformation.get().getAccountId())
          .putString(KEY_CARD_INFORMATION_LAST_FOUR,  cardInformation.get().getCardLastFour())
          .putString(KEY_CARD_INFORMATION_EXPIRATION, cardInformation.get().getCardExpiration())
          .apply();
    }
    else {
      getStore(context).edit()
          .putString(KEY_CARD_INFORMATION_ACCOUNT_ID, null)
          .putString(KEY_CARD_INFORMATION_LAST_FOUR,  null)
          .putString(KEY_CARD_INFORMATION_EXPIRATION, null)
          .apply();
    }
  }

  public static int getSubscriptionPlanType(SubscriptionPlan plan) {
    int planType = PLAN_TYPE_NONE;

    if (plan instanceof GooglePlan)
      planType = PLAN_TYPE_GOOGLE;
    else if (plan instanceof StripePlan)
      planType = PLAN_TYPE_STRIPE;

    return planType;
  }

  public static int getActiveSubscriptionPlanType(Context context) {
    return getStore(context).getInt(KEY_ACTIVE_SUBSCRIPTION_PLAN, PLAN_TYPE_NONE);
  }

  public static void setActiveSubscriptionPlanType(Context context, int planType) {
    getStore(context).edit()
        .putInt(KEY_ACTIVE_SUBSCRIPTION_PLAN, planType)
        .apply();
  }

}
