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
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.android.vending.billing.IInAppBillingService;

import org.anhonesteffort.flock.SubscriptionGoogleFragment;
import org.anhonesteffort.flock.auth.DavAccount;
import org.anhonesteffort.flock.registration.RegistrationApi;
import org.anhonesteffort.flock.registration.RegistrationApiException;
import org.anhonesteffort.flock.registration.model.AugmentedFlockAccount;
import org.anhonesteffort.flock.registration.model.GooglePlan;
import org.anhonesteffort.flock.registration.model.SubscriptionPlan;
import org.anhonesteffort.flock.sync.SyncWorker;
import org.anhonesteffort.flock.sync.SyncWorkerUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * rhodey
 */
public class SubscriptionSyncWorker implements SyncWorker {

  private static final String TAG = "org.anhonesteffort.flock.sync.subscription.SubscriptionSyncWorker";

  private final Context              context;
  private final DavAccount           account;
  private final IInAppBillingService billingService;
  private final SyncResult           result;
  private final RegistrationApi      registration;

  private List<SubscriptionPlan> subscriptionPlans;

  public SubscriptionSyncWorker(Context              context,
                                DavAccount           account,
                                IInAppBillingService billingService,
                                SyncResult           syncResult)
  {
    this.context        = context;
    this.account        = account;
    this.result         = syncResult;
    this.billingService = billingService;

    registration = new RegistrationApi(context);
  }

  private List<JSONObject> handleGetPurchasedGoogleSubscriptions() {
    List<JSONObject> subscriptions = new LinkedList<JSONObject>();

    if (billingService == null) {
      Log.e(TAG, "billing service is null");
      return subscriptions;
    }

    try {

      Bundle ownedItems = billingService
          .getPurchases(3, SubscriptionGoogleFragment.class.getPackage().getName(),
                           SubscriptionGoogleFragment.PRODUCT_TYPE_SUBSCRIPTION, null);

      ArrayList<String> purchaseDataList =
          ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");

      for (int i = 0; i < purchaseDataList.size(); ++i) {
        JSONObject productObject = new JSONObject(purchaseDataList.get(i));
        if (productObject.getString("productId") != null)
          subscriptions.add(productObject);
      }

    } catch (RemoteException e) {
      SyncWorkerUtil.handleException(context, e, result);
    } catch (JSONException e) {
      SyncWorkerUtil.handleException(context, e, result);
    }

    return subscriptions;
  }

  private void handleVerifyCanceledGoogleSubscriptionWithServer() {
    Log.d(TAG, "handleVerifyCanceledGoogleSubscriptionWithServer");
    /*
     TODO
      GET /accounts/{account}/subscription-plans type=GOOGLE, renewing=true
      if returned list is empty
        ask server to cancel subscription
      else
        google play services client api lied to us, ignore?
    */
  }

  private void handleLocallyCanceledGoogleSubscriptions() {
    Log.d(TAG, "handleLocallyCanceledGoogleSubscriptions");

    int planType  = SubscriptionStore.getActiveSubscriptionPlanType(context);
    if (planType != SubscriptionStore.PLAN_TYPE_GOOGLE)
      return;

    boolean          hasActiveSubscription = false;
    List<JSONObject> googleSubscriptions   = handleGetPurchasedGoogleSubscriptions();

    for (JSONObject googleSubscription : googleSubscriptions) {
      try {

        String subscriptionSku = googleSubscription.getString("productId");
        if (subscriptionSku.equals(SubscriptionGoogleFragment.SKU_YEARLY_SUBSCRIPTION)) {
          hasActiveSubscription = true;
          break;
        }

      } catch (JSONException e) {
        SyncWorkerUtil.handleException(context, e, result);
        return;
      }
    }

    if (!hasActiveSubscription && billingService != null)
      handleVerifyCanceledGoogleSubscriptionWithServer();
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

      int activePlans       = 0;
          subscriptionPlans = registration.getSubscriptionPlans(account);

      for (SubscriptionPlan plan : subscriptionPlans) {
        if (plan.isActive()) {
          activePlans++;
          int planType = SubscriptionStore.getSubscriptionPlanType(plan);
          SubscriptionStore.setActiveSubscriptionPlanType(context, planType);
        }
      }

      if (activePlans <= 0)
        SubscriptionStore.setActiveSubscriptionPlanType(context, SubscriptionStore.PLAN_TYPE_NONE);
      else if (activePlans > 1)
        Log.e(TAG, "client thinks it has " + activePlans + " active subscription plans :|");

    } catch (RegistrationApiException e) {
      SyncWorkerUtil.handleException(context, e, result);
    } catch (IOException e) {
      SyncWorkerUtil.handleException(context, e, result);
    }
  }

  private void handlePutNewGoogleSubscriptionToServer(JSONObject subscription) {
    Log.d(TAG, "handlePutNewGoogleSubscriptionToServer");
    // TODO put to server using registration api
  }

  private void handlePutNewGoogleSubscriptionsToServer() {
    Log.d(TAG, "handlePutNewGoogleSubscriptionsToServer");

    if (subscriptionPlans == null) {
      Log.w(TAG, "unable to get subscription plan list from " +
                 "server, skipping new google subscriptions");
      return;
    }

    for (JSONObject googleSubscription : handleGetPurchasedGoogleSubscriptions()) {
      try {

        String productSku    = googleSubscription.getString("productId");
        String purchaseToken = googleSubscription.getString("purchaseToken");

        if (productSku.equals(SubscriptionGoogleFragment.SKU_YEARLY_SUBSCRIPTION)) {
          boolean isNewSubscription = true;

          for (SubscriptionPlan plan : subscriptionPlans) {
            if (plan instanceof GooglePlan) {
              GooglePlan googlePlan = (GooglePlan) plan;
              if (googlePlan.getProductId().equals(productSku) &&
                  googlePlan.getPurchaseToken().equals(purchaseToken))
              {
                isNewSubscription = false;
              }
            }
          }

          if (isNewSubscription) {
            SubscriptionStore.setActiveSubscriptionPlanType(context, SubscriptionStore.PLAN_TYPE_GOOGLE);
            handlePutNewGoogleSubscriptionToServer(googleSubscription);
          }
        }

      } catch (JSONException e) {
        SyncWorkerUtil.handleException(context, e, result);
      }
    }
  }

  @Override
  public void run() {
    handleLocallyCanceledGoogleSubscriptions();

    handleUpdateFlockAccountCache();
    handleUpdateCardInformationCache();

    handleUpdateSubscriptionPlanCache();
    handlePutNewGoogleSubscriptionsToServer();
  }

  @Override
  public void cleanup() {

  }

}
