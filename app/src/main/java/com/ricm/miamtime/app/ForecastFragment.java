/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ricm.miamtime.app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Encapsulates fetching the forecast and displaying it as a {@link ListView} layout.
 */
public class ForecastFragment extends Fragment {


    private final String LOG_TAG = ForecastFragment.class.getSimpleName();

    private ArrayAdapter<String> mForecastAdapter;
    private ArrayAdapter<String> mForecastAdapterDetails;

    //Roue de chargement
    protected ProgressDialog myProgressDialog;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // The ArrayAdapter will take data from a source and
        // use it to populate the ListView it's attached to.
        mForecastAdapter =
                new ArrayAdapter<String>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_forecast, // The name of the layout ID.
                        R.id.list_item_forecast_textview, // The ID of the textview to populate.
                        new ArrayList<String>());
        mForecastAdapterDetails =
                new ArrayAdapter<String>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_forecast, // The name of the layout ID.
                        R.id.list_item_forecast_textview, // The ID of the textview to populate.
                        new ArrayList<String>());

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);

        listView.setAdapter(mForecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String forecast = mForecastAdapterDetails.getItem(position);
                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(intent);
            }
        });

        return rootView;
    }

    public void moreResults() {
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        myProgressDialog = ProgressDialog.show(getActivity(),
                "", "Chargement", true);
        weatherTask.execute(Utility.actualRange);
    }

    public void updateWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String range = prefs.getString(getString(R.string.pref_range_key),
                getString(R.string.pref_range_default));
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(getActivity());
        String tspType = sharedPrefs.getString(
                getString(R.string.pref_trnspt_key),
                getString(R.string.pref_trnspt_pied));
        String modeTsp;
        if (tspType.equals(getString(R.string.pref_trnspt_voiture))) {
            modeTsp = "driving";
        } else  {
            modeTsp = "walking";
        }

        if(range!=Utility.actualRange || modeTsp != Utility.actualTrspt){
           myProgressDialog = ProgressDialog.show(getActivity(),
                  "", "Chargement", true);
            Utility.nextPageToken=null;
            Utility.actualRange = range;
            Utility.actualTrspt = modeTsp;
            weatherTask.execute(range);
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();


        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String JsonStr)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_NPT = "next_page_token";
            final String OWM_RES = "results";
            final String OWM_NAME = "name";
            final String OWM_BOOL_OPENING = "open_now";
            final String OWM_BOOL_OPENING_HOURS = "opening_hours";
            final String OWM_TYPES = "types";
            final String OWM_RATING = "rating";
            // Location coordinate
            final String OWM_GEOM = "geometry";
            final String OWM_LOCATION_MIAM = "location";
            final String OWM_LATITUDE_MIAM = "lat";
            final String OWM_LONGITUDE_MIAM = "lng";
            final String OWM_ADRESS = "vicinity";


            JSONObject places_Json = new JSONObject(JsonStr);
            JSONArray PlacesArray = places_Json.getJSONArray(OWM_RES);

            Utility.nextPageToken = null;
            try {
                Utility.nextPageToken = places_Json.getString(OWM_NPT);
            } catch (org.json.JSONException j) {
                Utility.nextPageToken = null;
            }
            Log.d(LOG_TAG, "Toto : " + PlacesArray.length());
            String[] resultStrs = new String[PlacesArray.length()];
            for (int i = 0; i < PlacesArray.length(); i++) {

                // on recup nom, addr, et coords gps...
                String nom;
                String address;
                Double latres;
                Double lngres;


                // Get the JSON object representing the place
                JSONObject place = PlacesArray.getJSONObject(i);

                nom = place.getString(OWM_NAME);
                address = place.getString(OWM_ADRESS);

                latres = place.getJSONObject(OWM_GEOM).getJSONObject(OWM_LOCATION_MIAM).getDouble(OWM_LATITUDE_MIAM);
                lngres = place.getJSONObject(OWM_GEOM).getJSONObject(OWM_LOCATION_MIAM).getDouble(OWM_LONGITUDE_MIAM);

                //----------------------------------------
                SharedPreferences sharedPrefs =
                        PreferenceManager.getDefaultSharedPreferences(getActivity());
                String tspType = sharedPrefs.getString(
                        getString(R.string.pref_trnspt_key),
                        getString(R.string.pref_trnspt_pied));
                String modeTsp;
                if (tspType.equals(getString(R.string.pref_trnspt_voiture))) {
                  modeTsp = "driving";
                } else  {
                  modeTsp = "walking";
                }
                // Pour récupérer le temps de parcours;
                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;
                HttpURLConnection urlConnection2 = null;
                BufferedReader reader2 = null;
                String JsonDuree = null;
                String JsonDureeVoiture = null;
                String latQuery = Double.toString(Utility.latitude);
                String lngQuery = Double.toString(Utility.longitude);
                try {
                    String uriduree = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" +
                            latQuery + "," + lngQuery +
                            "&destinations=" +
                            Double.toString(latres) + "," + Double.toString(lngres) + "&mode=" +
                            modeTsp;
                    Uri builtUri = Uri.parse(uriduree).buildUpon().build();

                    URL url = new URL(builtUri.toString());
                    // Create the request to OpenWeatherMap, and open the connection
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // Read the input stream into a String
                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        // Nothing to do.
                        return null;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer.append(line + "\n");
                    }

                    if (buffer.length() == 0) {
                        // Stream was empty.  No point in parsing.
                        return null;
                    }
                    JsonDuree = buffer.toString();

                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error ", e);
                    // If the code didn't successfully get the weather data, there's no point in attemping
                    // to parse it.
                    return null;
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (final IOException e) {
                            Log.e(LOG_TAG, "Error closing stream", e);
                        }
                    }
                }

                JSONObject duree_Json = new JSONObject(JsonDuree);
                String dureeTrajet = duree_Json.getJSONArray("rows").getJSONObject(0).getJSONArray("elements").getJSONObject(0).getJSONObject("duration").getString("text");

                //-----------------------------------------
                resultStrs[i] = nom + "\n" + address + "\n" + Double.toString(latres) + "\n" + Double.toString(lngres) + "\n" + dureeTrajet;
            }
            myProgressDialog.dismiss();

            if(PlacesArray.length()==0){
                resultStrs = new String[1];
                resultStrs[0]= Utility.noResult;
            }
            return resultStrs;

        }
        @Override
        protected String[] doInBackground(String... params) {

            // If there's no zip code, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String JsonStr = null;

            String type = "food";
            String key = "AIzaSyDafj_vmRc5A7bQqu31OvXUa_RKY9vRNvI";
            String latQuery = Double.toString(Utility.latitude);
            String lngQuery = Double.toString(Utility.longitude);



            try {
                // Construct the URL for the GooglePlace API
                final String BASE_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?";
                final String LOCATION_PARAM_MIAM = "location";
                final String TYPES_PARAM_MIAM  = "types";
                final String UNITS_PARAM_MIAM  = "radius";
                final String OPENNOW_PARAM_MIAM  = "opennow";
                final String KEY_PARAM_MIAM  = "key";
                final String PTOKEN_PARAM_MIAM  = "pagetoken";


                Uri builtUri;
                if (Utility.nextPageToken == null) {
                    builtUri = Uri.parse(BASE_URL).buildUpon()
                            .appendQueryParameter(LOCATION_PARAM_MIAM, latQuery + "," + lngQuery)
                            .appendQueryParameter(UNITS_PARAM_MIAM, params[0])
                            .appendQueryParameter(TYPES_PARAM_MIAM, type)
                            .appendQueryParameter(OPENNOW_PARAM_MIAM, "")
                            .appendQueryParameter(KEY_PARAM_MIAM, key)
                            .build();
                }else{
                    builtUri = Uri.parse(BASE_URL).buildUpon()
                            .appendQueryParameter(PTOKEN_PARAM_MIAM, Utility.nextPageToken)
                            .appendQueryParameter(KEY_PARAM_MIAM, key)
                            .build();
                }

                URL url = new URL(builtUri.toString());

                Log.d(LOG_TAG, "check URL" + builtUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                JsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getWeatherDataFromJson(JsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                int i;
                mForecastAdapter.clear();
                mForecastAdapterDetails.clear();
                String[] mForecastSplit = new String[5];
                for(String dayForecastStr : result) {
                    i=0;
                    for(String retval: dayForecastStr.split("\n")){
                        mForecastSplit[i]=retval;
                        i++;
                    }
                    if(mForecastSplit[0] != Utility.noResult || mForecastSplit[4]!= null){
                        mForecastAdapter.add(mForecastSplit[0] + " - " + mForecastSplit[4]);
                    }else{
                        mForecastAdapter.add(mForecastSplit[0]);
                    }

                    mForecastAdapterDetails.add(dayForecastStr);
                }
                // New data is back from the server.  Hooray!
            }
        }
    }
}
