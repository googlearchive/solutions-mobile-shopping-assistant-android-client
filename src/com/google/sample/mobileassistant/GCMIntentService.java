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

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.sample.mobileassistant.shoppingassistant.Shoppingassistant;
import com.google.sample.mobileassistant.shoppingassistant.Shoppingassistant.Builder;
import com.google.sample.mobileassistant.shoppingassistant.Shoppingassistant.DeviceInfoEndpoint;
import com.google.sample.mobileassistant.shoppingassistant.model.DeviceInfo;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.IOException;

/**
 * IntentService responsible for handling communication with Google Cloud Messaging service.
 */
public class GCMIntentService extends GCMBaseIntentService {
private static final String GCM_PROJECT_ID = "!!! ENTER YOUR GCM PROJECT ID HERE !!!";

  /**
   * Register the device for GCM.
   *
   * @param context the activity's context.
   */
  public static void register(Context context) {
    GCMRegistrar.checkDevice(context);
    GCMRegistrar.checkManifest(context);
    GCMRegistrar.register(context, GCM_PROJECT_ID);
  }

  public GCMIntentService() {
    super(GCM_PROJECT_ID);
  }

  /**
   * Called on registration error. This is called in the context of a Service - no dialog or UI.
   *
   * @param context application's context
   * @param errorId an error message
   */
  @Override
  public void onError(Context context, String errorId) {}

  /**
   * Called when a cloud message has been received. The implementation looks for 'NotificationKind'
   * key in the payload and handles the message appropriately. Currently PriceCheckLowerPrices1 is
   * the only implemented NotificationKind and is handled by displaying a toast with information
   * about lower priced products.
   *
   * @param context application's context
   * @param intent intent containing the message payload as extras.
   */
  @Override
  public void onMessage(Context context, Intent intent) {
    if (intent.getStringExtra("NotificationKind").equals("PriceCheckLowerPrices1")) {
      final String message = getUserMessageForPriceCheckLowerPricesNotification(intent);

      Handler h = new Handler(Looper.getMainLooper());
      h.post(new Runnable() {
        @Override
        public void run() {
          Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
          toast.show();
        }
      });
    }
  }

  /**
   * Constructs the message to be displayed when PriceCheckLowerPrices notification has been
   * received. The payload for such a notification is expected to have two additional keys:
   * ProductName and ProductCount. The value of these keys are used in the message to the user.
   *
   * @param intent intent containing the payload as extras.
   */
  String getUserMessageForPriceCheckLowerPricesNotification(Intent intent) {
    String firstProductName = intent.getStringExtra("ProductName");
    String numberOfProductsAsString = intent.getStringExtra("ProductCount");

    int parsedNumberOfProducts;

    try {
      parsedNumberOfProducts = Integer.parseInt(numberOfProductsAsString);
    } catch (NumberFormatException n) {
      // assume that the number of products is 1
      parsedNumberOfProducts = 1;
    }

    final int numberOfProducts = parsedNumberOfProducts;

    int resourceId;

    if (numberOfProducts == 1) {
      resourceId = R.string.notification_PriceCheckLowerPrices1_1product;
    } else {
      resourceId = R.string.notification_PriceCheckLowerPrices1_manyProducts;
    }

    String message = String.format(getString(resourceId), firstProductName, numberOfProducts - 1);

    return message;
  }

  /**
   * Called when a registration token has been received. The method calls insertDeviceInfo API on
   * the backend passing the device registration id, so the backend can use it for sending push
   * notifications.
   *
   * @param context application's context
   * @param registration the registration id returned by the GCM service
   */
  @Override
  public void onRegistered(Context context, String registration) {
    try {
      Builder endpointBuilder = new Shoppingassistant.Builder(
          AndroidHttp.newCompatibleTransport(), new JacksonFactory(),
          CloudEndpointBuilderHelper.getRequestInitializer());

      DeviceInfoEndpoint deviceInfoEndpoint =
          CloudEndpointBuilderHelper.updateBuilder(endpointBuilder).build().deviceInfoEndpoint();
      deviceInfoEndpoint.insert(new DeviceInfo().setDeviceRegistrationID(registration)).execute();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Called when the device has been unregistered.
   *
   * @param context application's context
   * @param registrationId the registration id that was previously registered
   */
  @Override
  protected void onUnregistered(Context context, String registrationId) {}
}
