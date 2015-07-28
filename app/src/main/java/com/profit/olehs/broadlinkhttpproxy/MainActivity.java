package com.profit.olehs.broadlinkhttpproxy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.gson.JsonObject;

import cn.com.broadlink.blnetwork.BLNetwork;

public class MainActivity extends Activity {
    private static Context context;

    private BLNetwork mBlNetwork;
    private String api_id = "api_id";
    private String command = "command";
    private String licenseValue = "IDqOTOuVhMNQz8XWEc2wqmrjuYeTDGtBlMkm6AT1mmKKNLTrl45x4KzHGywehG/TzmSMIDnemvSlaNMSyYceBTJnNVQ10LKQ9sNzVIBX21r87yx+quE=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MainActivity.context = MainActivity.this;

        mBlNetwork = BLNetwork.getInstanceBLNetwork(MainActivity.this);
        JsonObject initJsonObjectIn = new JsonObject();
        initJsonObjectIn.addProperty(api_id, 1);
        initJsonObjectIn.addProperty(command, "network_init");
        initJsonObjectIn.addProperty("license", licenseValue);
        String string = initJsonObjectIn.toString();
        mBlNetwork.requestDispatch(string);

        startService(new Intent(this, MyService.class));
    }

    public static Context getAppContext() {
        return MainActivity.context;
    }
}
