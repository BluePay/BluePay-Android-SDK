package com.example.android.bluepayandroidsdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jslingerland on 2/10/2016.
 */
public class BluePayResponse {
    public static Map<String, String> getResponse(HttpURLConnection postConnection) {
        BufferedReader reader;
        StringBuilder sb = new StringBuilder();
        String response = "";
        try {
            int status = postConnection.getResponseCode();

            // Need to handle 400 status code for erred transactions
            if (status == 400) {
                InputStream errorstream = postConnection.getErrorStream();
                reader =new BufferedReader(new InputStreamReader(errorstream));
                String line;
                while ((line=reader.readLine()) != null) {
                    response+=line;
                }
                response = URLDecoder.decode(response, "UTF-8");

                // Transaction was either approved or declined. Grab the response
            } else {

                // Reads data line by line
                reader = new BufferedReader(new InputStreamReader(postConnection.getInputStream()));

                while((response = reader.readLine()) != null) { //Read till there is something available
                    sb.append(response + "\n");     //Reading and saving line by line - not all at once
                }
                response = URLDecoder.decode(sb.toString(), "UTF-8");

                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, String> responseParams = new HashMap<String, String>();
        String[] arr = response.split("&");
        for(String s : arr){
            String[] a = s.split("=");
            if (a.length > 1)
                responseParams.put(a[0],a[1]);
            else
                responseParams.put(a[0],"");
        }
        return responseParams;
    }
}
