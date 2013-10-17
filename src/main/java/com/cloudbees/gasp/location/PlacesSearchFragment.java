package com.cloudbees.gasp.location;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.cloudbees.gasp.activity.PlacesActivity;
import com.cloudbees.gasp.model.Places;
import com.cloudbees.gasp.model.Query;
import com.google.gson.Gson;

import java.net.URL;
import java.net.URLEncoder;

/**
 * Copyright (c) 2013 Mark Prichard, CloudBees
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class PlacesSearchFragment extends Fragment {
    private static final String TAG = PlacesSearchFragment.class.getName();

    private final String keyword = "Restaurant";
    private final String encoding = "utf8";

    private Query mQuery;
    private String jsonOutput;
    private String token;
    boolean haveToken = false;

    PlacesActivity activity = (PlacesActivity) getActivity();

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void placesSearch(Query query) {
        mQuery = query;
        Log.d(TAG, "Lat: " + String.valueOf(query.getLat()));
        Log.d(TAG, "Lng: " + String.valueOf(query.getLng()));
        Log.d(TAG, "Radius: " + query.getRadius());
        Log.d(TAG, "next_page_token: " + query.getNext_page_token());

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    String search = GooglePlacesClient.PLACES_API_BASE
                            + GooglePlacesClient.TYPE_SEARCH
                            + GooglePlacesClient.OUT_JSON
                            + "?sensor=false"
                            + "&key=" + GooglePlacesClient.API_KEY
                            + "&keyword=" + URLEncoder.encode(keyword, encoding)
                            + "&location=" + String.valueOf(mQuery.getLat()) + "," + String.valueOf(mQuery.getLng())
                            + "&radius=" + String.valueOf(mQuery.getRadius());
                    if (!mQuery.getNext_page_token().isEmpty()) {
                        search += "&pagetoken=" + URLEncoder.encode(mQuery.getNext_page_token(), encoding);
                    }
                    jsonOutput = GooglePlacesClient.doGet(new URL(search));

                } catch (Exception e) {
                    Log.e(TAG, "Exception: ", e);
                }
                return jsonOutput;
            }

            @Override
            protected void onPostExecute(String jsonOutput) {
                super.onPostExecute(jsonOutput);
                Places places = new Gson().fromJson(jsonOutput, Places.class);

                try {
                    Log.d(TAG, "Status: " + places.getStatus());

                    PlacesActivity activity = (PlacesActivity) getActivity();
                    activity.putOnMap(places);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }
}