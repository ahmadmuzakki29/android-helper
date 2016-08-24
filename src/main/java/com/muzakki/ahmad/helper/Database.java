package com.muzakki.ahmad.helper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by jeki on 7/12/16.
 */
public class Database extends SQLiteOpenHelper {
    private final Context context;

    public Database(Context context){
        super(context, Config.getString(context,"db_name"), null, Config.getInt(context,"db_version"));
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        JSONObject tables = Config.getObject(context, "db_tables");

        try {
            for(Iterator<String> iter = tables.keys(); iter.hasNext();) {
                String key = iter.next();
                JSONObject table = tables.getJSONObject(key);
                db.execSQL(table.getString("creator"));
                try {
                    JSONArray initValues = table.getJSONArray("init_value");
                    for (int i = 0; i < initValues.length(); i++) {
                        String val = initValues.getString(i);
                        db.execSQL(val);
                    }
                }catch (JSONException e){}
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void resetTable(SQLiteDatabase db){
        JSONObject tables = Config.getObject(context, "db_tables");
        JSONArray reset_table = Config.getArray(context,"db_reset_table");

        try {
            for(int i=0;i<reset_table.length();i++){
                String tb = reset_table.getString(i);
                Log.i("jeki","resetting table "+ tb);
                db.execSQL("drop table if exists "+tb);

                JSONObject table = tables.getJSONObject(tb);
                db.execSQL(table.getString("creator"));
                try {
                    JSONArray initValues = table.getJSONArray("init_value");
                    for (int j = 0; j < initValues.length(); j++) {
                        String val = initValues.getString(j);
                        db.execSQL(val);
                    }
                }catch (JSONException e){ e.printStackTrace();}
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        resetTable(sqLiteDatabase);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        resetTable(db);
    }

    @NonNull
    protected ArrayList<Bundle> getBundle(Cursor c){
        ArrayList<Bundle> result = new ArrayList<>();
        if(c.getCount()>0) {
            result = new ArrayList<>();
            while (c.moveToNext()) {
                Bundle row = new Bundle();
                for(int col=0;col<c.getColumnCount();col++){
                    row.putString(c.getColumnName(col),c.getString(col));
                }
                result.add(row);
            }
        }
        return result;
    }


    public ArrayList<Bundle> query(String sql, @Nullable String[] param){
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(sql, param);
        return getBundle(c);
    }
}
