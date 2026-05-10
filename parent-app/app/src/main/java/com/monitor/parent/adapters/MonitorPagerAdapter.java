package com.monitor.parent.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.monitor.parent.fragments.AudioPlayerFragment;
import com.monitor.parent.fragments.CallLogFragment;
import com.monitor.parent.fragments.CameraViewFragment;
import com.monitor.parent.fragments.LocationFragment;
import com.monitor.parent.fragments.MessagesFragment;
import com.monitor.parent.fragments.NotificationsFragment;

public class MonitorPagerAdapter extends FragmentStateAdapter {

    public MonitorPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new CameraViewFragment();
            case 1: return new AudioPlayerFragment();
            case 2: return new LocationFragment();
            case 3: return new CallLogFragment();
            case 4: return new MessagesFragment();
            case 5: return new NotificationsFragment();
            default: return new CameraViewFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 6;
    }
}
