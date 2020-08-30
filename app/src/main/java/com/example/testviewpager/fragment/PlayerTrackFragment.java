package com.example.testviewpager.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.testviewpager.activity.MainActivity;
import com.example.testviewpager.databinding.FragmentPlayerTrackBinding;


public class PlayerTrackFragment extends Fragment {

    private FragmentPlayerTrackBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentPlayerTrackBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        binding.textTitlePlayer.setSelected(true);

        binding.playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).playMusic();
            }
        });

        binding.pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).pauseMusic();
            }
        });

        binding.nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).skipToNextTrack();
            }
        });

        binding.prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).skipToPreviousTrack();
            }
        });
    }

    public void isPauseTrack() {
        binding.playButton.setVisibility(View.VISIBLE);
        binding.pauseButton.setVisibility(View.GONE);
    }

    public void isPlayTrack() {
        binding.pauseButton.setVisibility(View.VISIBLE);
        binding.playButton.setVisibility(View.GONE);
    }

    public void setProgressTrack(int progressTrack) {
        binding.progressMusic.setProgress(progressTrack);
    }

    public void initTrackViews(String title, String artist, Bitmap image) {
        binding.textArtistPlayer.setText(artist);
        binding.textTitlePlayer.setText(title);
        binding.albumArtPlayer.setImageBitmap(image);
    }
}