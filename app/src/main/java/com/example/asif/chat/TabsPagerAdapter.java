package com.example.asif.chat;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by asif on 08-Jan-18.
 */

class TabsPagerAdapter extends FragmentPagerAdapter{

    public TabsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:
                RequestsFragment requestsFragment = new RequestsFragment();
                return requestsFragment;
            case 1:
                ChatsFragment chatsFragment = new ChatsFragment();
                return chatsFragment;
            case 2:
                FriendsFragment friendsFragment = new FriendsFragment();
                return friendsFragment;
            case 3:
                LocationFragment locationFragment = new LocationFragment();
                return locationFragment;
            default:
                return null;
        }

    }

    @Override
    public int getCount() {
        return 4;
    }

    public CharSequence getPageTitle(int position){
        switch (position){
            case 0:
                return "Authentication Requests";
            case 1:
                return "Chats";
            case 2:
                return "Contacs";
            case 3:
                return "Locations";
            default:
                return null;
        }
    }
}
