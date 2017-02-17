package com.test.streamingserver;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Created by michaeldunn on 2/17/17.
 */

public class StreamProxy implements Runnable {

  private static final String TAG = StreamProxy.class.getCanonicalName();

  public static final int SERVER_PORT=8888;

  private Thread thread;
  private boolean isRunning;
  private ServerSocket socket;
  private int port;
  private Context context;

  public StreamProxy(Context context) {

    this.context = context;

    // Create listening socket
    try {
      socket = new ServerSocket(SERVER_PORT, 0, InetAddress.getByName(ServerUtils.getIpAddress()));
      //socket.setSoTimeout(5000);
      port = socket.getLocalPort();
    } catch (UnknownHostException e) { // impossible
    } catch (IOException e) {
      Log.e(TAG, "IOException initializing server", e);
    }

  }

  public void start() {
    thread = new Thread(this);
    thread.start();
  }

  public void stop() {
    isRunning = false;
    thread.interrupt();
    try {
      thread.join(5000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void run() {
    Looper.prepare();
    isRunning = true;
    while (isRunning) {
      try {
        Socket client = socket.accept();
        if (client == null) {
          continue;
        }
        Log.d(TAG, "client connected, about to start task: " + client.getInputStream().available());
        StreamToMediaPlayerTask task = new StreamToMediaPlayerTask(client);
        Log.d(TAG, "about to process request");
        if (task.processRequest()) {
          Log.d(TAG, "about to execute");
          task.execute();
        }

      } catch (SocketTimeoutException e) {
        // Do nothing
      } catch (IOException e) {
        Log.e(TAG, "Error connecting to client", e);
      }
    }
    Log.d(TAG, "Proxy interrupted. Shutting down.");
  }




  private class StreamToMediaPlayerTask extends AsyncTask<String, Void, Integer> {

    String localPath;
    Socket client;
    int cbSkip;

    public StreamToMediaPlayerTask(Socket client) {
      this.client = client;
    }

    private String stringFromStream(InputStream stream) {
      Log.d(TAG, "stringFromStream");
      try {
        InputStreamReader inputStreamReader = new InputStreamReader(stream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        StringBuilder builder = new StringBuilder();
        while (true) {
          int character = bufferedReader.read();
          Log.d(TAG, "character: " + character);
          if(character < 1) {
            Log.d(TAG, "breaking");
            break;
          }
          builder.append((char) character);
        }
        bufferedReader.close();
        Log.d(TAG, "after stringFromStream loop");
        return builder.toString();
      } catch (Exception e) {
        Log.e(TAG, "exception in string to stream");
        e.printStackTrace();
      }
      return null;
    }

    public boolean processRequest() {

      Log.d(TAG, "processRequest");

      // Read HTTP headers
      String headers = "";
      try {
        Log.d(TAG, "before string from stream: " + client.getInputStream().available());
        headers = stringFromStream(client.getInputStream());
        Log.d(TAG, "after string from stream");
        Log.d(TAG, headers);
      } catch (Exception e) {
        Log.e(TAG, "Error reading HTTP request header from stream:", e);
        return false;
      }

      // Get the important bits from the headers
      String[] headerLines = headers.split("\n");
      String urlLine = headerLines[0];
      if (!urlLine.startsWith("GET ")) {
        Log.e(TAG, "Only GET is supported");
        return false;
      }
      urlLine = urlLine.substring(4);
      int charPos = urlLine.indexOf(' ');
      if (charPos != -1) {
        urlLine = urlLine.substring(1, charPos);
      }
      localPath = urlLine;

      // See if there's a "Range:" header
      for (int i=0 ; i<headerLines.length ; i++) {
        String headerLine = headerLines[i];
        if (headerLine.startsWith("Range: bytes=")) {
          headerLine = headerLine.substring(13);
          charPos = headerLine.indexOf('-');
          if (charPos>0) {
            headerLine = headerLine.substring(0,charPos);
          }
          cbSkip = Integer.parseInt(headerLine);
        }
      }
      return true;
    }

    @Override
    protected Integer doInBackground(String... params) {

      Log.d(TAG, "doInBackground");

      long fileSize = 0;
      try {
        fileSize = context.getAssets().open("moonlight_sonata.mp3").available();
      } catch (IOException e) {
        e.printStackTrace();
      }

      // Create HTTP header
      String headers = "HTTP/1.0 200 OK\r\n";
      headers += "Content-Type: video/mp4\r\n";
      headers += "Content-Length: " + fileSize  + "\r\n";
      headers += "Connection: close\r\n";
      headers += "\r\n";

      // Begin with HTTP header
      int fc = 0;
      long cbToSend = fileSize - cbSkip;
      OutputStream output = null;
      byte[] buff = new byte[64 * 1024];
      try {
        output = new BufferedOutputStream(client.getOutputStream(), 32*1024);
        output.write(headers.getBytes());

        // Loop as long as there's stuff to send
        while (isRunning && cbToSend>0 && !client.isClosed()) {

          // See if there's more to send
          File file = new File(localPath);
          fc++;
          int cbSentThisBatch = 0;
          if (file.exists()) {
            //FileInputStream input = new FileInputStream(file);
            InputStream input = context.getAssets().open("moonlight_sonata.mp3");
            input.skip(cbSkip);
            int cbToSendThisBatch = input.available();
            while (cbToSendThisBatch > 0) {
              int cbToRead = Math.min(cbToSendThisBatch, buff.length);
              int cbRead = input.read(buff, 0, cbToRead);
              if (cbRead == -1) {
                break;
              }
              cbToSendThisBatch -= cbRead;
              cbToSend -= cbRead;
              output.write(buff, 0, cbRead);
              output.flush();
              cbSkip += cbRead;
              cbSentThisBatch += cbRead;
            }
            input.close();
          }

          // If we did nothing this batch, block for a second
          if (cbSentThisBatch == 0) {
            Log.d(TAG, "Blocking until more data appears");
            Thread.sleep(1000);
          }
        }
      }
      catch (SocketException socketException) {
        Log.e(TAG, "SocketException() thrown, proxy client has probably closed. This can exit harmlessly");
      }
      catch (Exception e) {
        Log.e(TAG, "Exception thrown from streaming task:");
        Log.e(TAG, e.getClass().getName() + " : " + e.getLocalizedMessage());
        e.printStackTrace();
      }

      // Cleanup
      try {
        if (output != null) {
          output.close();
        }
        client.close();
      }
      catch (IOException e) {
        Log.e(TAG, "IOException while cleaning up streaming task:");
        Log.e(TAG, e.getClass().getName() + " : " + e.getLocalizedMessage());
        e.printStackTrace();
      }

      return 1;
    }

  }
}