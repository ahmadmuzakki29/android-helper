package com.muzakki.ahmad.helper;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class PersistentConnection extends IntentService {

    public PersistentConnection() {
        super("PersistentConnection");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent==null) return;

        String url = intent.getStringExtra("url");
        Bundle params = intent.getBundleExtra("params");
        String method = intent.getStringExtra("method");
        String tag = intent.getStringExtra("tag"); // success tag
        InternetConnection ic = new MyIC(this,true,tag);

        if(method.equals(InternetConnection.GET)){
            ic.get(url,params);
        }else{
            ic.post(url,params);
        }
    }

    private class MyIC extends InternetConnection{

        private final Context ctx;
        private final String tag;

        public MyIC(Context ctx, boolean persistent,String tag) {
            super(ctx, persistent);
            this.ctx = ctx;
            this.tag = tag;
        }

        @Override
        protected void onSuccess(JSONObject result) {
            if(tag!=null) {
                Intent in = new Intent(tag);
                try {
                    in.putExtra("data", Helper.jsonToBundle(result));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                ctx.sendBroadcast(in);
            }
        }
    }

}
