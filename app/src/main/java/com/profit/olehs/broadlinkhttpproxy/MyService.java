package com.profit.olehs.broadlinkhttpproxy;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import java.io.IOException;

public class MyService extends Service {

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }

    @Override
    public void onCreate(){
        super.onCreate();}

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                BroadlinkHTTPServer server = new BroadlinkHTTPServer();
                try {
                    server.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread thread = new Thread(runnable);
        thread.start();
        return START_STICKY;
    }
}
