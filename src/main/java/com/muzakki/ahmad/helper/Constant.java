package com.muzakki.ahmad.helper;

import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;

/**
 * Created by jeki on 7/12/16.
 */
public class Constant {

    public static final int TIMEOUT = 5000;



    public static HashMap<String,String> getHeaders(){
        HashMap<String,String> headers = new HashMap<>();
        headers.put("Authorization","PUT YOUR AUTH KEY HERE");
        return headers;
    }

    public static final String HOST = "my host HOST";

    public static final String URL_LOGIN = HOST+"/login";

    public static final String URL_TOKEN = HOST+"/device/token";

    public static final String URL_RECEIVED = HOST+"/message/received";
    public static final String URL_USER_LIST = HOST + "/get/user";
    public static final String URL_USER_ADD = HOST + "/insert/user";


    public static final int DATABASE_VERSION = 3;
    public static final String DATABASE_NAME = "kordinasi.db";
    public static final String[] TABLES = new String[]{
            "create table user(id integer primary key,username text," +
                    "nama text)"
    };

    public static void resetTable(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("drop table user");
        sqLiteDatabase.execSQL(TABLES[0]);
    }
}
