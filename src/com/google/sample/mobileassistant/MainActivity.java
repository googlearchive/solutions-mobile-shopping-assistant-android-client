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

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.sample.mobileassistant.shoppingassistant.Shoppingassistant;
import com.google.sample.mobileassistant.shoppingassistant.Shoppingassistant.Builder;
import com.google.sample.mobileassistant.shoppingassistant.Shoppingassistant.CheckInEndpoint;
import com.google.sample.mobileassistant.shoppingassistant.Shoppingassistant.PlaceEndpoint;
import com.google.sample.mobileassistant.shoppingassistant.model.CheckIn;
import com.google.sample.mobileassistant.shoppingassistant.model.PlaceInfo;
import com.google.sample.mobileassistant.shoppingassistant.model.PlaceInfoCollection;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Mobile Shopping Assistant Main Activity. Launched after the application starts. Allows retrieving
 * the list of places (e.g., stores) and selecting a place / checking into a place and navigating to
 * another activities. Also responsible for integration with Google Play Services and Google
 * Accounts for OAuth2 authentication.
 */
public class MainActivity extends Activity {
  private Logger log = Logger.getLogger(MainActivity.class.getName());

  private ListView placesList;
  private TextView placesListLabel;

  private List<PlaceInfo> places = null;

  private GeoLocationHelper geoLocationHelper = new GeoLocationHelper();

  /**
   * Initializes the activity content, binds relevant widgets, sets up geo-location retrieval,
   * registers with Google Cloud Messaging (GCM) and starts asynchronously retrieving the list of
   * nearby places.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

    setContentView(R.layout.activity_main);

    placesList = (ListView) findViewById(R.id.PlacesList);
    placesListLabel = (TextView) findViewById(R.id.PlacesListLabel);
    placesList.setOnItemClickListener(placesListClickListener);

    geoLocationHelper.startRetrievingLocation(this);

    GCMIntentService.register(MainActivity.this);

    // start retrieving the list of nearby places
    new ListOfPlacesAsyncRetriever().execute(geoLocationHelper.getCurrentLocation());
  }

  /**
   * Event handler for Button widget that allows the user to refresh the list of nearby places.
   */
  public void onRetrievePlaces(View view) {
    if (geoLocationHelper.getCurrentLocation() == null) {
      // TODO(user): Tell the user that location is not available and prompt to
      // enable GPS/Network Location
      // or prompt for address
    }
    new ListOfPlacesAsyncRetriever().execute(geoLocationHelper.getCurrentLocation());
  }

  /**
   * Event handler invoked when user clicks on an item in the list of nearby places. It
   * asynchronously checks the user into the selected place and navigates to the activity that will
   * present details about this place
   */
  private OnItemClickListener placesListClickListener = new OnItemClickListener() {
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
      PlaceInfo selectedPlace = places.get((int) arg3);

      new CheckInTask().execute(selectedPlace);

      PlaceDetailsActivity.currentPlace = selectedPlace;
      Intent i = new Intent(MainActivity.this, PlaceDetailsActivity.class);
      startActivity(i);
    }
  };

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.activity_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int itemId = item.getItemId();
    if (itemId == R.id.menu_signOut) {
      SignInActivity.onSignOut(this);
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  /**
   * Stops retrieving geo-location updates when the activity is no longer visible.
   */
  @Override
  protected void onStop() {
    super.onStop();
    geoLocationHelper.stopRetrievingLocation();
  }

  /**
   * Resumes retrieving geo-location updates when the activity is restarted.
   */
  @Override
  protected void onRestart() {
    super.onRestart();
    geoLocationHelper.startRetrievingLocation(this);
  }

  /**
   * AsyncTask for retrieving the list of nearby places (e.g., stores) and updating the
   * corresponding ListView and label.
   */
  private class ListOfPlacesAsyncRetriever extends AsyncTask<Location, Void, PlaceInfoCollection> {

    /**
     * Updates UI to indicate that the list of nearby places is being retrieved.
     */
    @Override
    protected void onPreExecute() {
      placesListLabel.setText(R.string.retrievingPlaces);
      MainActivity.this.setProgressBarIndeterminateVisibility(true);
    }

    /**
     * Updates UI to indicate that retrieval of the list of nearby places completed successfully or
     * failed.
     */
    @Override
    protected void onPostExecute(PlaceInfoCollection result) {
      MainActivity.this.setProgressBarIndeterminateVisibility(false);

      if (result == null || result.getItems() == null || result.getItems().size() < 1) {
        if (result == null) {
          placesListLabel.setText(R.string.failedToRetrievePlaces);
        } else {
          placesListLabel.setText(R.string.noPlacesNearby);
        }

        placesList.setAdapter(null);
        return;
      }

      placesListLabel.setText(R.string.nearbyPlaces);

      ListAdapter placesListAdapter = createPlaceListAdapter(result.getItems());
      placesList.setAdapter(placesListAdapter);

      places = result.getItems();
    }

    /**
     * Creates ListAdapter populated with the list of nearby places.
     *
     * @param places the list of nearby places used to populate the adapter.
     * @return an adapter populated with the list of nearby places.
     */
    private ListAdapter createPlaceListAdapter(List<PlaceInfo> places) {
      final double kilometersInAMile = 1.60934;
      List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
      for (PlaceInfo place : places) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("placeIcon", R.drawable.ic_launcher);
        map.put("placeName", place.getName());
        map.put("placeAddress", place.getAddress());
        String distance = String.format(
            getString(R.string.distance), place.getDistanceInKilometers(),
            place.getDistanceInKilometers() / kilometersInAMile);
        map.put("placeDistance", distance);
        data.add(map);
      }

      SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, data, R.layout.place_item,
          new String[] {"placeIcon", "placeName", "placeAddress", "placeDistance"},
          new int[] {R.id.place_Icon, R.id.place_name, R.id.place_address, R.id.place_distance});

      return adapter;
    }


    /**
     * Retrieves the list of nearby places through appropriate CloudEndpoint.
     *
     * @param params the current geolocation for which to retrieve the list of nearby places.
     * @return the collection of retrieved nearby places.
     */
    @Override
    protected PlaceInfoCollection doInBackground(Location... params) {
      Location checkInLocation = params[0];

      float longitude;
      float latitude;

      if (checkInLocation == null) {
        // return null;
        // TODO(user): Remove this temporary code and just return null

        longitude = (float) -122.12151;
        latitude = (float) 47.67399;
      } else {
        latitude = (float) checkInLocation.getLatitude();
        longitude = (float) checkInLocation.getLongitude();
      }

      Builder endpointBuilder = new Shoppingassistant.Builder(
          AndroidHttp.newCompatibleTransport(), new JacksonFactory(),
          CloudEndpointBuilderHelper.getRequestInitializer());

      PlaceEndpoint placeEndpoint =
          CloudEndpointBuilderHelper.updateBuilder(endpointBuilder).build().placeEndpoint();

      PlaceInfoCollection result;

      // Retrieve the list of up to 10 places within 50 kms
      try {
        long distanceInKm = 50;
        int count = 10;

        result = placeEndpoint.list(
            count, distanceInKm, Float.toString(latitude), Float.toString(longitude)).execute();
      } catch (IOException e) {
        if (e != null) {
          String message = e.getMessage();
          if (message == null) {
            message = e.toString();
          }
          log.severe("Exception=" + message);
        }
        result = null;
      }
      return result;
    }
  }

  /**
   * AsyncTask for calling Mobile Assistant API for checking into a place (e.g., a store)
   */
  private class CheckInTask extends AsyncTask<PlaceInfo, Void, Void> {

    /**
     * Calls appropriate CloudEndpoint to indicate that user checked into a place.
     *
     * @param params the place where the user is checking in.
     */
    @Override
    protected Void doInBackground(PlaceInfo... params) {

      CheckIn checkin = new CheckIn();
      checkin.setPlaceId(params[0].getPlaceId());

      Builder endpointBuilder = new Shoppingassistant.Builder(
          AndroidHttp.newCompatibleTransport(), new JacksonFactory(),
          CloudEndpointBuilderHelper.getRequestInitializer());

      CheckInEndpoint checkinEndpoint =
          CloudEndpointBuilderHelper.updateBuilder(endpointBuilder).build().checkInEndpoint();

      try {
        checkinEndpoint.insert(checkin).execute();
      } catch (IOException e) {
        String message = e.getMessage();
        if (message == null) {
          message = e.toString();
        }
        log.warning("Exception when checking in =" + message);
      }
      return null;
    }
  }
}
