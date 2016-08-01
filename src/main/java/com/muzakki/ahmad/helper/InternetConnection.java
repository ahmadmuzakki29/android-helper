package com.muzakki.ahmad.helper;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jeki on 4/27/16.
 * JSON type must be JSONObject OR JSONArray
 */

public class InternetConnection implements Handler.Callback{
    public static final String GET="GET";
    public static final String POST="POST";
    public static final int MAX_DELAY = 60000;
    private final String MULTIPART = "multi";
    private final Context context;
    private final long DELAY=2000;
    private final boolean persistent;
    private HashMap<String, String> headers;
    private int attempt=0;
    private int maxAttempt=3;
    private Bundle data = new Bundle();
    private Thread thread;
    private HttpURLConnection httpConn;
    private MultiPartPost multiPartPost;

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    private boolean debug;


    public InternetConnection(boolean persistent){
        this(null,persistent);
    }

    public InternetConnection(Context ctx){
        this(ctx,false);
    }

    public InternetConnection(Context ctx, boolean persistent){
        this.context = ctx;
        this.persistent = persistent;
        headers = getHeaders();
    }

    public void get(String url){
        request(url, null, GET);
    }

    public void get(String url,Bundle params){
        if(params!=null){
            url +="?";
            for(String key : params.keySet()) {
                String value = params.getString(key);
                url += key+"="+value+"&";
            }
        }
        if(isDebug()) Log.d("jeki", url);
        attempt=0;
        request(url, null, GET);
    }

    public void post(String url,Bundle params){
        if(isDebug()){
            Log.d("jeki", url);
            Log.d("jeki",params.toString());
        }
        request(url, params, POST);
    }

    public void postMultiPart(final String url,final Bundle params){
        if(multiPartPost!=null) multiPartPost.disconnect();
        if(thread!=null) thread.interrupt();
        data.putString("url", url);
        data.putBundle("params", params);
        data.putString("method", MULTIPART);
        thread = new Thread() {
            public void run() {
                Message msg = Message.obtain();
                msg.what = 1;
                Bundle b= null;
                String response = null;
                try {
                    multiPartPost = new MultiPartPost(url);

                    for(String key: params.keySet()){
                        Bundle field = params.getBundle(key);
                        Type type = (Type) field.getSerializable("type");
                        if(type== Type.TEXT) {
                            multiPartPost.addFormField(key, field.getString("value"));
                        }else{
                            try {
                                multiPartPost.addFilePart(key, new File(field.getString("value")));
                            }catch(IOException e){}
                        }
                    }

                    response  = multiPartPost.execute();
                    b = new Bundle();
                    b.putString("response", response);
                    b.putString("url", url);
                    b.putInt("response_code", HttpURLConnection.HTTP_OK);
                } catch (IOException|NullPointerException e) {
                    e.printStackTrace();
                    b = new Bundle();
                    b.putInt("response_code", HttpURLConnection.HTTP_INTERNAL_ERROR);
                }

                msg.setData(b);
                handleMessage(msg);
            }
        };
        thread.start();

    }

    private void request(final String url,final Bundle params,final String method){
        if(thread!=null) thread.interrupt();
        if(httpConn!=null)httpConn.disconnect();

        data.putString("url", url);
        data.putBundle("params", params);
        data.putString("method", method);
        thread = new Thread() {
            public void run() {
                Message msg = Message.obtain();
                msg.what = 1;
                Bundle b;
                String response = null;
                try {
                    response = openHttpConnection(url, params, method);

                    b = new Bundle();
                    b.putString("response", response);
                    b.putString("url", url);
                    b.putInt("response_code", HttpURLConnection.HTTP_OK);
                } catch (IOException e) {
                    e.printStackTrace();
                    b = new Bundle();
                    b.putInt("response_code", HttpURLConnection.HTTP_INTERNAL_ERROR);
                } catch (NullPointerException e){
                    b = new Bundle();
                    e.printStackTrace();
                    b.putInt("response_code", HttpURLConnection.HTTP_NOT_FOUND);
                }

                msg.setData(b);
                handleMessage(msg);
            }
        };
        thread.start();
    }

    @Override
    public boolean handleMessage(Message msg) {
        try{
            int code = msg.getData().getInt("response_code");
            if(code==HttpURLConnection.HTTP_OK){
                final String resp =msg.getData().getString("response");

                final JSONObject  obj = new JSONObject(resp);

                if(context instanceof Activity){
                    ((Activity) context).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onSuccess(obj);
                        }
                    });
                }else{
                    onSuccess(obj);
                }
            }else if(code==HttpURLConnection.HTTP_NOT_FOUND){
                throw new NullPointerException("NOT FOUND");
            }else{
                onFailure();
            }
        }catch(JSONException e){
            e.printStackTrace();
            onFailure();
        }catch(final NullPointerException nu){
            if(context instanceof Activity){
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onNull(nu);
                    }
                });
            }else {
                onNull(nu);
            }
        }
        return false;
    }

    protected void onSuccess(JSONObject result){
        Log.i("jeki","success with result : "+result.toString());
    }

    protected void TryAgain(){
        String url = data.getString("url");
        Bundle params = data.getBundle("params");
        String method = data.getString("method");
        if(method.equals(MULTIPART)){
            postMultiPart(url,params);
        }else {
            request(url, params, method);
        }
    }

    private void onFailure(){
        attempt++;
        Log.i("jeki","attempt "+attempt);
        if(attempt>=maxAttempt && !persistent){
            if(context instanceof Activity){
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onTimeout();
                    }
                });
            }else{ onTimeout();}
            return;
        }
        try{
            //exponential backoff
            long delay = DELAY * attempt;
            if(delay>=MAX_DELAY) delay = MAX_DELAY;
            Thread.sleep(delay);
            TryAgain();
        }catch (InterruptedException ex){ Log.i("jeki","another process going on");}
    }

    public void resetAndTryAgain(){
        attempt=0;
        TryAgain();
    }

    protected void onTimeout(){
        Log.e("jeki","Connection Timeout");
    }

    protected void onNull(NullPointerException ex){
        ex.printStackTrace();
        onFailure();
    }

    public String openHttpConnection(String urlStr,Bundle params,String method)
            throws IOException, NullPointerException {

        int resCode = -1;
        String total = null;
        if(context!=null && !isConnected()){
            throw new IOException("Not Connected");
        }

        try {
            URL url = new URL(urlStr);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setDoInput(true);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod(method);

            // set all headers
            for(Map.Entry<String,String> header: headers.entrySet()){
                httpConn.setRequestProperty(header.getKey(), header.getValue());
            }
            httpConn.setConnectTimeout(Constant.TIMEOUT);

            String param = "";
            if(params!=null) {
                OutputStream os = httpConn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                param = encodeParams(params);

                writer.write(param);
                writer.flush();
                writer.close();
            }

            httpConn.connect();

            resCode = httpConn.getResponseCode();
            if (resCode == HttpURLConnection.HTTP_OK) {
                URL origin = httpConn.getURL();
                total = getReponse(httpConn);
                URL dest = httpConn.getURL();
                if(!origin.sameFile(dest))
                    throw new IOException(" response code "+resCode+" redirected to "+method+" "+httpConn.getURL());
            }else if(resCode==HttpURLConnection.HTTP_INTERNAL_ERROR){
                URL errorurl = new URL(urlStr+"?"+param);
                throw new IOException(" response code "+resCode+"method"+method+" "+errorurl);
            }else if(resCode==HttpURLConnection.HTTP_NOT_FOUND){
                URL errorurl = new URL(urlStr+"?"+param);
                Log.i("jeki","response code "+resCode+"method"+method+" "+errorurl);
                throw new NullPointerException();
            }else{
                throw new IOException(" response code "+resCode+"method"+method+" "+urlStr);
            }

        }catch (MalformedURLException e) {
            Helper.sendLog(context, e.getMessage());
        }
        finally {
            if(httpConn!=null){
                httpConn.disconnect();
            }
        }

        return total;
    }

    public void setMaxAttempt(int maxAttempt) {
        this.maxAttempt = maxAttempt;
    }

    private String getReponse(HttpURLConnection http) throws IOException{
        InputStream in = http.getInputStream();
        StringBuilder total = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(in));

        String line;
        while ((line = r.readLine()) != null) {
            total.append(line);
        }
        return total.toString();
    }

    private String encodeParams(Bundle params){
        String  strParam = "";
        for (String key : params.keySet()){
            strParam += key + "=" + params.getString(key) + "&";
        }
        strParam = strParam.substring(0,strParam.length()-1);
        return strParam;
    }

    public boolean isConnected() {
        return isConnected(context);
    }

    public static boolean isConnected(Context ctx) {
        ConnectivityManager connec =(ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);

        if ( connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTED ||

                connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTING ||
                connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTING ||
                connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTED ) {

            return true;
        }else if (
                connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.DISCONNECTED ||
                        connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.DISCONNECTED  ) {
            return false;
        }
        return false;
    }

    private HashMap<String,String> getHeaders(){
        return Constant.getHeaders();
    }

    public void addHeaders(String key,String value){
        headers.put(key,value);
    }

    public enum Type{
        TEXT,FILE
    }
    public void disconnect(){
        if(thread!=null) thread.interrupt();
        if(httpConn!=null)httpConn.disconnect();
    }
    private class MultiPartPost{
        private final String boundary;
        private final OutputStream outputStream;
        private final PrintWriter writer;
        private static final String LINE_FEED = "\n";
        private final String charset = "UTF-8";

        MultiPartPost(String requestURL)throws IOException{
            boundary = "===" + System.currentTimeMillis() + "===";

            URL url = new URL(requestURL);
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setUseCaches(false);
            httpConn.setDoOutput(true); // indicates POST method
            httpConn.setDoInput(true);
            httpConn.setInstanceFollowRedirects(false);
            httpConn.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + boundary);

            for(Map.Entry<String,String> entry:headers.entrySet()){
                httpConn.setRequestProperty(entry.getKey(),entry.getValue());
            }

            outputStream = httpConn.getOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(outputStream, charset),
                    true);
        }

        public void addFormField(String name, String value) {
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"" + name + "\"")
                    .append(LINE_FEED);
            writer.append("Content-Type: text/plain; charset=" + charset).append(
                    LINE_FEED);
            writer.append(LINE_FEED);
            writer.append(value).append(LINE_FEED);
            writer.flush();
        }

        /**
         * Adds a upload file section to the request
         * @param fieldName name attribute in <input type="file" name="..." />
         * @param uploadFile a File to be uploaded
         * @throws IOException
         */
        public void addFilePart(String fieldName, File uploadFile)
                throws IOException {
            String fileName = uploadFile.getName();
            writer.append("--" + boundary).append(LINE_FEED);
            writer.append(
                    "Content-Disposition: form-data; name=\"" + fieldName
                            + "\"; filename=\"" + fileName + "\"")
                    .append(LINE_FEED);
            writer.append(
                    "Content-Type: "
                            + URLConnection.guessContentTypeFromName(fileName))
                    .append(LINE_FEED);
            writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();

            FileInputStream inputStream = new FileInputStream(uploadFile);
            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            inputStream.close();

            writer.append(LINE_FEED);
            writer.flush();
        }

        /**
         * Completes the request and receives response from the server.
         * @return a list of Strings as response in case the server returned
         * status OK, otherwise an exception is thrown.
         * @throws IOException
         */
        public String execute() throws IOException {


            writer.append("--" + boundary + "--").append(LINE_FEED);
            writer.close();
            StringBuffer response = new StringBuffer();
            // checks server's status code first
            int status = httpConn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        httpConn.getInputStream()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                httpConn.disconnect();
            } else {
                throw new IOException("Server returned non-OK status: " + status +" url:"
                        +httpConn.getURL().toString());
            }

            return response.toString();
        }

        public void disconnect(){
            if(httpConn!=null){
                httpConn.disconnect();
                httpConn = null;
            }
        }
    }
}
