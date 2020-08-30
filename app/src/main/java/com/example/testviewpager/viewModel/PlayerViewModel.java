package com.example.testviewpager.viewModel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.testviewpager.model.FileManager;
import com.example.testviewpager.model.TrackCollection;

public class PlayerViewModel extends AndroidViewModel {

    public PlayerViewModel(@NonNull Application application) {
        super(application);
        TrackCollection.setTracks(FileManager.getAudioFromDevice(application.getApplicationContext()));
    }
}
