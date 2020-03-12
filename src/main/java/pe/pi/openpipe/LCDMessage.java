/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pe.pi.openpipe;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/**
 *
 * @author tim
 */
public class LCDMessage {

    public String to;
    public String from;
    public String session;
    @SerializedName("type")
    public String ltype;
    public String candidate;
    public String ufrag;
    public String upass;
    public String fingerprint;

    static LCDMessage fromJson(String json) {
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        return gson.fromJson(json, LCDMessage.class);
    }
    public String toString(){
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String ret = gson.toJson(this);
        return ret;
    }
}
