package com.example.asif.chat;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

/**
 * Created by asif on 16-May-18.
 */

public class LocationShare_service extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

     LocationShare_service(){ }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getApplicationContext(),"Location service Destroyed!!",Toast.LENGTH_SHORT).show();
        //Intent broadcast = new Intent("RestartService");
        //sendBroadcast(broadcast);
        Intent serviceIntent = new Intent(getApplicationContext(),LocationShare_service.class);
        //startService(serviceIntent);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);

    }
}
