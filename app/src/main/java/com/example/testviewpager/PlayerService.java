package com.example.testviewpager;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.session.MediaButtonReceiver;

import com.example.testviewpager.activity.MainActivity;
import com.example.testviewpager.model.PlayerSharedPreferences;
import com.example.testviewpager.model.TrackCollection;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import static android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ALL;
import static android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_NONE;

public class PlayerService extends Service {

    private final String TAG = "Service";
    private final String NOTIFICATION_DEFAULT_CHANNEL_ID = "notification_channel";

    private MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();
    private MediaSessionCompat mediaSessionCompat;
    private final PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder().setActions(
            PlaybackStateCompat.ACTION_PLAY
                    | PlaybackStateCompat.ACTION_STOP
                    | PlaybackStateCompat.ACTION_PAUSE
                    | PlaybackStateCompat.ACTION_PLAY_PAUSE
                    | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
    );

    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean audioFocusRequested = false;

    private SimpleExoPlayer exoPlayer;

    private int lastState;
    int currentState = PlaybackStateCompat.STATE_STOPPED;

    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @SuppressLint("WrongConstant") NotificationChannel notificationChannel =
                    new NotificationChannel(NOTIFICATION_DEFAULT_CHANNEL_ID, "fff",
                            NotificationManagerCompat.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .setAcceptsDelayedFocusGain(false)
                    .setWillPauseWhenDucked(true)
                    .setAudioAttributes(audioAttributes)
                    .build();
        }
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mediaSessionCompat = new MediaSessionCompat(this, "PlayerService");
        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSessionCompat.setCallback(mediaSessionCallback);

        Context context = getApplicationContext();

        Intent activityIntent = new Intent(context, MainActivity.class);
        mediaSessionCompat.setSessionActivity(PendingIntent.getActivity(context, 0,
                activityIntent, 0));

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null, context,
                MediaButtonReceiver.class);
        mediaSessionCompat.setMediaButtonReceiver(PendingIntent.getBroadcast(context, 0,
                mediaButtonIntent, 0));

        exoPlayer = ExoPlayerFactory.newSimpleInstance(this, new DefaultRenderersFactory(this),
                new DefaultTrackSelector(), new DefaultLoadControl());
        exoPlayer.addListener(exoPlayerListener);

        final Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(MainActivity.BROADCAST_RECEIVER_PROGRESS);
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                broadcastIntent.putExtra(MainActivity.INTENT_PROGRESS,
                        (int) ((exoPlayer.getCurrentPosition() * 100) / exoPlayer.getDuration()));
                Log.i("progress:", String.valueOf((int) ((exoPlayer.getCurrentPosition() * 100) / exoPlayer.getDuration())));
                sendBroadcast(broadcastIntent);
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(runnable, 0);
    }

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN: {
                    exoPlayer.setVolume(1);
                    if (lastState == PlaybackStateCompat.STATE_PLAYING) {
                        mediaSessionCallback.onPlay();
                    }
                    break;
                }
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: {
                    exoPlayer.setVolume((float) 0.4);
                    break;
                }
                default: {
                    lastState = currentState;
                    mediaSessionCallback.onPause();
                }
            }
        }
    };

    private ExoPlayer.EventListener exoPlayerListener = new ExoPlayer.EventListener() {

        @Override
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
        }

        @Override
        public void onLoadingChanged(boolean isLoading) {
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            if (playWhenReady && playbackState == ExoPlayer.STATE_ENDED) {
                if(PlayerSharedPreferences.getRepeatMode() == REPEAT_MODE_NONE) {
                    mediaSessionCallback.onSkipToNext();
                }
                else if(PlayerSharedPreferences.getRepeatMode() == REPEAT_MODE_ALL) {
                    exoPlayer.seekTo(0);
                }
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
        }

        @Override
        public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        }
    };

    private MediaSessionCompat.Callback mediaSessionCallback = new MediaSessionCompat.Callback() {

        Uri currentTrackUri;

        @Override
        public void onPlay() {
            if (!exoPlayer.getPlayWhenReady()) {
                startService(new Intent(getApplicationContext(), PlayerService.class));
                TrackCollection.Track track = TrackCollection.getCurrentTrack();
                updateMetadata(track);
                prepareToPlay(track.getTrackUri());

                if (!audioFocusRequested) {
                    audioFocusRequested = true;
                    int audioFocusResult;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        audioFocusResult = audioManager.requestAudioFocus(audioFocusRequest);
                    } else {
                        audioFocusResult = audioManager.requestAudioFocus(audioFocusChangeListener,
                                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
                    }
                    if (audioFocusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        return;
                    }
                }
                mediaSessionCompat.setActive(true);
                registerReceiver(becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
                exoPlayer.setPlayWhenReady(true);
            }
            mediaSessionCompat.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1).build());
            currentState = PlaybackStateCompat.STATE_PLAYING;
            refreshNotification(currentState);
        }

        @Override
        public void onPause() {
            if (exoPlayer.getPlayWhenReady()) {
                exoPlayer.setPlayWhenReady(false);
                unregisterReceiver(becomingNoisyReceiver);
            }
            mediaSessionCompat.setPlaybackState(stateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1).build());
            currentState = PlaybackStateCompat.STATE_PAUSED;
            refreshNotification(currentState);
        }

        @Override
        public void onStop() {
            if (exoPlayer.getPlayWhenReady()) {
                exoPlayer.setPlayWhenReady(false);
                unregisterReceiver(becomingNoisyReceiver);
            }

            if (audioFocusRequested) {
                audioFocusRequested = false;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioManager.abandonAudioFocusRequest(audioFocusRequest);
                } else {
                    audioManager.abandonAudioFocus(audioFocusChangeListener);
                }
            }

            mediaSessionCompat.setActive(false);
            refreshNotification(currentState);
            stopSelf();
        }

        @Override
        public void onSkipToNext() {
            prepareToPlayAnotherTrack(TrackCollection.getNextTrack());
        }

        @Override
        public void onSkipToPrevious() {
            prepareToPlayAnotherTrack(TrackCollection.getPreviousTrack());
        }

        @Override
        public void onSkipToQueueItem(long id) {
            try {
                prepareToPlayAnotherTrack(TrackCollection.getTrackById((int) id));
                onPlay();
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }
        }

        private void prepareToPlayAnotherTrack(TrackCollection.Track track) {
            updateMetadata(track);
            refreshNotification(currentState);
            prepareToPlay(track.getTrackUri());
        }

        private void prepareToPlay(Uri trackUri) {
            if (!trackUri.equals(currentTrackUri)) {
                currentTrackUri = trackUri;
                String userAgent = Util.getUserAgent(getApplicationContext(), String.valueOf(R.string.app_name));
                MediaSource mediaSource = new ExtractorMediaSource(trackUri,
                        new DefaultDataSourceFactory(getApplicationContext(), userAgent),
                        new DefaultExtractorsFactory(),
                        null, null);
                exoPlayer.prepare(mediaSource);
            }
        }

        private void updateMetadata(TrackCollection.Track track) {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, track.getBitmap(getResources()));
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.getTitle());
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.getArtist());
            metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.getArtist());
            metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, track.getTrackId());
            mediaSessionCompat.setMetadata(metadataBuilder.build());
        }
    };

    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Disconnecting headphones - stop playback
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                mediaSessionCallback.onPause();
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new PlayerBinder();
    }

    private void refreshNotification(int playbackStatus) {
        int NOTIFICATION_ID = 203;
        switch (playbackStatus) {
            case PlaybackStateCompat.STATE_PLAYING: {
                startForeground(NOTIFICATION_ID, getNotification(playbackStatus));
                break;
            }
            case PlaybackStateCompat.STATE_PAUSED: {
                NotificationManagerCompat.from(PlayerService.this).notify(NOTIFICATION_ID,
                        getNotification(playbackStatus));
                stopForeground(false);
                break;
            }
            default: {
                stopForeground(true);
                break;
            }
        }
    }

    private Notification getNotification(int playbackState) {
        NotificationCompat.Builder builder = getBuilder();
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_previous,
                getString(R.string.previous), MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));
        if (playbackState == PlaybackStateCompat.STATE_PLAYING) {
            builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_pause,
                    getString(R.string.pause), MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                    PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        } else {
            builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_play,
                    getString(R.string.play), MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                    PlaybackStateCompat.ACTION_PLAY_PAUSE)));
        }
        builder.addAction(new NotificationCompat.Action(android.R.drawable.ic_media_next,
                getString(R.string.next), MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));
        builder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(1)
                .setShowCancelButton(true)
                .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_STOP))
                .setMediaSession(mediaSessionCompat.getSessionToken()));
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setShowWhen(false);
        builder.setPriority(NotificationCompat.PRIORITY_HIGH);
        builder.setOnlyAlertOnce(true);
        builder.setChannelId(NOTIFICATION_DEFAULT_CHANNEL_ID);

        return builder.build();
    }

    private NotificationCompat.Builder getBuilder() {
        MediaControllerCompat controllerCompat = mediaSessionCompat.getController();
        MediaMetadataCompat metadataCompat = controllerCompat.getMetadata();
        MediaDescriptionCompat descriptionCompat = metadataCompat.getDescription();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(descriptionCompat.getTitle())
                .setContentText(descriptionCompat.getSubtitle())
                .setSubText(descriptionCompat.getDescription())
                .setLargeIcon(descriptionCompat.getIconBitmap())
                .setContentIntent(controllerCompat.getSessionActivity())
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                        PlaybackStateCompat.ACTION_STOP))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        return builder;
    }


    public class PlayerBinder extends Binder {
        public MediaSessionCompat.Token getToken() {
            return mediaSessionCompat.getSessionToken();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mediaSessionCompat, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaSessionCompat.release();
        exoPlayer.release();
    }
}
