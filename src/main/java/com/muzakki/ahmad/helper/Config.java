package com.muzakki.ahmad.helper;

import android.content.Context;
import android.content.res.AssetManager;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

/**
 * Created by jeki on 7/12/16.
 */
public class Config {
    JSONObject configValue;
    private Config(Context ctx){
        AssetManager assetManager = ctx.getResources().getAssets();

        try {
            InputStream inputStream = assetManager.open("config.json");

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            String value =out.toString();
            configValue = new JSONObject(value);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject getConfig(){
        return configValue;
    }

    private static Config configInstance;
    public static Config getInstance(Context ctx){
        if(configInstance ==null) configInstance = new Config(ctx);
        return configInstance;
    }

    @Nullable
    public static String getString(Context ctx, String key){
        try {
            return getInstance(ctx).getConfig().getString(key);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int getInt(Context ctx,String key){
        try {
            return getInstance(ctx).getConfig().getInt(key);
        } catch (JSONException e) {
            return 0;
        }
    }

    public static JSONArray getArray(Context ctx,String key){
        try {
            return getInstance(ctx).getConfig().getJSONArray(key);
        } catch (JSONException e) {
            return null;
        }
    }

    public static JSONObject getObject(Context context, String key) {
        try {
            return getInstance(context).getConfig().getJSONObject(key);
        } catch (JSONException e) {
            return null;
        }
    }

    public static final int TIMEOUT = 5000;

    public static HashMap<String,String> getHeaders(){
        HashMap<String,String> headers = new HashMap<>();
        headers.put("Authorization","PUT YOUR AUTH KEY HERE");
        return headers;
    }

    public static final String HOST = "my host HOST";



}
