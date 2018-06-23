package com.example.asif.chat;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class LocationFragment extends Fragment {

    private RecyclerView mFriendsList;
    private DatabaseReference friendsdb;
    private DatabaseReference mUsersDatabase;

    private FirebaseAuth mAuth;

    private String Current_UserId;
    private View view;

    public LocationFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_friends, container, false);

        mFriendsList = (RecyclerView)view.findViewById(R.id.friends_list);
        mAuth = FirebaseAuth.getInstance();
        Current_UserId = mAuth.getCurrentUser().getUid();

        friendsdb = FirebaseDatabase.getInstance().getReference().child("Friends").child(Current_UserId);
        friendsdb.keepSynced(true);
        mUsersDatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        mUsersDatabase.keepSynced(true);

        mFriendsList.setHasFixedSize(true);
        mFriendsList.setLayoutManager(new LinearLayoutManager(getContext()));
        return view;    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerAdapter<Friends, FriendsViewHolder> friendsRecyclerViewAdapter = new FirebaseRecyclerAdapter<Friends, FriendsViewHolder>(

                Friends.class,
                R.layout.user_single_location,
                FriendsViewHolder.class,
                friendsdb


        ) {
            @Override
            protected void populateViewHolder(final FriendsViewHolder friendsViewHolder, Friends friends, int i) {


                final String list_user_id = getRef(i).getKey();


                mUsersDatabase.child(list_user_id).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        final String userName = dataSnapshot.child("name").getValue().toString();
                        String userThumb = dataSnapshot.child("thumbnail").getValue().toString();

                        String location = null,lastSeenTime = null;
                        long time;
                        if(dataSnapshot.child("Loc").hasChild("location") && dataSnapshot.child("Loc").hasChild("loc_time")) {
                            location = dataSnapshot.child("Loc").child("location").getValue().toString();
                            time = Long.valueOf(dataSnapshot.child("Loc").child("loc_time").getValue().toString());
                            GetTimeAgo getTimeAgo = new GetTimeAgo();
                            lastSeenTime = getTimeAgo.getTimeAgo(time, getContext());
                        }

                        if(dataSnapshot.hasChild("Online")) {

                            String userOnline = dataSnapshot.child("Online").getValue().toString();
                            friendsViewHolder.setUserOnline(userOnline);

                        }

                        friendsViewHolder.setName(userName);
                        friendsViewHolder.setUserImage(userThumb, getContext());


                        friendsViewHolder.setLocation(location);
                        friendsViewHolder.setUserLastSeen(lastSeenTime);

                        friendsViewHolder.mView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                CharSequence options[] = new CharSequence[]{"Ping Location", "Send message","Show in map"};

                                final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

                                builder.setTitle("Select Options");
                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                        //Click Event for each item.
                                        if(i == 0){
                                            mUsersDatabase.child(list_user_id).child("Loc").child("ping").setValue(1);

                                        }

                                        if(i == 1){

                                            Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                            chatIntent.putExtra("user_id", list_user_id);
                                            chatIntent.putExtra("user_name", userName);
                                            startActivity(chatIntent);
                                        }

                                        if(i==2){
                                            OpenMaps(list_user_id);
                                        }

                                    }
                                });

                                builder.show();

                            }
                        });


                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        };

        mFriendsList.setAdapter(friendsRecyclerViewAdapter);

    }

    private void OpenMaps(String list_user_id) {
        Log.e("Open maps","user id: "+list_user_id);
        mUsersDatabase.child(list_user_id).child("Loc").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.e("Open maps","in value event listener");

                if(dataSnapshot.hasChild("lattitude") && dataSnapshot.hasChild("longitude")) {

                    String lat = dataSnapshot.child("lattitude").getValue().toString();
                    String longt = dataSnapshot.child("longitude").getValue().toString();

                    Log.e("Open maps",lat+" "+longt);

                    String strUri = "http://maps.google.com/maps?q=loc:" + lat + "," + longt + " (" + "Here" + ")";
                    Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(strUri));
                    intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                    startActivity(intent);

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public static class FriendsViewHolder extends RecyclerView.ViewHolder {

        View mView;

        public FriendsViewHolder(View itemView) {
            super(itemView);

            mView = itemView;

        }
        public void setLocation(String location){

            TextView userStatusView = (TextView) mView.findViewById(R.id.User_status);
            userStatusView.setText(location);

        }

        public void setName(String name){

            TextView userNameView = (TextView) mView.findViewById(R.id.User_name);
            userNameView.setText(name);

        }

        public void setUserImage(String thumb_image, Context ctx){

            CircleImageView userImageView = (CircleImageView) mView.findViewById(R.id.User_image);
            Picasso.with(ctx).load(thumb_image).placeholder(R.drawable.picture).into(userImageView);

        }
        public void setUserOnline(String online_status) {

            ImageView userOnlineView = (ImageView) mView.findViewById(R.id.user_single_online_icon);

            if(online_status.equals("true")){
                userOnlineView.setVisibility(View.VISIBLE);
            } else {
                userOnlineView.setVisibility(View.INVISIBLE);
            }
        }
        public void setUserLastSeen(String timeAgo){

            TextView time = (TextView)mView.findViewById(R.id.User_lastSeen);
            time.setText("Last seen: "+timeAgo);
        }
    }

}

