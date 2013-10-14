/*
 * Copyright (c) 2013 Mark Prichard, CloudBees
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudbees.gasp.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.cloudbees.gasp.R;
import com.cloudbees.gasp.activity.TwitterStreamActivity;
import com.cloudbees.gasp.service.RESTIntentService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;

/**
 * Closely modeled on Neil Goodman's Android REST tutorials
 * https://github.com/posco2k8/rest_service_tutorial
 * https://github.com/posco2k8/rest_loader_tutorial.git
 *
 * @author Mark Prichard
 */
public class TwitterResponderFragment extends RESTResponderFragment {
    private static final String TAG = TwitterResponderFragment.class.getName();
    
    // We cache our stored tweets here so that we can return right away
    // on multiple calls to setTweets() during the Activity lifecycle events (such
    // as when the user rotates their device). In a real application we would want
    // to cache this data in a more sophisticated way, probably using SQLite and
    // Content Providers, but for the demo and simple apps this will do.
    private List<String> mTweets;
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        // This gets called each time our Activity has finished creating itself.
        setTweets();
    }

    private void setTweets() {
        TwitterStreamActivity activity = (TwitterStreamActivity) getActivity();
        
        if (mTweets == null && activity != null) {
            Intent intent = new Intent(activity, RESTIntentService.class);
            intent.setData(Uri.parse("http://search.twitter.com/search.json"));

            Bundle params = new Bundle();
            params.putString("q", "cloudbees");
            
            intent.putExtra(RESTIntentService.EXTRA_PARAMS, params);
            intent.putExtra(RESTIntentService.EXTRA_RESULT_RECEIVER, getResultReceiver());

            activity.startService(intent);
        }
        else if (activity != null) {
            ArrayAdapter<String> adapter = activity.getArrayAdapter();
            
            adapter.clear();
            for (String tweet : mTweets) {
                adapter.add(tweet);
            }
        }
    }
    
    @Override
    public void onRESTResult(int code, String result) {
        Log.d(TAG, "Result code: " + code);
        Log.d(TAG, "Result data: " + result);

        if (code == 200 && result != null) {
            mTweets = getTweetsFromJson(result);
            setTweets();
        }
        else {
            Activity activity = getActivity();
            if (activity != null) {
                Toast.makeText(activity, 
                                getResources().getString(R.string.gasp_twitter_error),
                                Toast.LENGTH_SHORT)
                                .show();
            }
        }
    }

    // TODO: Replace with Gson
    private static List<String> getTweetsFromJson(String json) {
        ArrayList<String> tweetList = new ArrayList<String>();
        
        try {
            JSONObject tweetsWrapper = (JSONObject) new JSONTokener(json).nextValue();
            JSONArray  tweets        = tweetsWrapper.getJSONArray("results");
            
            for (int i = 0; i < tweets.length(); i++) {
                JSONObject tweet = tweets.getJSONObject(i);
                Log.d(TAG, "Tweet: " + tweet.getString("text"));
                tweetList.add(tweet.getString("text"));
            }
        }
        catch (JSONException e) {
            Log.e(TAG, "Failed to parse JSON.", e);
        }
        
        return tweetList;
    }

}