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

import com.google.api.client.googleapis.services.AbstractGoogleClient;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;

/**
 * Allows configuring Cloud Endpoint builders to support authenticated calls, as well as calls to
 * CLoud Endpoints exposed from an App Engine backend that run locally during development.
 */
class CloudEndpointBuilderHelper {
  private static final boolean LOCAL_ANDROID_RUN = false;
  private static final String LOCAL_APP_ENGINE_SERVER_URL = "http://10.0.2.2:8888";

  /**
   * Updates the Google client builder to connect the appropriate server based on whether
   * LOCAL_ANDROID_RUN is true or false.
   *
   * @param builder Google client builder
   * @return same Google client builder
   */
  public static <B extends AbstractGoogleClient.Builder> B updateBuilder(B builder) {
    if (LOCAL_ANDROID_RUN) {
      builder.setRootUrl(LOCAL_APP_ENGINE_SERVER_URL + "/_ah/api/");
    }

    // only enable GZip when connecting to remote server
    final boolean enableGZip = builder.getRootUrl().startsWith("https:");

    builder.setGoogleClientRequestInitializer(new GoogleClientRequestInitializer() {
      @Override
      public void initialize(AbstractGoogleClientRequest<?> request) {
        if (!enableGZip) {
          request.setDisableGZipContent(true);
        }
      }
    });

    return builder;
  }

  /**
   * Returns appropriate HttpRequestInitializer depending whether the application is configured to
   * require users to be signed in or not.
   */
  static HttpRequestInitializer getRequestInitializer() {
    if (SignInActivity.SIGN_IN_REQUIRED) {
      return SignInActivity.credential;
    } else {
      HttpRequestInitializer httpRequestInitializer = new HttpRequestInitializer() {
        @Override
        public void initialize(HttpRequest arg0) {}
      };
      return httpRequestInitializer;
    }
  }
}
