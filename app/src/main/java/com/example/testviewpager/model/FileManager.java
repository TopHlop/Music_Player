package com.example.testviewpager.model;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

public class FileManager {

    public static List<TrackCollection.Track> getAudioFromDevice(Context context) {
        List<TrackCollection.Track> tracks = new ArrayList<>();
        tracks.addAll(getAudioFromStorage(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, context));
        tracks.addAll(getAudioFromStorage(MediaStore.Audio.Media.INTERNAL_CONTENT_URI, context));
        return tracks;
    }

    private static List<TrackCollection.Track> getAudioFromStorage(Uri storageUri, Context context) {
        List<TrackCollection.Track> tracks = new ArrayList<>();
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " +
                MediaStore.Audio.Media.DATA + " LIKE '%.mp3'";

        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA
                //MediaStore.Audio.Media.CONTENT_TYPE
        };

        Cursor cursor = context.getContentResolver().query(
                storageUri,
                projection,
                selection,
                null,
                null);

        while (cursor.moveToNext()) {
            tracks.add(new TrackCollection.Track(cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    context.getResources()));
        }
        cursor.close();
        return tracks;
    }
}
