package com.example.novelix.Adapter;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.novelix.Fragment.WelcomeFragment;
import com.example.novelix.R;

public class WelcomePagerAdapter extends FragmentStateAdapter {
    private static final int[] LAYOUT_IDS = {
            R.layout.fragment_welcome1,
            R.layout.fragment_welcome2,
            R.layout.fragment_welcome3,
            R.layout.fragment_welcome4
    };

    public WelcomePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        WelcomeFragment fragment = new WelcomeFragment();
        Bundle args = new Bundle();
        args.putInt(WelcomeFragment.ARG_LAYOUT_ID, LAYOUT_IDS[position]);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getItemCount() {
        return LAYOUT_IDS.length;
    }
}
