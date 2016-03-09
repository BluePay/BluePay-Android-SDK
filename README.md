# BluePay-Android-SDK
This SDK is used for android devices in conjuction with the BluePay gateway. Includes swipe support for the IDTech UniMag II and Shuttle.
BluePay-Android-SDK can be used to process authorizations, sales, generate tokens as $0 authorizations, and process card-present transactions.

If you plan to accept credit card transactions, it's recommended per PCI compliance to use BluePay-Android-SDK to handle this so that sensitive card data never passes through your server.

Since this SDK works with the BluePay gateway, you will need a gateway account to utilize the SDK. [Click here](https://www.bluepay.com/contact-us/request-sandbox-account/) if you'd like to request a sandbox account

# Usage
Before you can do any processing, you must enter your Account ID and Secret Key in the BluePayHelper.java class. This [helpful video](https://www.bluepay.com/video/locating-your-bluepay-secret-key-and-account-id/?width=640&height=380) shows you how and where to grab these two values.
The other two options that you will need to set in the BluePayHelper class is the transaction mode (can be TEST or LIVE) and the transaction type (can be SALE or AUTH).
```
public class BluePayHelper {

    private static String accountID = "Merchant's Account ID here"; // Gateway Account ID
    private static String secretKey = "Merchant's Secret Key here"; // Gateway Secret Key
    private static String transactionMode = "TEST"; // Can be either TEST or LIVE
    private static String transactionType = "SALE"; // Can be either SALE or AUTH
    
```
    
The SDK comes with 3 Fragments sitting underneath a FragmentActivity. These Fragments are:

1) Run Payment
 - This shows how to run either an AUTH or a SALE transaction using the user-inputted values for the name, address, credit card #, expiration date, CVV2, and amount. If you have the transaction type set as 'AUTH' in the BluePayHelper class, an authorization will be run after the 'Pay Now' button is selected; a SALE transaction will be run otherwise.

2) Store Token
- This shows how to generate a BluePay token so that subsequent transactions against the same card can be issued in the future without having to ask your customer for their payment information again. From the BluePay side of things, this will run a $0.00 AUTH using the credit card, expiration date, and CVV2 entered on this Fragment.

3) Swipe Card
- This shows how to use either a IDTech UniMag II or Shuttle mobile card swiper to process a card-present transaction. Once you have connected your IDTech device, enter in the amount and (optionally) the address information, then hit the 'Swipe Card' button. This will prompt you to then swipe the card and you should receive a real-time response from the BluePay gateway after the encrypted card information has been sent. If a bad swipe occurs, you will get an error.

The 'Run Payment' and 'Store Token' Fragments check that the credit card # entered passes the [Luhn algorithm](https://en.wikipedia.org/wiki/Luhn_algorithm)  before it is sent to the BluePay gateway. The amount field is checked as well to ensure that the amount is an acceptable value. The CVV2 is also checked for either a 3 or 4 digit number. If you want extra validity for the expiration date, you can implement your own method to make sure that the expiration date entered by the user is not a value that has already expired. 

Lastly, make sure that your project grants sufficient permissions to the user's android device. To do so, edit your AndroidManifest.xml file to have the following permissions.

```
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.INTERNET"/>
  ````

# Building and running the project
- Clone the git repository to your local machine
- Make sure your Android Studio supports at least SDK version 23
- Import the project into Android Studio: choose Import Project... from the "Welcome to Android Studio" screen. Select the build.gradle file at the top of the stripe-android repository.
- Now, go ahead and build+run the app.
