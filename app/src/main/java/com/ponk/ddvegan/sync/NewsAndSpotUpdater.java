package com.ponk.ddvegan.sync;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Toast;

import com.ponk.ddvegan.R;
import com.ponk.ddvegan.activities.MainActivity;
import com.ponk.ddvegan.activities.SplashActivity;
import com.ponk.ddvegan.fragments.NewsFragment;
import com.ponk.ddvegan.fragments.SpotListFragment;
import com.ponk.ddvegan.models.DataRepo;
import com.ponk.ddvegan.models.VeganNews;
import com.ponk.ddvegan.utils.NetworkUtil;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;


public class NewsAndSpotUpdater extends AsyncTask<Integer, Integer, Integer> {
    SplashActivity splashActivity;
    Context context;
    DatabaseManager dbMan;
    SQLiteDatabase db;
    int ret = 0;
    boolean firstStart = false;

    boolean updateSpots = false;

    public NewsAndSpotUpdater(SplashActivity splashActivity, int result) {
        this.splashActivity = splashActivity;
        this.context = splashActivity.getApplicationContext();
        firstStart = (result == -1);
    }

    public NewsAndSpotUpdater(Context c, int result) {
        this.context = c;
        firstStart = (result == -1);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Integer doInBackground(Integer... ints) {
        dbMan = new DatabaseManager(context);
        db = dbMan.getWritableDatabase();

        if (NetworkUtil.isConnected(context)) {
            HashSet<Integer> spots = updateNews();
            if (!firstStart)
                updateVeganSpots(spots);
        } else {
            ret = NetworkUtil.connectionError;
            dbMan.getVeganNewsFromDatabase();
            dbMan.getVeganSpotsFromDatabase(false);
            Collections.reverse(DataRepo.veganNews);
        }

        return ret;
    }

    protected void onPostExecute(Integer result) {
        super.onPostExecute(result);
        switch (result) {
            case NetworkUtil.connectionError:
                Toast.makeText(context, context.getString(R.string.sync_fail_internet), Toast.LENGTH_SHORT).show();
                break;
            case NetworkUtil.serverError:
                Toast.makeText(context, context.getString(R.string.sync_fail_server), Toast.LENGTH_SHORT).show();
                break;
        }

        if (NewsFragment.newsRefresher != null) {
            NewsFragment.newsAdapter.notifyDataSetChanged();
            SpotListFragment.spotListAdapter.notifyDataSetChanged();
            NewsFragment.newsRefresher.setRefreshing(false);
        }

        if (splashActivity != null) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    Animation x = new AlphaAnimation(1, 0);
                    x.setFillAfter(true);
                    x.setDuration(200);
                    x.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            Intent intent = new Intent(context, MainActivity.class);
                            splashActivity.startActivity(intent);
                            splashActivity.overridePendingTransition(R.anim.anim_slide_in, R.anim.anim_slide_out);
                            splashActivity.finish();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                    splashActivity.icon.startAnimation(x);

                }
            }, 1000);
        }
    }


    public void updateVeganSpots(HashSet<Integer> spots) {

        if (spots.isEmpty()) {
            Log.i("NewsAndSpotUpdater", "No spot updates!");
            dbMan.getVeganSpotsFromDatabase(false);
            return;
        }

        JSONObject spotObj = new JSONObject();
        JSONArray idArray = new JSONArray();
        for (int i : spots) {
            idArray.put(i + "");
        }
        try {
            spotObj.put("spotIds", idArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i("NewsAndSpotUpdater", spotObj.toString());

        String result = requestSpotUpdates(spotObj.toString());
        JSONArray jArray;
        db = dbMan.getWritableDatabase();
        try {
            jArray = new JSONArray(result);
            for (int i = 0; i < jArray.length(); i++) {
                JSONObject jsonSpot;
                jsonSpot = jArray.getJSONObject(i);
                int id = jsonSpot.getInt("spotId");
                String name = jsonSpot.getString("spotName");
                String address = jsonSpot.getString("spotAddress");
                String phone = jsonSpot.getString("spotPhone");
                String mail = jsonSpot.getString("spotMail");
                String url = jsonSpot.getString("spotUrl");
                String info = jsonSpot.getString("spotInfo");
                String imgKey = jsonSpot.getString("spotImgKey");
                String hours = jsonSpot.getString("spotHours");
                int catFood = jsonSpot.getInt("catFood");
                int catBakery = jsonSpot.getInt("catBakery");
                int catShopping = jsonSpot.getInt("catShopping");
                int catVokue = jsonSpot.getInt("catVokue");
                int catCafe = jsonSpot.getInt("catCafe");
                int catIcecream = jsonSpot.getInt("catIcecream");
                double gpsLat = Float.parseFloat(jsonSpot.getString("spotLocLat"));
                double gpsLong = Float.parseFloat(jsonSpot.getString("spotLocLong"));
                Log.i("NewsAndSpotUpdater", "updating vegan spot: " + name);
                ContentValues values = new ContentValues();
                values.put("spotId", id);
                values.put("spotName", name);
                values.put("spotAddress", address);
                values.put("spotPhone", phone);
                values.put("spotMail", mail);
                values.put("spotUrl", url);
                values.put("spotInfo", info);
                values.put("spotImgKey", imgKey);
                values.put("spotHours", hours.replace("\n", "").replace("\r", ""));
                values.put("catFood", catFood);
                values.put("catShopping", catShopping);
                values.put("catCafe", catCafe);
                values.put("catIcecream", catIcecream);
                values.put("catVokue", catVokue);
                values.put("catBakery", catBakery);
                values.put("spotLocLong", gpsLong);
                values.put("spotLocLat", gpsLat);
                db.insertWithOnConflict("veganSpots", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            ret = NetworkUtil.serverError;
        }
        db.close();
        dbMan.getVeganSpotsFromDatabase(false);

    }


    public HashSet<Integer> updateNews() {
        dbMan.getVeganNewsFromDatabase();
        HashSet<Integer> updateTheseSpots = new HashSet<>();
        String result = requestNews(dbMan.getMaxNewsId());
        db = dbMan.getWritableDatabase();
        JSONArray jArray;
        try {
            jArray = new JSONArray(result);
            for (int i = 0; i < jArray.length(); i++) {
                JSONObject jsonNews;
                jsonNews = jArray.getJSONObject(i);
                int newsId = jsonNews.getInt("newsId");
                int newsType = jsonNews.getInt("newsType");
                int spotId = jsonNews.getInt("spotId");
                String newsContent = jsonNews.getString("newsContent");
                String newsTime = jsonNews.getString("newsTime");
                if (newsType <= 6) {
                    updateSpots = true;
                    if (newsType == 6) {
                        Log.i("NewsAndSpotUpdater", "Deleting vegan spot: " + newsContent);
                        String query = "DELETE FROM veganSpots WHERE spotId = " + spotId;
                        db.execSQL(query);
                    } else {
                        updateTheseSpots.add(spotId);
                    }
                }
                Log.i("NewsAndSpotUpdater", "Inserting vegan news: " + newsId);
                ContentValues values = new ContentValues();
                values.put("newsId", newsId);
                values.put("newsType", newsType);
                values.put("spotId", spotId);
                values.put("newsContent", newsContent);
                values.put("newsTime", newsTime);
                db.insert("veganNews", null, values);
                DataRepo.veganNews.add(new VeganNews(newsId,newsType,spotId,newsContent,newsTime,true));
            }
        } catch (JSONException e) {
            ret = NetworkUtil.serverError;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!(prefs.getString(DataRepo.APP_VERSION_KEY, "").equals(DataRepo.appVersionName))) {
            VeganNews tutNews = new VeganNews(-1, 8, -1, context.getString(R.string.news_tutorial), "", false);
            DataRepo.veganNews.add(tutNews);
        }
        db.close();

        Collections.reverse(DataRepo.veganNews);

        return updateTheseSpots;
    }

    public String requestNews(int maxId) {

        HttpClient httpClient = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(DataRepo.apiVeganNews+"/"+maxId);

        InputStream inputStream = null;
        String result = "";
        try {
            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();

            inputStream = entity.getContent();
            // json is UTF-8 by default
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    inputStream, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            result = sb.toString();
        } catch (Exception ignored) {
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    public String requestSpotUpdates(String spotIds) {

        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(DataRepo.apiVeganSpotUpdates);

        try {
            List<NameValuePair> nameValuePairs = new ArrayList<>(1);
            nameValuePairs.add(new BasicNameValuePair("spotIds", "" + spotIds));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        } catch (IOException ignored) {
        }

        InputStream inputStream = null;
        String result = "";
        try {
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();

            inputStream = entity.getContent();
            // json is UTF-8 by default
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    inputStream, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line).append("\n");
            result = sb.toString();
        } catch (Exception ignored) {
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (Exception ignored) {
            }
        }
        return result;
    }
}
