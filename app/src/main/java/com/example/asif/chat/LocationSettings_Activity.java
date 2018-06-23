package com.example.asif.chat;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LocationSettings_Activity extends AppCompatActivity {

    private Toolbar mtoolbar;
    private String CurrentUser;
    private DatabaseReference db;
    private Switch aSwitch;
    private int Stat=0;

    /*
    Always - 1
    9 to 5 -  2
    9 to 12 - 3
    12 to 5 - 4
    Never - 0
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_settings_);

        mtoolbar = (Toolbar)findViewById(R.id.LocSettings_toolbar);
        setSupportActionBar(mtoolbar);
        getSupportActionBar().setTitle("Set Location sharing");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CurrentUser = FirebaseAuth.getInstance().getCurrentUser().getUid().toString();
        db = FirebaseDatabase.getInstance().getReference().child("Users").child(CurrentUser);

        db.child("Loc").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String val = dataSnapshot.child("share").getValue().toString();
                Stat = Integer.parseInt(val);
                if (Stat==1){
                    aSwitch.setChecked(true);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        aSwitch = (Switch) findViewById(R.id.location);
        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if(b){
                    db.child("Loc").child("share").setValue(1);
                    Log.e("Location settings","Enabled");
                }
                else {
                    db.child("Loc").child("share").setValue(0);
                    Log.e("Location settings","Disabled");
                }

            }
        });


    }

}
