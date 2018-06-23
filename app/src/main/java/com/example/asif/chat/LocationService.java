package com.example.asif.chat;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

import static android.content.ContentValues.TAG;
import static com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY;


public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "MyLocationService";
    private DatabaseReference Ping_db;
    private DatabaseReference UserLocation_ref;
    private DatabaseReference shareDb;
    private DatabaseReference UsersDb;
    private String CurrentUser = null;
    private static Context context;
    private int ping, share;

    private Location mLastLocation;
    private LocationRequest LocationRequest;
    private LocationRequest mLocationRequest;
    private GoogleApiClient googleApiClient;
    private double longitude;
    private double lattitude;
    private String addrs;
    private FusedLocationProviderClient mFusedLocationClient;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback mLocationCallback;
    private LocationCallback Single_LocationCallBack;

    public LocationService() {
    }

    public LocationService(String CurrentUser) {
        this.CurrentUser = CurrentUser;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Log.e("service  : ", "Started!!!");

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            onDestroy();
            Log.e("service  : ", "Destroyed!!!");
        }

        CurrentUser = FirebaseAuth.getInstance().getCurrentUser().getUid();

        UserLocation_ref = FirebaseDatabase.getInstance().getReference().child("Location").child(CurrentUser);
        UsersDb = FirebaseDatabase.getInstance().getReference().child("Users").child(CurrentUser);
        Ping_db = FirebaseDatabase.getInstance().getReference().child("Users").child(CurrentUser).child("Loc").child("ping");
        shareDb = UsersDb.child("Loc").child("share");

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        buildAPiClient();
        createLocationRequest();

        Ping_db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    ping = Integer.parseInt(dataSnapshot.getValue().toString());
//                Toast.makeText(getApplicationContext(), "Data change done   " + ping, Toast.LENGTH_SHORT).show();

                    if (ping == 1) {
                        create_Single_LocationRequest();
                        start_single_LocationUpdate();
                        //String address = displayLocation();
                        //UserLocation_ref.child("loc").setValue(address);
                    }
                    Ping_db.setValue(0);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        shareDb.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() != null) {
                    share = Integer.parseInt(dataSnapshot.getValue().toString());
                    Log.e("location", "Share status :" + share);

                    if (share == 1)
                        startLocationUpdates();
                    else if (share == 0)
                        removeLocationUpdates();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Log.e("RESULT", "No LOCATION");
                    updateLocation("Location Unavailable",0,0);
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    double longt = location.getLongitude();
                    double latt = location.getLatitude();

                    Log.e("RESULT", "  " + latt + "  " + longt);
                    String a = getAdress(latt, longt);
                    updateLocation(a,latt,longt);
                }
            }

            ;
        };

        Single_LocationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

                Log.e("locationCallBack", "   Single Location CallBack");

                if (locationResult == null) {
                    Log.e("RESULT", "No LOCATION");
                    updateLocation("Location Unavailable",0,0);
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    double longt = location.getLongitude();
                    double latt = location.getLatitude();

                    Log.e("RESULT", "  " + latt + "  " + longt);
                    String a = getAdress(latt, longt);
                    updateLocation(a,latt,longt);
                }
            }

            ;
        };


        return START_STICKY;

    }

    private void removeLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(getApplicationContext(), "Destroyed!!", Toast.LENGTH_SHORT).show();
        //Intent broadcast = new Intent("RestartService");
        //sendBroadcast(broadcast);
        Intent serviceIntent = new Intent(getApplicationContext(), LocationService.class);
        //startService(serviceIntent);
    }

    private void buildAPiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    private void createLocationRequest() {

        LocationRequest = new LocationRequest();
        LocationRequest.setInterval(10000);
        LocationRequest.setFastestInterval(50000);
        LocationRequest.setSmallestDisplacement(10);
        LocationRequest.setPriority(PRIORITY_HIGH_ACCURACY);
    }

    private void create_Single_LocationRequest() {

        mLocationRequest = new LocationRequest();
        //mLocationRequest.setNumUpdates(10);
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(PRIORITY_HIGH_ACCURACY);
    }


    private String displayLocation() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("location", "returning....");
            return "location access denied";
        }
        Log.e("location", "Locationg........");
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (mLastLocation != null) {
            longitude = mLastLocation.getLongitude();
            lattitude = mLastLocation.getLatitude();
            Log.e("Current Location", "Location changed    " + longitude + " : " + lattitude);
            addrs = getAdress(longitude, lattitude);
            if (!addrs.isEmpty())
                updateLocation(addrs,lattitude,longitude);
        } else {
            Log.d("Current Location", "cannot get Location ");
            if (FirebaseAuth.getInstance().getCurrentUser() != null)
                UserLocation_ref.child("loc").setValue("cannot get location");

        }
        Log.e("Current Location", "Location changed    " + longitude + " : " + lattitude);

        return "cannot get location";
    }

    private void updateLocation(String addrs, double lat, double longt) {
        UsersDb.child("Loc").child("location").setValue(addrs);
        UsersDb.child("Loc").child("loc_time").setValue(ServerValue.TIMESTAMP);
        UsersDb.child("Loc").child("longitude").setValue(longt);
        UsersDb.child("Loc").child("lattitude").setValue(lat);
        UserLocation_ref.child("loc").setValue(addrs);
        UserLocation_ref.child("longitude").setValue(longt);
        UserLocation_ref.child("lattitude").setValue(lat);
    }

    public String getAdress(double latt, double longt) {

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latt, longt, 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder("");
                for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
                    Log.e(" loction address", "Lo++++++++++++++++++     " + strReturnedAddress);
                }
                addrs = strReturnedAddress.toString();
                Log.e(" address", addrs);
                //UserLocation_ref.child("loc").setValue(addrs);
                //Toast.makeText(getApplicationContext(),addrs,Toast.LENGTH_SHORT);
                return addrs;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(" loction address", "Canont get Address!!!");
            //UserLocation_ref.child("loc").setValue("cannot get location");
            Toast.makeText(getApplicationContext(), "no location!!", Toast.LENGTH_SHORT);
        }
        return "No Location Available";
    }


    private void start_single_LocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(final Location location) {
                if (location != null) {
                    Log.e("ping location","Available");
                    String adr = getAdress(location.getLatitude(), location.getLongitude());
                    updateLocation(adr,location.getLatitude(),location.getLongitude());
                } else {
                    Log.e("ping location","UnAvailable");

                    LocationRequest req = new LocationRequest().setInterval(1000)
                            .setFastestInterval(5000).setPriority(PRIORITY_HIGH_ACCURACY).setNumUpdates(3).setSmallestDisplacement(10);

                    fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    fusedLocationProviderClient.requestLocationUpdates(req, Single_LocationCallBack, null);
                }

            }
        });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Log.e("fused location","starting updates....");
        mFusedLocationClient.requestLocationUpdates(LocationRequest,
                mLocationCallback,
                null /* Looper */);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.e("GoogleAPiClient","GoogleAPiClient Connected");

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e("GoogleAPiClient","GoogleAPiClient Connection suspended");
        googleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("GoogleAPiClient","GoogleAPiClient Connection Failed");

        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult((Activity) getApplicationContext(), 1);
            } catch (IntentSender.SendIntentException e) {
                Log.e("Connection Failed", "Exception while resolving connection error.", e);
            }
        } else {
            int errorCode = connectionResult.getErrorCode();
            Log.e("Connection Failed", "Connection to Google Play services failed with error code " + errorCode);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e("Location","Location Changed");
        mLastLocation = location;
        String adress = displayLocation();
        Log.e("Location","adress "+adress+ServerValue.TIMESTAMP);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        Log.e("Location","Location status changed ");

    }

    @Override
    public void onProviderEnabled(String s) {
        Log.e("Location","Location provider enabled");

    }

    @Override
    public void onProviderDisabled(String s) {
        Log.e("Location","Location provider disabled");

    }
}
