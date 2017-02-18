package com.test.streamingserver;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.VideoView;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getCanonicalName();

  private EncryptedMediaStreamingServer mEncryptedMediaStreamingServer;

  private SimpleExoPlayerView mSimpleExoPlayerView;
  private VideoView mVideoView;
  private Button mMediaPlayerButton;
  private Button mExoPlayerButton;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
/*
    StreamProxy streamProxy = new StreamProxy(this);
    streamProxy.start();

    try {
      MediaPlayer mediaPlayer = new MediaPlayer();
      mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      mediaPlayer.setDataSource("http://" + ServerUtils.getIpAddress() + ":" + StreamProxy.SERVER_PORT + "/videofilename");
      mediaPlayer.prepare();
      mediaPlayer.start();
    } catch(Exception e) {
      Log.d(TAG, "problem");
    }
    */

    mVideoView = (VideoView) findViewById(R.id.videoview);
    mSimpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.simpleexoplayerview);
    mMediaPlayerButton = (Button) findViewById(R.id.button_mediaplayer);
    mExoPlayerButton = (Button) findViewById(R.id.button_exoplayer);

    mEncryptedMediaStreamingServer = new EncryptedMediaStreamingServer(this);
    mEncryptedMediaStreamingServer.start();

    mMediaPlayerButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        startMediaPlayer();
      }
    });

    mExoPlayerButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        startExoPlayer();
      }
    });

  }

  private void startMediaPlayer() {
    MediaController mediaController = new MediaController(this);
    mediaController.setAnchorView(mVideoView);
    mVideoView.setMediaController(mediaController);
    mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mediaPlayer) {
        mVideoView.start();
      }
    });
    try {
      mVideoView.setVideoPath(mEncryptedMediaStreamingServer.getUrl("bob"));
    } catch (Exception e) {
      //
    }
  }

  private void startExoPlayer() {
    DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
    TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveVideoTrackSelection.Factory(bandwidthMeter);
    TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
    LoadControl loadControl = new DefaultLoadControl();
    SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
    mSimpleExoPlayerView.setPlayer(player);
    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "bob"), bandwidthMeter);
    ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
    try {
      Uri mp4VideoUri = Uri.parse(mEncryptedMediaStreamingServer.getUrl("bob"));
      MediaSource videoSource = new ExtractorMediaSource(mp4VideoUri, dataSourceFactory, extractorsFactory, null, null);
      player.prepare(videoSource);
      player.setPlayWhenReady(true);
    } catch (Exception e) {
      Log.e(TAG, "error setting up exoplayer: " + e.getMessage());
    }
  }
}
