package com.example.nerdyvirus.bluetoothapp;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by nerdyvirus on 14/9/15.
 */
public class CustomFragmentPagerAdapter extends FragmentPagerAdapter {
    public CustomFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        switch (position)
        {
            case 0:
                return new ChatBoxFragment();
            case 1:
                return new AvailableDeviceFragment();
        }
        return null;
    }

    @Override
    public int getCount() {
        return 2;
    }
}
