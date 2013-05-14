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
import com.google.sample.mobileassistant.shoppingassistant.Shoppingassistant.OfferEndpoint;
import com.google.sample.mobileassistant.shoppingassistant.Shoppingassistant.RecommendationEndpoint;
import com.google.sample.mobileassistant.shoppingassistant.model.Offer;
import com.google.sample.mobileassistant.shoppingassistant.model.OfferCollection;
import com.google.sample.mobileassistant.shoppingassistant.model.PlaceInfo;
import com.google.sample.mobileassistant.shoppingassistant.model.Recommendation;
import com.google.sample.mobileassistant.shoppingassistant.model.RecommendationCollection;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
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
 * Activity used when the user "selected" a place or checked into a place.
 */
public class PlaceDetailsActivity extends Activity {
  protected static PlaceInfo currentPlace;

  ListView offersList;
  ListView recommendationsList;

  TextView placesNameLabel;
  TextView offersListLabel;
  TextView recommendationsListLabel;

  /**
   * Initializes the activity content, binds relevant widgets and starts asynchronously retrieving
   * offers and recommendations.
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

    setContentView(R.layout.activity_single_place);

    offersList = (ListView) findViewById(R.id.OffersList);
    recommendationsList = (ListView) findViewById(R.id.RecommendationsList);

    placesNameLabel = (TextView) findViewById(R.id.PlacesNameLabel);
    offersListLabel = (TextView) findViewById(R.id.OffersListLabel);
    recommendationsListLabel = (TextView) findViewById(R.id.RecommendationListLabel);

    placesNameLabel.setText(currentPlace.getName());

    retrieveOffers();
    retrieveRecommendations();
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.activity_place_details, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int itemId = item.getItemId();
    if (itemId == R.id.menu_refresh) {
      retrieveOffers();
      retrieveRecommendations();
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  /**
   * Retrieves offers asynchronously and updates relevant widgets
   */
  void retrieveOffers() {
    new ListOfOffersAsyncRertriever().execute(currentPlace);
  }

  /**
   * Retrieves recommendations asynchronously and updates relevant widgets
   */
  void retrieveRecommendations() {
    new ListOfRecommendationsAsyncRetriever().execute(currentPlace);
  }

  /**
   * AsyncTask for retrieving the list of offers and updating the corresponding ListView and label.
   */
  private class ListOfOffersAsyncRertriever extends AsyncTask<PlaceInfo, Void, OfferCollection> {
    private Logger log = Logger.getLogger(ListOfOffersAsyncRertriever.class.getName());

    /**
     * Updates UI to indicate that offers are being retrieved.
     */
    @Override
    protected void onPreExecute() {
      offersListLabel.setText(R.string.retrievingOffers);
      PlaceDetailsActivity.this.setProgressBarIndeterminateVisibility(true);
    }

    /**
     * Updates UI to indicate that retrieval of the offers completed successfully or failed.
     */
    @Override
    protected void onPostExecute(OfferCollection result) {
      PlaceDetailsActivity.this.setProgressBarIndeterminateVisibility(false);

      if (result == null || result.getItems() == null || result.getItems().size() < 1) {
        if (result == null) {
          offersListLabel.setText(R.string.failedToRetrieveOffers);
        } else {
          offersListLabel.setText(R.string.noOffers);
        }
        offersList.setAdapter(null);
        return;
      }

      offersListLabel.setText(R.string.offers);

      ListAdapter offersListAdapter = createOfferListAdapter(result.getItems());

      offersList.setAdapter(offersListAdapter);
    }

    /**
     * Creates ListAdapter populated with offer information.
     *
     * @param offers the list of offers used to populate the adapter.
     * @return an adapter populated with offer information.
     */
    private ListAdapter createOfferListAdapter(List<Offer> offers) {
      List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
      for (Offer offer : offers) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("offerIcon", offer.getImageUrl());
        map.put("offerTitle", offer.getTitle());
        map.put("offerDetails", offer.getDescription());
        data.add(map);
      }

      SimpleAdapter adapter = new SimpleAdapter(PlaceDetailsActivity.this, data,
          R.layout.offer_item, new String[] {"offerIcon", "offerTitle", "offerDetails"},
          new int[] {R.id.offer_Image, R.id.offer_name, R.id.offer_description});
      adapter.setViewBinder(new ImageUrlViewBinder(R.id.offer_Image));
      return adapter;
    }

    /**
     * Retrieves the list of offers through appropriate CloudEndpoint.
     *
     * @param params the place for which to retrieve offers.
     * @return collection of retrieved offers.
     */
    @Override
    protected OfferCollection doInBackground(PlaceInfo... params) {
      PlaceInfo place = params[0];

      if (place == null) {
        return null;
      }

      Builder endpointBuilder = new Shoppingassistant.Builder(
          AndroidHttp.newCompatibleTransport(), new JacksonFactory(),
          CloudEndpointBuilderHelper.getRequestInitializer());

      OfferEndpoint offerEndpoint =
          CloudEndpointBuilderHelper.updateBuilder(endpointBuilder).build().offerEndpoint();

      OfferCollection result;

      try {
        result = offerEndpoint.list(place.getPlaceId()).execute();
      } catch (IOException e) {
        String message = e.getMessage();
        if (message == null) {
          message = e.toString();
        }
        log.severe("Exception=" + message);
        result = null;
      }
      return result;
    }
  }

  /**
   * AsyncTask for retrieving the list of recommendations and updating the corresponding ListView
   * and label.
   */
  private class ListOfRecommendationsAsyncRetriever
      extends AsyncTask<PlaceInfo, Void, RecommendationCollection> {
    private Logger log = Logger.getLogger(ListOfOffersAsyncRertriever.class.getName());

    /**
     * Updates UI to indicate that recommendations are being retrieved.
     */
    @Override
    protected void onPreExecute() {
      recommendationsListLabel.setText(R.string.retrievingRecommendations);
      PlaceDetailsActivity.this.setProgressBarIndeterminateVisibility(true);
    }

    /**
     * Updates UI to indicate that retrieval of the offers completed successfully or failed.
     */
    @Override
    protected void onPostExecute(RecommendationCollection result) {
      PlaceDetailsActivity.this.setProgressBarIndeterminateVisibility(false);

      if (result == null || result.getItems() == null || result.getItems().size() < 1) {
        if (result == null) {
          recommendationsListLabel.setText(R.string.failedToRetrieveRecommendations);
        } else {
          recommendationsListLabel.setText(R.string.noRecommendations);
        }
        recommendationsList.setAdapter(null);
        return;
      }

      recommendationsListLabel.setText(R.string.recommendations);

      ListAdapter recommendationsListAdapter = createRecommendationsListAdapter(result.getItems());
      recommendationsList.setAdapter(recommendationsListAdapter);
    }

    /**
     * Creates ListAdapter populated with recommendation information.
     *
     * @param recommendations the list of recommendations used to populate the adapter.
     * @return an adapter populated with recommendation information.
     */
    private ListAdapter createRecommendationsListAdapter(List<Recommendation> recommendations) {
      List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
      for (Recommendation recommendation : recommendations) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("productImage", recommendation.getImageUrl());
        map.put("recommendationTitle", recommendation.getTitle());
        map.put("recommendationDetails", recommendation.getDescription());
        data.add(map);
      }

      SimpleAdapter adapter = new SimpleAdapter(PlaceDetailsActivity.this, data,
          R.layout.offer_item,
          new String[] {"productImage", "recommendationTitle", "recommendationDetails"},
          new int[] {R.id.offer_Image, R.id.offer_name, R.id.offer_description});
      adapter.setViewBinder(new ImageUrlViewBinder(R.id.offer_Image));
      return adapter;
    }

    /**
     * Retrieves the list of recommendations through appropriate CloudEndpoint.
     *
     * @param params the place for which to retrieve recommendations.
     * @return collection of retrieved recommendations.
     */
    @Override
    protected RecommendationCollection doInBackground(PlaceInfo... params) {
      PlaceInfo place = params[0];

      if (place == null) {
        return null;
      }

      Builder endpointBuilder = new Shoppingassistant.Builder(
          AndroidHttp.newCompatibleTransport(), new JacksonFactory(),
          CloudEndpointBuilderHelper.getRequestInitializer());

      RecommendationEndpoint recommendationEndpoint =
          CloudEndpointBuilderHelper.updateBuilder(endpointBuilder)
              .build().recommendationEndpoint();

      RecommendationCollection result;

      try {
        result = recommendationEndpoint.list(place.getPlaceId()).execute();
      } catch (IOException e) {
        String message = e.getMessage();
        if (message == null) {
          message = e.toString();
        }
        log.severe("Exception=" + message);
        result = null;
      }
      return result;
    }
  }
}
