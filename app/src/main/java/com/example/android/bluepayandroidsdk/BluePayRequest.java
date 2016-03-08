package com.example.android.bluepayandroidsdk;

import android.os.StrictMode;

import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jslingerland on 2/10/2016.
 */
public class BluePayRequest {

    public static Map<String, String> postToBluePay(String accountID, String secretKey, String transactionMode,
                                                    String transactionType, Map<String, String> paymentInfo) {
        StringBuilder sb = new StringBuilder();
        Map<String,String> postData = new HashMap<>();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            URL postURL = new URL("https://secure.bluepay.com/interfaces/bp20post");
            HttpURLConnection postConnection = (HttpURLConnection) postURL.openConnection();
            postConnection.setDoOutput(true);
            postConnection.setRequestMethod("POST");
            postConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            postConnection.setRequestProperty("User-Agent", "BluePay Android SDK/1.0.0");

            if (paymentInfo.get("transType") != null)
                postData.put("TRANS_TYPE", paymentInfo.get("transType"));
            else
                postData.put("TRANS_TYPE", transactionType);
            String tps = null;
            postData.put("ACCOUNT_ID", accountID);
            postData.put("NAME1", paymentInfo.get("name1"));
            postData.put("NAME2", paymentInfo.get("name2"));
            postData.put("ADDR1", paymentInfo.get("addr1"));
            postData.put("ADDR2", paymentInfo.get("addr2"));
            postData.put("CITY", paymentInfo.get("city"));
            postData.put("STATE", paymentInfo.get("state"));
            postData.put("ZIP", paymentInfo.get("zip"));
            postData.put("AMOUNT", paymentInfo.get("amount"));
            postData.put("MODE", transactionMode);
            if (paymentInfo.get("track1") == null) {
                postData.put("PAYMENT_ACCOUNT", paymentInfo.get("cardNumber"));
                postData.put("CARD_CVV2", paymentInfo.get("cvv2"));
                postData.put("CARD_EXPIRE", paymentInfo.get("expMonth") + paymentInfo.get("expYear").substring(2));
                tps = secretKey + accountID + postData.get("TRANS_TYPE") +
                        paymentInfo.get("amount") + paymentInfo.get("name1") + paymentInfo.get("cardNumber");
            } else {
                postData.put("KSN", paymentInfo.get("ksn"));
                postData.put("TRACK1_ENC", paymentInfo.get("track1"));
                postData.put("TRACK1_EDL", paymentInfo.get("track1Length"));
                tps = secretKey + accountID + postData.get("TRANS_TYPE") +
                        paymentInfo.get("amount") + paymentInfo.get("name1");
            }
            postData.put("TAMPER_PROOF_SEAL", BluePayHelper.md5(tps));

            // Send post data to BluePay
            String encodedStr = getEncodedData(postData);
            OutputStreamWriter writer = new OutputStreamWriter(postConnection.getOutputStream());
            writer.write(encodedStr);
            writer.flush();
            Map<String, String> responseParams = BluePayResponse.getResponse(postConnection);
            return responseParams;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getEncodedData(Map<String,String> data) {
        StringBuilder sb = new StringBuilder();
        for(String key : data.keySet()) {
            String value = null;
            try {
                if (data.get(key) != null)
                    value = URLEncoder.encode(data.get(key),"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            if(sb.length() > 0)
                sb.append("&");

            sb.append(key + "=" + value);
        }
        return sb.toString();
    }
}
