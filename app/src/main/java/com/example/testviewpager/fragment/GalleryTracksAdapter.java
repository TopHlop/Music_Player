package com.example.testviewpager.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.example.testviewpager.R;
import com.example.testviewpager.model.TrackCollection;
import com.example.testviewpager.databinding.RecyclerViewTrackBinding;

class GalleryTracksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private int currentTrackId;
    private Context context;
    private MutableLiveData<Integer> selectTrackId = new MutableLiveData<>();

    public GalleryTracksAdapter(Context context) {
        super();
        this.context = context;
        currentTrackId = TrackCollection.getFirstTrack().getTrackId();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        RecyclerViewTrackBinding binding = RecyclerViewTrackBinding.inflate(inflater, parent, false);
        return new ViewHolderMain(binding);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final ViewHolderMain holderMain = (ViewHolderMain) holder;
        TrackCollection.Track track = TrackCollection.getTrack(position);

        holderMain.binding.textTitleGallery.setText(track.getTitle());
        holderMain.binding.textArtistGallery.setText(track.getArtist());
        holderMain.binding.albumArtGallery.setImageBitmap(track.getBitmap(holder.itemView.getResources()));
        holderMain.binding.mainLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTrackId.postValue(track.getTrackId());
                currentTrackId = track.getTrackId();
                TrackCollection.setCurrentTrackPosition(position);
            }
        });
        if (currentTrackId == track.getTrackId()) {
            holderMain.binding.mainLayout.setBackground(context.getResources().
                    getDrawable(R.drawable.track_select_ripple_effect));
            holderMain.binding.textTitleGallery.setSelected(true);
        } else {
            holderMain.binding.mainLayout.setBackground(context.getResources().
                    getDrawable(R.drawable.track_ripple_effect));
            holderMain.binding.textTitleGallery.setSelected(false);
        }
    }

    public LiveData<Integer> getSelectTrackId() {
        return selectTrackId;
    }

    @Override
    public int getItemCount() {
        return TrackCollection.getSize();
    }

    public void setCurrentTrackId(int trackId) {
        currentTrackId = trackId;
        notifyDataSetChanged();
    }

    public class ViewHolderMain extends RecyclerView.ViewHolder {
        RecyclerViewTrackBinding binding;

        ViewHolderMain(RecyclerViewTrackBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
