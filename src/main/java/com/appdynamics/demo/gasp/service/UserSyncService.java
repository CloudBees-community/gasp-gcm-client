/*
 * Copyright (c) 2013 Mark Prichard
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

package com.appdynamics.demo.gasp.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.appdynamics.demo.gasp.R;
import com.appdynamics.demo.gasp.adapter.UserDataAdapter;
import com.appdynamics.demo.gasp.model.User;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.ListIterator;

public class UserSyncService extends IntentService implements IRESTListener {
    private static final String TAG = UserSyncService.class.getName();

    private Uri mGaspUsersUri;

    private void getGaspUsersUriSharedPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences gaspSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String gaspUsersUri = gaspSharedPreferences.getString(getString(R.string.gasp_server_uri_preferences), "")
                + getString(R.string.gasp_users_location);

        this.mGaspUsersUri = Uri.parse(gaspUsersUri);
    }

    private Uri getGaspUsersUri() {
        return mGaspUsersUri;
    }

    public UserSyncService() {
        super(UserSyncService.class.getName());
    }

    private long checkLastId() {
        long lastId = 0;

        UserDataAdapter userData = new UserDataAdapter(this);
        userData.open();
        try {
            lastId = userData.getLastId();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            userData.close();
        }

        return lastId;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        getGaspUsersUriSharedPreferences();
        Log.i(TAG, "Using Gasp Server Restaurants URI: " + getGaspUsersUri());

        AsyncRESTClient asyncRestCall = new AsyncRESTClient(getGaspUsersUri(), this);
        asyncRestCall.getAll();
    }

    @Override
    public void onCompleted(String results) {
        Log.i(TAG, "Response from " + mGaspUsersUri.toString() + " :" + results + '\n');

        if (results != null) {
            try {
                Gson gson = new Gson();
                Type type = new TypeToken<List<User>>() {
                }.getType();
                List<User> users = gson.fromJson(results, type);

                // Check how many records already in local SQLite database
                long localRecords = checkLastId();

                UserDataAdapter userDB = new UserDataAdapter(getApplicationContext());
                userDB.open();
                ListIterator<User> iterator = users.listIterator();

                int index = 0;
                while (iterator.hasNext()) {
                    try {
                        User user = iterator.next();
                        if (user.getId() > localRecords) {
                            userDB.insert(user);
                            index++;
                        }
                    } catch (SQLiteConstraintException e) {
                        e.printStackTrace();
                    }
                }
                userDB.close();

                String resultTxt = "Sync: Found " + localRecords + ", Loaded " + index
                        + " users from " + mGaspUsersUri;
                Log.i(TAG, resultTxt + '\n');

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
