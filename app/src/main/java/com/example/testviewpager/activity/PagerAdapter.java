package com.example.testviewpager.activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.testviewpager.fragment.GalleryTracksFragment;
import com.example.testviewpager.fragment.PlayerTrackFragment;

class PagerAdapter extends FragmentStateAdapter {

    public static final int POSITION_PLAYER_TRACK_FRAGMENT = 0;
    public static final int POSITION_GALLERY_TRACKS_FRAGMENT = 1;
    private static final int NUM_FRAGMENTS = 2;

    private MutableLiveData<Boolean> isCreatePlayerTrackFragment = new MutableLiveData<>();
    private MutableLiveData<Boolean> isCreateGalleryTracksFragment = new MutableLiveData<>();

    public PagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case POSITION_PLAYER_TRACK_FRAGMENT:
                isCreatePlayerTrackFragment.postValue(true);
                return new PlayerTrackFragment();
            case POSITION_GALLERY_TRACKS_FRAGMENT:
                isCreateGalleryTracksFragment.postValue(true);
                return new GalleryTracksFragment();
            default:
                return new Fragment();
        }
    }

    @Override
    public int getItemCount() {
        return NUM_FRAGMENTS;
    }

    public LiveData<Boolean> isCreateGalleryTracksFragment() {
        return isCreateGalleryTracksFragment;
    }

    public LiveData<Boolean> isCreatePlayerTrackFragment() {
        return isCreatePlayerTrackFragment;
    }
}
