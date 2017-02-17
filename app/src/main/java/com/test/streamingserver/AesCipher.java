package com.test.streamingserver;

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by michaeldunn on 2/15/17.
 */

public class AesCipher {

  private static final String DUMMY_SALT = "have a very merry christmas, it's the best time of the year";

  private static final String UTF8_CHARSET = "UTF-8";
  private static final String SHA1_ALGORITHM = "SHA-1";

  private static final String AES_ALGORITHM = "AES";
  private static final String AES_TRANSFORMATION = "AES/CTR/NoPadding";

  // TODO: this
  private static final byte[] IV = new byte[]{1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0};

  public static byte[] getSecretKey(String string) throws UnsupportedEncodingException, NoSuchAlgorithmException {
    byte[] key = string.getBytes(UTF8_CHARSET);
    MessageDigest sha = MessageDigest.getInstance(SHA1_ALGORITHM);
    key = sha.digest(key);
    return Arrays.copyOf(key, 16);
  }

  public static byte[] getSecretKey() throws UnsupportedEncodingException, NoSuchAlgorithmException {
    return getSecretKey(DUMMY_SALT);
  }

  public static SecretKeySpec getSecretKeySpec(String string) throws UnsupportedEncodingException, NoSuchAlgorithmException {
    byte[] key = getSecretKey(string);
    return new SecretKeySpec(key, AES_ALGORITHM);
  }

  public static SecretKeySpec getSecretKeySpec() throws UnsupportedEncodingException, NoSuchAlgorithmException {
    return getSecretKeySpec(DUMMY_SALT);
  }

  public static Cipher getCipher() {
    try {
      return Cipher.getInstance(AES_TRANSFORMATION);
    } catch(NoSuchAlgorithmException | NoSuchPaddingException e) {
      Log.e("ENC", "getCipher: " + e.getMessage());
      return null;
    }
  }

  public static Cipher getInitializedCipher(int mode) {
    Cipher cipher = getCipher();
    if (cipher == null) {
      return null;
    }
    try {
      Key key = getSecretKeySpec(DUMMY_SALT);
      cipher.init(mode, key, new IvParameterSpec(IV));
      return cipher;
    } catch(Exception e) {
      Log.e("ENC", "getInitializedCipher: " + e.getMessage());
    }
    return null;
  }

  public static Cipher getDecryptionCipher() {
    return getInitializedCipher(Cipher.DECRYPT_MODE);
  }

  public static Cipher getEncryptionCipher() {
    return getInitializedCipher(Cipher.ENCRYPT_MODE);
  }

}
