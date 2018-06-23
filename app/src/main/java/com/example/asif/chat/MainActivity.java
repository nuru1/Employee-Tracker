package com.example.asif.chat;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Toolbar mtoolbar;
    private ViewPager mviewPager;
    private TabsPagerAdapter tabsPagerAdapter;
    private TabLayout mtabLayout;
    private DatabaseReference userdb;
    private FloatingActionButton floatingActionButton;

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST=4321;

    private Intent serviceIntent;
    private Intent Location_serviceIntent;

    private String Glink="https://drive.google.com/open?id=1Fq2olyy-z4JKk4k5UBmTas-ag1UnWM0g";
    private String msg = "Heyy try this app: ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        floatingActionButton = (FloatingActionButton)findViewById(R.id.floatBtn);

        mAuth = FirebaseAuth.getInstance();
        mtoolbar = (Toolbar)findViewById(R.id.app_toolbar);
        setSupportActionBar(mtoolbar);
        getSupportActionBar().setTitle("Thikana");
        if (mAuth.getCurrentUser() != null) {
            userdb = FirebaseDatabase.getInstance().getReference().child("Users").child(mAuth.getCurrentUser().getUid());
            userdb.child("Loc").child("ping").setValue(0);
            serviceIntent = new Intent(getApplicationContext(),LocationService.class);
            Location_serviceIntent = new Intent(getApplicationContext(),LocationShare_service.class);
            if(checkPlayServices()){
                if(!isMyServiceRunning(LocationService.class))
                    startService(serviceIntent);
                if(!isMyLocation_ServiceRunning(LocationShare_service.class))
                    startService(Location_serviceIntent);
            }else
                Log.e("MaiActivity","No play services");
        }
        mviewPager=(ViewPager)findViewById(R.id.viewPager);
        tabsPagerAdapter = new TabsPagerAdapter(getSupportFragmentManager());
        mviewPager.setAdapter(tabsPagerAdapter);
        mtabLayout=(TabLayout)findViewById(R.id.tabLayout);
        mtabLayout.setupWithViewPager(mviewPager);

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent all_users = new Intent(MainActivity.this,UsersActivity.class);
                startActivity(all_users);
            }
        });
    }

    private boolean isMyServiceRunning(Class<? extends LocationService> aClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (aClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.e ("isMyServiceRunning?", false+"");
        return false;
    }

    private boolean isMyLocation_ServiceRunning(Class<? extends LocationShare_service> aClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (aClass.getName().equals(service.service.getClassName())) {
                Log.e ("isMyLoc_ServiceRunning?", true+"");
                return true;
            }
        }
        Log.e ("isMyLoc_ServiceRunning?", false+"");
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();

        if(checkPlayServices())
        setupLocation();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser == null){
            toStart();
        }
        else {
            userdb.child("Online").setValue("true");
            if(checkPlayServices()){
                if(!isMyServiceRunning(LocationService.class))
                    startService(serviceIntent);
                if(!isMyLocation_ServiceRunning(LocationShare_service.class))
                    startService(Location_serviceIntent);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null) {

            userdb.child("Online").setValue(ServerValue.TIMESTAMP);
        }
    }

    private void toStart() {
        Intent intent = new Intent(MainActivity.this,StartActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_items,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if(item.getItemId() == R.id.lout){

            if(FirebaseAuth.getInstance().getCurrentUser().getUid() != null) {
                userdb.child("Online").setValue(ServerValue.TIMESTAMP);
            }
            FirebaseAuth.getInstance().signOut();
            toStart();
        }
        if(item.getItemId()==R.id.settings_account){
            Intent settings = new Intent(MainActivity.this,SettingsActivity.class);
            startActivity(settings);
        }
        if(item.getItemId()==R.id.share){
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, msg+Glink);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        //stopService(serviceIntent);
        super.onDestroy();
    }

    private void setupLocation() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1234);

        }
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, (Activity) getApplicationContext(), PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(), "This device is not supported", Toast.LENGTH_SHORT).show();
                Log.e("Location Services", "Google Play services is unavailable.");
            }
            return false;
        }
        return true;
    }
}