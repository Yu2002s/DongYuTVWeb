package xyz.jdynb.tv.utils;


import android.os.Build;

import androidx.annotation.Nullable;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AESUtils {

    @Nullable
    public static String encrypt(String data, String key, String iv) {
        try {
            byte[] raw = key.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivParameterSpec);
            byte[] encrypted = cipher.doFinal(data.getBytes());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return Base64.getEncoder().encodeToString(encrypted);
            } else {
                return android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT);
            }
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static String decrypt(String data, String key, String iv) {
        try {
            byte[] raw = key.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            IvParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivParameterSpec);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return new String(cipher.doFinal(Base64.getDecoder().decode(data)), StandardCharsets.UTF_8);
            } else {
                return new String(cipher.doFinal(android.util.Base64.decode(data, android.util.Base64.DEFAULT)), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
