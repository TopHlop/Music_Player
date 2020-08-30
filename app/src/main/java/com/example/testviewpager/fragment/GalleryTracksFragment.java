package com.example.testviewpager.fragment;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.testviewpager.activity.MainActivity;
import com.example.testviewpager.databinding.FragmentGalleryTracksBinding;
import com.example.testviewpager.model.TrackCollection;

public class GalleryTracksFragment extends Fragment {

    private FragmentGalleryTracksBinding binding;
    private GalleryTracksAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentGalleryTracksBinding.inflate(inflater, container, false);
        initRecyclerView();
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private void initRecyclerView() {
        adapter = new GalleryTracksAdapter(getContext());
        binding.galleryTracksRecyclerView.setAdapter(adapter);
        binding.galleryTracksRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter.getSelectTrackId().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer selectTrackId) {
                ((MainActivity) getActivity()).playTrack(selectTrackId);
            }
        });
    }

    public void highlightSelectTrack(int trackId) {
        adapter.setCurrentTrackId(trackId);
    }

    public void mixTrack() {
        TrackCollection.mixTracks();
        adapter.notifyDataSetChanged();
    }
}