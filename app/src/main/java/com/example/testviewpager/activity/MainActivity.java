package com.example.testviewpager.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PermissionChecker;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager2.widget.ViewPager2;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.View;

import com.example.testviewpager.PlayerService;
import com.example.testviewpager.R;
import com.example.testviewpager.databinding.ActivityMainBinding;
import com.example.testviewpager.fragment.GalleryTracksFragment;
import com.example.testviewpager.fragment.PlayerTrackFragment;
import com.example.testviewpager.model.PlayerSharedPreferences;
import com.example.testviewpager.model.TrackCollection;
import com.example.testviewpager.viewModel.PlayerViewModel;

import static android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ALL;
import static android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_NONE;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private static final int REQUEST_CODE_PERMISSIONS = 101;
    private static final String[] REQUIRED_PERMISSIONS =
            new String[]{"android.permission.READ_EXTERNAL_STORAGE"};

    public static final String BROADCAST_RECEIVER_PROGRESS = "progress";
    public static final String INTENT_PROGRESS = "progressVal";
    private BroadcastReceiverProgress receiverProgress;

    private MediaControllerCompat mediaController;
    private MediaControllerCompat.Callback callback;
    private PlayerService.PlayerBinder playerBinder;

    private ServiceConnection serviceConnection;

    private PlayerTrackFragment playerTrackFragment;
    private GalleryTracksFragment galleryTracksFragment;

    private PlayerViewModel playerViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        PlayerSharedPreferences.loadSettings(getApplicationContext());
        setColorOfRepeatTrackButton();

        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(this,
                    REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        } else {
            initViewPager();
            playerViewModel = ViewModelProviders.of(this).get(PlayerViewModel.class);
        }


        initService();

        binding.mixTrackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                galleryTracksFragment.mixTrack();
            }
        });
        binding.repeatTrackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PlayerSharedPreferences.getRepeatMode() == REPEAT_MODE_ALL) {
                    PlayerSharedPreferences.setRepeatMode(REPEAT_MODE_NONE, getApplicationContext());
                } else if (PlayerSharedPreferences.getRepeatMode() == REPEAT_MODE_NONE) {
                    PlayerSharedPreferences.setRepeatMode(REPEAT_MODE_ALL, getApplicationContext());
                }
                setColorOfRepeatTrackButton();
            }
        });
    }

    private void setColorOfRepeatTrackButton() {
        if (PlayerSharedPreferences.getRepeatMode() == REPEAT_MODE_ALL) {
            binding.repeatTrackButton.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                    R.color.select_point));
        } else if (PlayerSharedPreferences.getRepeatMode() == REPEAT_MODE_NONE) {
            binding.repeatTrackButton.setColorFilter(ContextCompat.getColor(getApplicationContext(),
                    R.color.unselect_point));
        }
    }

    private void initService() {
        IntentFilter filter = new IntentFilter(BROADCAST_RECEIVER_PROGRESS);
        receiverProgress = new BroadcastReceiverProgress();
        registerReceiver(receiverProgress, filter);

        callback = new MediaControllerCompat.Callback() {
            @Override
            public void onMetadataChanged(MediaMetadataCompat metadata) {
                if (playerTrackFragment == null)
                    return;
                if (metadata == null) {
                    TrackCollection.Track track = TrackCollection.getFirstTrack();
                    playerTrackFragment.initTrackViews(track.getTitle(), track.getArtist(),
                            track.getBitmap(getResources()));
                    if (galleryTracksFragment != null) {
                        galleryTracksFragment.highlightSelectTrack(track.getTrackId());
                    }
                } else {
                    playerTrackFragment.initTrackViews((String) metadata.getText(MediaMetadataCompat.METADATA_KEY_TITLE),
                            (String) metadata.getText(MediaMetadataCompat.METADATA_KEY_ARTIST),
                            metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART));
                    if (galleryTracksFragment != null) {
                        galleryTracksFragment.highlightSelectTrack(
                                (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER));
                    }
                }
            }

            @Override
            public void onPlaybackStateChanged(PlaybackStateCompat state) {
                if (state == null || playerTrackFragment == null)
                    return;
                if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                    playerTrackFragment.isPlayTrack();
                } else {
                    playerTrackFragment.isPauseTrack();
                }

            }
        };

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                playerBinder = (PlayerService.PlayerBinder) service;
                try {
                    Log.d("Tag", "подключил");
                    mediaController = new MediaControllerCompat(MainActivity.this, playerBinder.getToken());
                    mediaController.registerCallback(callback);
                    callback.onPlaybackStateChanged(mediaController.getPlaybackState());
                    callback.onMetadataChanged(mediaController.getMetadata());
                } catch (RemoteException e) {
                    mediaController = null;
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                playerBinder = null;
                if (mediaController != null) {
                    mediaController.unregisterCallback(callback);
                    mediaController = null;
                }
            }
        };

        bindService(new Intent(this, PlayerService.class), serviceConnection, BIND_AUTO_CREATE);
    }

    private void initViewPager() {
        ViewPager2 viewPager2 = findViewById(R.id.view_pager);
        PagerAdapter adapter = new PagerAdapter(this);
        viewPager2.setAdapter(adapter);
        adapter.isCreatePlayerTrackFragment().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isCreatePlayerTrackFragment) {
                if (isCreatePlayerTrackFragment) {
                    initPlayerTrackFragment();
                    TrackCollection.Track track = TrackCollection.getFirstTrack();
                    playerTrackFragment.initTrackViews(track.getTitle(), track.getArtist(),
                            track.getBitmap(getResources()));
                    if (galleryTracksFragment != null) {
                        galleryTracksFragment.highlightSelectTrack(track.getTrackId());
                    }
                }
            }
        });
        adapter.isCreateGalleryTracksFragment().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isCreateGalleryTracksFragment) {
                if (isCreateGalleryTracksFragment) {
                    initGalleryTracksFragment();
                }
            }
        });
        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                switch (position) {
                    case 0:
                        isPlayerSelected();
                        break;
                    case 1:
                        isGalleryTracksSelected();
                        break;
                }
            }
        });

        initPlayerTrackFragment();
        initGalleryTracksFragment();
    }

    private void initPlayerTrackFragment() {
        playerTrackFragment = (PlayerTrackFragment) getSupportFragmentManager().
                findFragmentByTag("f" + PagerAdapter.POSITION_PLAYER_TRACK_FRAGMENT);
    }

    private void initGalleryTracksFragment() {
        galleryTracksFragment = (GalleryTracksFragment) getSupportFragmentManager().
                findFragmentByTag("f" + PagerAdapter.POSITION_GALLERY_TRACKS_FRAGMENT);
    }

    private void isPlayerSelected() {
        binding.playerFragmentPoint.setColorFilter(ContextCompat.getColor(this, R.color.select_point));
        binding.galleryFragmentPoint.setColorFilter(ContextCompat.getColor(this, R.color.unselect_point));
        binding.mixTrackButton.setVisibility(View.GONE);
    }

    private void isGalleryTracksSelected() {
        binding.playerFragmentPoint.setColorFilter(ContextCompat.getColor(this, R.color.unselect_point));
        binding.galleryFragmentPoint.setColorFilter(ContextCompat.getColor(this, R.color.select_point));
        binding.mixTrackButton.setVisibility(View.VISIBLE);
    }

    public void playMusic() {
        if (mediaController != null) {
            mediaController.getTransportControls().play();
        }
    }

    public void playTrack(int trackId) {
        if (mediaController != null) {
            mediaController.getTransportControls().skipToQueueItem(trackId);
        }
    }

    public void pauseMusic() {
        if (mediaController != null) {
            mediaController.getTransportControls().pause();
        }
    }

    public void skipToNextTrack() {
        if (mediaController != null) {
            mediaController.getTransportControls().skipToNext();
        }
    }

    public void skipToPreviousTrack() {
        if (mediaController != null) {
            mediaController.getTransportControls().skipToPrevious();

        }
    }

    private boolean allPermissionsGranted() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), REQUIRED_PERMISSIONS[0]) !=
                PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                initViewPager();
                playerViewModel = ViewModelProviders.of(this).get(PlayerViewModel.class);
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {
                    finish();
                } else if (PermissionChecker.checkCallingOrSelfPermission(this, REQUIRED_PERMISSIONS[0]) !=
                        PermissionChecker.PERMISSION_GRANTED) {
                    showPermissionAlertDialog();
                }
            }
        }
    }

    private void showPermissionAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.message_read_storage_permission_alert_dialog)
                .setTitle(R.string.title_permission_alert_dialog)
                .setPositiveButton(R.string.sure_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton(R.string.set_permission_button, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + MainActivity.this.getPackageName())));
                        finish();
                    }
                })
                .setCancelable(false);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private class BroadcastReceiverProgress extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (playerTrackFragment == null)
                return;
            playerTrackFragment.setProgressTrack(intent.getIntExtra(INTENT_PROGRESS, 0));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerBinder = null;
        unregisterReceiver(receiverProgress);
        if (mediaController != null) {
            mediaController.unregisterCallback(callback);
            mediaController = null;
        }
        unbindService(serviceConnection);
    }
}