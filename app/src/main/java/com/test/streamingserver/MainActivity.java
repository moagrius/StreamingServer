package com.test.streamingserver;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getCanonicalName();

  private EncryptedMediaStreamingServer mEncryptedMediaStreamingServer;

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

    mEncryptedMediaStreamingServer = new EncryptedMediaStreamingServer(this);
    mEncryptedMediaStreamingServer.start();

    findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        startVideo();
      }
    });

  }

  private void startVideo() {
    final VideoView videoView = (VideoView) findViewById(R.id.videoview);
    MediaController mediaController = new MediaController(this);
    mediaController.setAnchorView(videoView);
    videoView.setMediaController(mediaController);
    videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mediaPlayer) {
        videoView.start();
      }
    });
    try {
      videoView.setVideoPath(mEncryptedMediaStreamingServer.getUrl("bob"));
    } catch(Exception e) {
      //
    }
  }
}
