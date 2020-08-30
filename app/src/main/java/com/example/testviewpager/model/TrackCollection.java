package com.example.testviewpager.model;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;

import com.example.testviewpager.R;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class TrackCollection {

    private static LinkedList<Track> tracks = new LinkedList<>();

    private static int currentTrackPosition = 0;

    public static void setTracks(List<Track> newTracks) {
        tracks.addAll(newTracks);
    }

    public static void mixTracks() {
        final Random random = new Random();
        final int currentTrackId = tracks.get(currentTrackPosition).getTrackId();
        Track trackBasket;
        int numMix;
        for (int i = 0; i < tracks.size(); i++) {
            numMix = random.nextInt(tracks.size());
            if (numMix == i) {
                continue;
            }
            trackBasket = tracks.get(i);
            tracks.set(i, tracks.get(numMix));
            tracks.set(numMix, trackBasket);
            //change currentTrackPosition
            if (tracks.get(i).getTrackId() == currentTrackId) {
                currentTrackPosition = i;
            } else if (tracks.get(numMix).getTrackId() == currentTrackId) {
                currentTrackPosition = numMix;
            }
        }

    }

    public static Track getFirstTrack() {
        return tracks.getFirst();
    }

    public static Track getNextTrack() {
        if (currentTrackPosition == tracks.size() - 1) {
            currentTrackPosition = 0;
        } else {
            currentTrackPosition++;
        }
        return getCurrentTrack();
    }

    public static Track getPreviousTrack() {
        if (currentTrackPosition == 0) {
            currentTrackPosition = tracks.size() - 1;
        } else {
            currentTrackPosition--;
        }
        return getCurrentTrack();
    }

    public static Track getCurrentTrack() {
        return tracks.get(currentTrackPosition);
    }

    public static void setCurrentTrackPosition(int currentTrackPosition) {
        TrackCollection.currentTrackPosition = currentTrackPosition;
    }

    public static int getCurrentTrackPosition() {
        return currentTrackPosition;
    }

    public static int getSize() {
        return tracks.size();
    }

    public static Track getTrack(int position) {
        return tracks.get(position);
    }

    public static Track getTrackById(int trackId) throws Exception {
        for (Track track : tracks) {
            if (track.getTrackId() == trackId) {
                return track;
            }
        }
        throw new Exception("Track not found!");
    }

    public static class Track {

        private int trackId;
        private String title;
        private String artist;
        private String path;
        private Bitmap albumArt;

        public Track(int trackId, String artist, String title, String path, Resources resources) {
            this.artist = artist;
            this.title = title;
            this.trackId = trackId;
            this.path = path;
            AlbumArtTask albumArtTask = new AlbumArtTask();
            albumArtTask.execute(resources);
        }

        public String getTitle() {
            return title;
        }

        public String getArtist() {
            if (artist.contains("<unknown>")) {
                return " ";
            }
            return artist;
        }

        public Bitmap getBitmap(Resources resources) {
            if(albumArt == null) {
                return BitmapFactory.decodeResource(resources, R.drawable.placeholder);
            }
            else {
                return albumArt;
            }
        }

        public int getTrackId() {
            return trackId;
        }

        public Uri getTrackUri() {
            return Uri.fromFile(new File(path));
        }

        @SuppressLint("StaticFieldLeak")
        private class AlbumArtTask extends AsyncTask<Resources, Void, Bitmap> {

            @Override
            protected Bitmap doInBackground(Resources... resources) {
                MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
                metadataRetriever.setDataSource(path);
                byte[] data = metadataRetriever.getEmbeddedPicture();
                if (data != null) {
                    return BitmapFactory.decodeByteArray(data, 0, data.length);
                } else {
                    return BitmapFactory.decodeResource(resources[0], R.drawable.placeholder);
                }
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                albumArt = bitmap;
            }
        }
    }
}
