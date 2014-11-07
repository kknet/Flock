/*
 * *
 *  Copyright (C) 2014 Open Whisper Systems
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 * /
 */

package org.anhonesteffort.flock;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

import org.anhonesteffort.flock.auth.DavAccount;
import org.anhonesteffort.flock.sync.subscription.SubscriptionStore;

/**
 * Programmer: rhodey
 */
public class ManageSubscriptionActivity extends FragmentActivity {

  public static final String KEY_DAV_ACCOUNT_BUNDLE = "KEY_DAV_ACCOUNT_BUNDLE";
  public static final String KEY_CURRENT_FRAGMENT   = "KEY_CURRENT_FRAGMENT";

  protected DavAccount davAccount;
  protected Menu       optionsMenu;
  private   int        currentFragment = -1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    requestWindowFeature(Window.FEATURE_PROGRESS);

    setContentView(R.layout.simple_fragment_activity);
    getActionBar().setDisplayHomeAsUpEnabled(true);
    getActionBar().setTitle(R.string.title_manage_subscription);

    if (savedInstanceState != null && !savedInstanceState.isEmpty()) {
      if (!DavAccount.build(savedInstanceState.getBundle(KEY_DAV_ACCOUNT_BUNDLE)).isPresent()) {
        finish();
        return;
      }

      davAccount      = DavAccount.build(savedInstanceState.getBundle(KEY_DAV_ACCOUNT_BUNDLE)).get();
      currentFragment = savedInstanceState.getInt(KEY_CURRENT_FRAGMENT, -1);
    }
    else if (getIntent().getExtras() != null) {
      if (!DavAccount.build(getIntent().getExtras().getBundle(KEY_DAV_ACCOUNT_BUNDLE)).isPresent()) {
        finish();
        return;
      }

      davAccount      = DavAccount.build(getIntent().getExtras().getBundle(KEY_DAV_ACCOUNT_BUNDLE)).get();
      currentFragment = getIntent().getExtras().getInt(KEY_CURRENT_FRAGMENT, -1);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.manage_subscription, menu);

    if (currentFragment == SubscriptionStore.PLAN_TYPE_NONE)
      menu.findItem(R.id.button_send_bitcoin).setVisible(false);

    optionsMenu = menu;
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {

      case android.R.id.home:
        finish();
        break;

      case R.id.button_send_bitcoin:
        Intent nextIntent = new Intent(getBaseContext(), SendBitcoinActivity.class);
        nextIntent.putExtra(ManageSubscriptionActivity.KEY_DAV_ACCOUNT_BUNDLE, davAccount.toBundle());
        startActivity(nextIntent);
        break;

    }
    return false;
  }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    savedInstanceState.putBundle(KEY_DAV_ACCOUNT_BUNDLE, davAccount.toBundle());
    savedInstanceState.putInt(KEY_CURRENT_FRAGMENT,      currentFragment);

    super.onSaveInstanceState(savedInstanceState);
  }

  @Override
  public void onRestoreInstanceState(Bundle savedInstanceState) {
    if (savedInstanceState != null && !savedInstanceState.isEmpty()) {
      if (!DavAccount.build(savedInstanceState.getBundle(KEY_DAV_ACCOUNT_BUNDLE)).isPresent()) {
        finish();
        return;
      }

      davAccount      = DavAccount.build(savedInstanceState.getBundle(KEY_DAV_ACCOUNT_BUNDLE)).get();
      currentFragment = savedInstanceState.getInt(KEY_CURRENT_FRAGMENT, -1);
    }

    super.onRestoreInstanceState(savedInstanceState);
  }

  protected void updateFragmentWithPlanType(int planType) {
    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
    Fragment            nextFragment        = null;

    switch (planType) {
      case SubscriptionStore.PLAN_TYPE_GOOGLE:
        nextFragment = new SubscriptionGoogleFragment();

        break;

      case SubscriptionStore.PLAN_TYPE_STRIPE:
        nextFragment = new SubscriptionStripeFragment();
        break;

      default:
        nextFragment = new UnsubscribedFragment();
        if (optionsMenu != null)
          optionsMenu.findItem(R.id.button_send_bitcoin).setVisible(false);
        break;
    }

    fragmentTransaction.replace(R.id.fragment_view, nextFragment);
    fragmentTransaction.commit();

    currentFragment = planType;
  }

  @Override
  public void onResume() {
    super.onResume();

    if (currentFragment >= 0)
      updateFragmentWithPlanType(currentFragment);
    else
      updateFragmentWithPlanType(SubscriptionStore.getActiveSubscriptionPlanType(getBaseContext()));
  }

  @Override
  public void onBackPressed() {
    int activePlanType = SubscriptionStore.getActiveSubscriptionPlanType(getBaseContext());

    switch (currentFragment) {
      case SubscriptionStore.PLAN_TYPE_GOOGLE:
        if (activePlanType == SubscriptionStore.PLAN_TYPE_GOOGLE)
          super.onBackPressed();
        else
          updateFragmentWithPlanType(SubscriptionStore.PLAN_TYPE_NONE);
        break;

      case SubscriptionStore.PLAN_TYPE_STRIPE:
        if (activePlanType == SubscriptionStore.PLAN_TYPE_STRIPE)
          super.onBackPressed();
        else
          updateFragmentWithPlanType(SubscriptionStore.PLAN_TYPE_NONE);
        break;

      default:
        super.onBackPressed();
    }
  }

}
