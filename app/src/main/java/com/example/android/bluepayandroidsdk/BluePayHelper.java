package com.example.android.bluepayandroidsdk;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Created by jslingerland on 2/10/2016.
 */
public class BluePayHelper {
    private static String accountID = "100013391447"; // Gateway Account ID
    private static String secretKey = "5YRFNRBCZN/6Y4OPZNWPYDRNAVX7BMMD"; // Gateway Secret Key
    private static String transactionMode = "TEST"; // Can be either TEST or LIVE
    private static String transactionType = "SALE"; // Can be either SALE or AUTH

    public static Map<String, String> doPost(Map<String, String> cardInfo) {
        return BluePayRequest.postToBluePay(accountID, secretKey, transactionMode, transactionType, cardInfo);
    }

    public static final String md5(String tps) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest.getInstance(MD5);
            digest.update(tps.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
