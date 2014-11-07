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
import android.content.SyncResult;
import android.util.Log;

import org.anhonesteffort.flock.auth.DavAccount;
import org.anhonesteffort.flock.registration.RegistrationApi;
import org.anhonesteffort.flock.registration.RegistrationApiException;
import org.anhonesteffort.flock.registration.model.AugmentedFlockAccount;
import org.anhonesteffort.flock.registration.model.SubscriptionPlan;
import org.anhonesteffort.flock.sync.SyncWorker;
import org.anhonesteffort.flock.sync.SyncWorkerUtil;

import java.io.IOException;
import java.util.List;

/**
 * rhodey
 */
public class SubscriptionSyncWorker implements SyncWorker {

  private static final String TAG = "org.anhonesteffort.flock.sync.subscription.SubscriptionSyncWorker";

  private final Context         context;
  private final DavAccount      account;
  private final SyncResult      result;
  private final RegistrationApi registration;

  public SubscriptionSyncWorker(Context context, DavAccount account, SyncResult syncResult) {
    this.context      = context;
    this.account      = account;
    this.result       = syncResult;

    registration = new RegistrationApi(context);
  }

  private void handleUpdateFlockAccountCache() {
    Log.d(TAG, "handleUpdateFlockAccountCache");

    try {

      AugmentedFlockAccount flockAccount = registration.getAccount(account);

      SubscriptionStore.setDaysRemaining(context,    flockAccount.getDaysRemaining());
      SubscriptionStore.setLastChargeFailed(context, flockAccount.getLastStripeChargeFailed());

    } catch (RegistrationApiException e) {
      SyncWorkerUtil.handleException(context, e, result);
    } catch (IOException e) {
      SyncWorkerUtil.handleException(context, e, result);
    }
  }

  private void handleUpdateCardInformationCache() {
    Log.d(TAG, "handleUpdateCardInformationCache");

    try {

      SubscriptionStore.setCardInformation(context, registration.getCard(account));

    } catch (RegistrationApiException e) {
      SyncWorkerUtil.handleException(context, e, result);
    } catch (IOException e) {
      SyncWorkerUtil.handleException(context, e, result);
    }
  }

  private void handleUpdateSubscriptionPlanCache() {
    Log.d(TAG, "handleUpdateSubscriptionPlanCache");

    try {

      boolean                hasActivePlan     = false;
      List<SubscriptionPlan> subscriptionPlans = registration.getSubscriptionPlans(account);

      for (SubscriptionPlan plan : subscriptionPlans) {
        if (plan.isActive()) {
              hasActivePlan = true;
          int planType      = SubscriptionStore.getSubscriptionPlanType(plan);
          SubscriptionStore.setActiveSubscriptionPlanType(context, planType);
        }
      }

      if (!hasActivePlan)
        SubscriptionStore.setActiveSubscriptionPlanType(context, SubscriptionStore.PLAN_TYPE_NONE);

    } catch (RegistrationApiException e) {
      SyncWorkerUtil.handleException(context, e, result);
    } catch (IOException e) {
      SyncWorkerUtil.handleException(context, e, result);
    }
  }

  @Override
  public void run() {

    // TODO: if has new google play subscription PUT to server

    handleUpdateFlockAccountCache();
    handleUpdateCardInformationCache();
    handleUpdateSubscriptionPlanCache();
  }

  @Override
  public void cleanup() {

  }

}
