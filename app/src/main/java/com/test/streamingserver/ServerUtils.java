package com.test.streamingserver;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by michaeldunn on 2/17/17.
 */

public class ServerUtils {
  public static String getIpAddress() {
    try {
      Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();
      while (enumNetworkInterfaces.hasMoreElements()) {
        NetworkInterface networkInterface = enumNetworkInterfaces.nextElement();
        Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses();
        while (enumInetAddress.hasMoreElements()) {
          InetAddress inetAddress = enumInetAddress.nextElement();
          if (inetAddress.isSiteLocalAddress()) {
            return inetAddress.getHostAddress();
          }
        }
      }
    } catch (SocketException e) {
      e.printStackTrace();
    }
    return null;
  }
}
