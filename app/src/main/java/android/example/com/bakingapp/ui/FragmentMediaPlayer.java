package android.example.com.bakingapp.ui;

import android.example.com.bakingapp.R;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class FragmentMediaPlayer extends Fragment implements ExoPlayer.EventListener{


    private PlayerView mPlayerView;
    private String mVideoUrl;
    private SimpleExoPlayer mPlayer;
    private static MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mPlaybackStateBuilder;

    private boolean mPlayWhenReady = true;
    private int mCurrentWindow = 0;
    private long mPlaybackPosition = 0;

    private static final String PLAY_WHEN_READY = "play_when_ready";
    private static final String CURRENT_WINDOW = "current_window";
    private static final String PLAYBACK_POSITION = "playback_position";
    private static final String VIDEO_URL = "video_url";

    private static final String TAG = FragmentMediaPlayer.class.getSimpleName();

    public FragmentMediaPlayer(){

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(savedInstanceState != null){
            mPlayWhenReady = savedInstanceState.getBoolean(PLAY_WHEN_READY);
            mCurrentWindow = savedInstanceState.getInt(CURRENT_WINDOW);
            mPlaybackPosition = savedInstanceState.getLong(PLAYBACK_POSITION);
            mVideoUrl = savedInstanceState.getString(VIDEO_URL);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_media_player, container, false);

        mPlayerView = rootView.findViewById(R.id.fragment_video_view);

        return rootView;
    }

    public void setVideoUrl(String urlString){
        mVideoUrl = urlString;
    }

    private void initializePlayer(){



        if(mPlayer == null && mVideoUrl != null && !mVideoUrl.isEmpty()){
            Uri uri = Uri.parse(mVideoUrl);
            String userAgent = Util.getUserAgent(getContext(), "BakingApp");
            MediaSource mediaSource = new ExtractorMediaSource(uri,
                    new DefaultDataSourceFactory(getContext(), userAgent), new DefaultExtractorsFactory(),
                    null, null);

            TrackSelector trackSelector = new DefaultTrackSelector(getContext());
            LoadControl loadControl = new DefaultLoadControl();

            mPlayer = ExoPlayerFactory.newSimpleInstance(getContext(), trackSelector, loadControl);
            mPlayerView.setPlayer(mPlayer);
            mPlayer.prepare(mediaSource);
            mPlayer.setPlayWhenReady(mPlayWhenReady);
            mPlayer.seekTo(mCurrentWindow, mPlaybackPosition);
            mPlayer.addListener(this);





        }
    }


    private void releasePlayer(){
        if(mPlayer != null){
            mPlayer.removeListener(this);
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }


    private void initializeMediaSession(){

        mMediaSession = new MediaSessionCompat(getContext(), TAG);

        mMediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mMediaSession.setMediaButtonReceiver(null);

        mPlaybackStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_FAST_FORWARD |
                                PlaybackStateCompat.ACTION_REWIND |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);

        mMediaSession.setPlaybackState(mPlaybackStateBuilder.build());

        mMediaSession.setCallback(new MySessionCallback());

        mMediaSession.setActive(true);
    }


    @Override
    public void onStart() {
        super.onStart();
        initializeMediaSession();
        initializePlayer();
    }



    @Override
    public void onResume() {
        super.onResume();
        if(mPlayer != null){
            mPlayer.setPlayWhenReady(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mPlayer != null){
            mPlayWhenReady = mPlayer.getPlayWhenReady();
            mPlaybackPosition = mPlayer.getCurrentPosition();
            mCurrentWindow = mPlayer.getCurrentWindowIndex();
            mPlayer.setPlayWhenReady(false);
            mPlayer.getPlaybackState();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        releasePlayer();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PLAY_WHEN_READY, mPlayWhenReady);
        outState.putLong(PLAYBACK_POSITION, mPlaybackPosition);
        outState.putInt(CURRENT_WINDOW, mCurrentWindow);
        outState.putString(VIDEO_URL, mVideoUrl);

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if((playbackState == ExoPlayer.STATE_READY) && playWhenReady){
            mPlaybackStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING,
                    mPlayer.getCurrentPosition(), 1f);
        }else if((playbackState == ExoPlayer.STATE_READY)){
            mPlaybackStateBuilder.setState(PlaybackStateCompat.STATE_PAUSED,
                    mPlayer.getCurrentPosition(), 1f);
        }
        mMediaSession.setPlaybackState(mPlaybackStateBuilder.build());
    }


    private class MySessionCallback extends MediaSessionCompat.Callback{
        @Override
        public void onPlay() {
            mPlayer.setPlayWhenReady(true);
        }

        @Override
        public void onPause() {
            mPlayer.setPlayWhenReady(false);
        }

        @Override
        public void onFastForward() {
            mPlaybackPosition = mPlayer.getCurrentPosition();
            mPlayer.seekTo(mPlaybackPosition + 5000);
        }

        @Override
        public void onRewind() {
            mPlaybackPosition = mPlayer.getCurrentPosition();
            mPlayer.seekTo(mPlaybackPosition - 5000);
        }
    }
}



