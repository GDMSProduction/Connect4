package project.connect4;

import android.content.Context;
import android.content.SharedPreferences;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by User on 11/14/2017.
 */

public class Networking {
    public static final String PREFS_NAME = "Strategic4Prefs";
    private static final String baseURL = "https://starcatcher.us/connect4/";
    public static final String ReqTAG = "MooTag";
    private static Context context;
    private static String uuid;
    static RequestQueue queue;

    public static void init(Context _context){
        context = _context;
        queue = Volley.newRequestQueue(context);

        // Restore preferences
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        uuid = settings.getString("uuid", "");
        if (uuid.equals("")){
            uuid = UUID.randomUUID().toString();
        }
    }
    public static void stopRequests(){
        queue.cancelAll(ReqTAG);
    }
    public static void exit(){
        stopRequests();
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("uuid", uuid);

        // Commit the edits!
        editor.commit();
    }

    public static void doRequest(String url, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener){
        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url , null, listener, errorListener);
        //jsObjRequest.setTag(ReqTAG);
        queue.add(jsObjRequest);
    }
    public static void Connect(int gameType, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener){
        String reqURL = baseURL + "test.lua?action=connect&game=" + gameType + "&uuid=" + uuid;

        doRequest(reqURL,listener,errorListener);
    }
    public static void GetTurns(int gameID, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener){
        String reqURL = baseURL + "test.lua?action=get&ID=" + gameID;

        doRequest(reqURL,listener,errorListener);
    }
    public static void SendEvent(int gameID, int _event, int _data, int _type, Response.Listener<JSONObject> listener){
        String reqURL = baseURL + "test.lua?action=move&ID=" + gameID + "&Event=" + _event + "&Data=" + _data + "&Type=" + _type;

        doRequest(reqURL,listener,error -> {});
    }
    public static void KeepAlive(int gameID){
        String reqURL = baseURL + "test.lua?action=keepAlive&ID=" + gameID;

        doRequest(reqURL,response -> {},error -> {});
    }
}
