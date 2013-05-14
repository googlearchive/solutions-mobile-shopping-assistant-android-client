/*
 * Copyright (c) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.sample.mobileassistant;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

/**
 * Activity that allows the user to select the account they want to use to sign in. The class also
 * implements integration with Google Play Services and Google Accounts.
 */
public class SignInActivity extends Activity {
  static final boolean SIGN_IN_REQUIRED = true;
  private static final String AUDIENCE =
"server:client_id:!!! ENTER YOUR WEB CLIENT ID HERE !!!!";
  private static final String ACCOUNT_NAME_SETTING_NAME = "accountName";

  // constants for startActivityForResult flow
  private static final int REQUEST_ACCOUNT_PICKER = 1;
  private static final int REQUEST_GOOGLE_PLAY_SERVICES = 2;

  static GoogleAccountCredential credential;

  /**
   * Initializes the activity content and then navigates to the MainActivity if the user is already
   * signed in or if the app is configured to not require the sign in. Otherwise it initiates
   * starting the UI for the account selection and a check for Google Play Services being up to
   * date.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_signin);

    if (!SIGN_IN_REQUIRED) {
      // The app won't use authentication, so just launch the main activity.
      startMainActivity();
      return;
    }

    if (!checkGooglePlayServicesAvailable()) {
      // Google Play Services are required, so don't proceed until they are installed.
      return;
    }

    if (isSignedIn()) {
      startMainActivity();
    } else {
      startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

  }

  /**
   * Handles the results from activities launched to select an account and to install Google Play
   * Services.
   */
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case REQUEST_ACCOUNT_PICKER:
        if (data != null && data.getExtras() != null) {
          String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
          if (accountName != null) {
            onSignedIn(accountName);
            return;
          }
        }
        // Signing in is required so display the dialog again
        startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
        break;
      case REQUEST_GOOGLE_PLAY_SERVICES:
        if (resultCode != Activity.RESULT_OK) {
          checkGooglePlayServicesAvailable();
        }
        break;
    }
  }

  /**
   * Retrieves the previously used account name from the application preferences and checks if the
   * credential object can be set to this account.
   */
  private boolean isSignedIn() {
    credential = GoogleAccountCredential.usingAudience(this, AUDIENCE);
    SharedPreferences settings = getSharedPreferences("MobileAssistant", 0);
    String accountName = settings.getString(ACCOUNT_NAME_SETTING_NAME, null);
    credential.setSelectedAccountName(accountName);

    return credential.getSelectedAccount() != null;
  }

  /**
   * Called when the user selected an account. The account name is stored in the application
   * preferences and set in the credential object.
   *
   * @param accountName the account that the user selected.
   */
  private void onSignedIn(String accountName) {
    SharedPreferences settings = getSharedPreferences("MobileAssistant", 0);

    SharedPreferences.Editor editor = settings.edit();
    editor.putString(ACCOUNT_NAME_SETTING_NAME, accountName);
    editor.commit();
    credential.setSelectedAccountName(accountName);

    startMainActivity();
  }

  /**
   * Called to sign out the user, so user can later on select a different account.
   *
   * @param activity activity that initiated the sign out.
   */
  static void onSignOut(Activity activity) {
    SharedPreferences settings = activity.getSharedPreferences("MobileAssistant", 0);

    SharedPreferences.Editor editor = settings.edit();
    editor.putString(ACCOUNT_NAME_SETTING_NAME, "");

    editor.commit();
    credential.setSelectedAccountName("");

    Intent intent = new Intent(activity, SignInActivity.class);
    activity.startActivity(intent);
  }

  /**
   * Navigates to the MainActivity
   */
  private void startMainActivity() {
    Intent intent = new Intent(this, MainActivity.class);
    startActivity(intent);
  }


  @Override
  protected void onResume() {
    super.onResume();
    if (SIGN_IN_REQUIRED) {
      // As per GooglePlayServices documentation, an application needs to check
      // from within onResume if Google Play Services is available.
      checkGooglePlayServicesAvailable();
    }
  }

  /**
   * Checks if Google Play Services are installed and if not it initializes opening the dialog to
   * allow user to install Google Play Services.
   */
  private boolean checkGooglePlayServicesAvailable() {
    final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
    if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
      showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
      return false;
    }
    return true;
  }

  /**
   * Shows the Google Play Services dialog on UI thread.
   */
  void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
            connectionStatusCode, SignInActivity.this, REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
      }
    });
  }
}
