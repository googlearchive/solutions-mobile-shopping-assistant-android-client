# Mobile Shopping Assistant Android Client

## Copyright
Copyright 2013 Google Inc. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

## Disclaimer
This sample application is not an official Google product.

## Supported Platform and Versions
This sample source code and project is designed to work with Eclipse. It was tested with Eclipse 3.8. The resulted application runs on Android physical devices with Google API and was tested on devices with API level 16 and 17.

## Overview
[Mobile Shopping Assistant Android Client](https://github.com/GoogleCloudPlatform/solutions-mobile-shopping-assistant-android-client) demonstrates how to build an Android native application that is powered by Google Cloud Platform and uses Google Cloud Endpoints to communicate with a mobile backend running on Google App Engine.

### Prerequisite
1. Eclipse with [Google Plugin for Eclipse](https://developers.google.com/eclipse/docs/getting_started) and [Android Development Tools](http://developer.android.com/tools/sdk/eclipse-adt.html) with Google API Level 16 or higher installed.

## Download Instruction
You can download this sample [here](https://github.com/GoogleCloudPlatform/solutions-mobile-shopping-assistant-android-client). After the download finishes, unzip the files and import the projects into Eclipse.

## Developer Guide using Eclipse
1. Follow the steps in README.md for [Mobile Shopping Assistant Backend - Java](https://github.com/GoogleCloudPlatform/solutions-mobile-shopping-assistant-backend-java) to configure and deploy the mobile backend needed for this client app.

2. Open MobileAssistant/src/com/google/sample/mobileassistant/SignInActivity.java and update *AUDIENCE* variable with your *WEB_CLIENT_ID*.

3. Open MobileAssistant/src/com/google/sample/mobileassistant/GCMIntentService.java and update *GCM_PROJECT_ID* variable with your Google Cloud Console Project Id.

4. Add Google Play Services by following the Setup Google Play Services SDK section from [Android documentation](https://developer.android.com/google/play-services/setup.html).

5. Connect your Android device with [USB debugging enabled](http://developer.android.com/tools/device.html), select MobileAssistant project and run it. 

6. If you don't have a physical Android device available, you can test a subset of the app funcionality on Android emulator with Google API level 17 or higher, but first you need to edit MobileAssistant/src/com/google/sample/mobileassistant/SignInActivity.java and change SIGN_IN_REQUIRED constant to false. 

