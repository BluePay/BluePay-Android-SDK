/*
 * Copyright 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluepayandroidsdk;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.NumberPicker;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import IDTech.MSR.XMLManager.StructConfigParameters;
import IDTech.MSR.uniMag.StateList;
import IDTech.MSR.uniMag.UniMagTools.uniMagSDKTools;
import IDTech.MSR.uniMag.uniMagReader;
import IDTech.MSR.uniMag.uniMagReaderMsg;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener, uniMagReaderMsg {

    AppSectionsPagerAdapter mAppSectionsPagerAdapter;

    ViewPager mViewPager;
    public static BluePayHelper bluepay;

    private uniMagReader myUniMagReader = null;
    private uniMagSDKTools firmwareUpdateTool = null;

    private TextView connectStatusTextView; // displays status of UniMag Reader: Connected / Disconnected
    //private TextView headerTextView; // short description of data displayed below
    //private TextView textAreaTop;
    //private EditText textAreaBottom;
    private Button btnCommand;
    private Button btnSwipeCard;
    private boolean isReaderConnected = false;
    private boolean isExitButtonPressed = false;
    private boolean isWaitingForCommandResult=false;
    private boolean isSaveLogOptionChecked = false;
    //	private boolean isConnectWithCommand = true;
    private int readerType = -1; // 0: UniMag, 1: UniMag II

    //update the powerup status
    private int percent = 0;
    private long beginTime = 0;
    private long beginTimeOfAutoConfig = 0;
    private byte[] challengeResponse = null;

    private String popupDialogMsg = null;
    private boolean enableSwipeCard =false;
    private boolean autoconfig_running = false;

    private String strMsrData = null;
    private byte[] msrData = null;
    private String statusText = null;
    private int challengeResult = 0;

    static private final int REQUEST_GET_XML_FILE = 1;
    static private final int REQUEST_GET_BIN_FILE = 2;
    static private final int REQUEST_GET_ENCRYPTED_BIN_FILE = 3;

    //property for the menu item.
    static final private int START_SWIPE_CARD 	= Menu.FIRST;
    static final private int SETTINGS_ITEM 		= Menu.FIRST + 2;
    static final private int SUB_SAVE_LOG_ITEM 	= Menu.FIRST + 3;
    static final private int SUB_USE_AUTOCONFIG_PROFILE = Menu.FIRST + 4;
    static final private int SUB_SELECT_READER = Menu.FIRST + 5;
    static final private int SUB_LOAD_XML 		= Menu.FIRST + 6;
    static final private int SUB_LOAD_BIN 		= Menu.FIRST + 7;
    static final private int SUB_START_AUTOCONFIG= Menu.FIRST + 8;
    static final private int SUB_STOP_AUTOCONFIG = Menu.FIRST + 10;
    static final private int SUB_ATTACHED_TYPE 	= Menu.FIRST + 103;
    static final private int SUB_SUPPORT_STATUS	= Menu.FIRST + 104;
    static final private int DELETE_LOG_ITEM 	= Menu.FIRST + 11;
    static final private int ABOUT_ITEM 		= Menu.FIRST + 12;
    static final private int EXIT_IDT_APP 		= Menu.FIRST + 13;
    static final private int SUB_LOAD_ENCRYPTED_BIN = Menu.FIRST + 14;

    private MenuItem itemStartSC = null;
    private MenuItem itemSubSaveLog = null;
    private MenuItem itemSubUseAutoConfigProfile = null;
    private MenuItem itemSubSelectReader = null;
    private MenuItem itemSubLoadXML = null;
    private MenuItem itemSubStartAutoConfig = null;
    private MenuItem itemSubStopAutoConfig = null;
    private MenuItem itemDelLogs = null;
    private MenuItem itemAbout = null;
    private MenuItem itemExitApp = null;

    private SubMenu sub = null;

    private UniMagTopDialog dlgTopShow = null ;
    private UniMagTopDialog dlgError = null;
    private UniMagTopDialog dlgSwipeTopShow = null ;
    private UniMagTopDialogYESNO dlgYESNOTopShow = null ;

    private StructConfigParameters profile = null;
    private Handler handler = new Handler();

    private String track1 = null;
    private String encTrack1 = null;
    private String KSN = null;
    private String cardHolderFirst = null;
    private String cardHolderLast = null;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAppSectionsPagerAdapter = new AppSectionsPagerAdapter(getSupportFragmentManager());
        final ActionBar actionBar = getActionBar();

        actionBar.setHomeButtonEnabled(false);

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mAppSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                // When swiping between different app sections, select the corresponding tab.
                // We can also use ActionBar.Tab#select() to do this if we have a reference to the
                // Tab.
                actionBar.setSelectedNavigationItem(position);
            }
        });

        for (int i = 0; i < mAppSectionsPagerAdapter.getCount(); i++) {
            if (i == 0) {
                actionBar.addTab(
                        actionBar.newTab()
                                .setText("Run Payment")
                                .setTabListener(this));
            } else if (i == 1) {
                actionBar.addTab(
                        actionBar.newTab()
                                .setText("Store Token")
                                .setTabListener(this));
            } else if (i == 2) {
                actionBar.addTab(
                        actionBar.newTab()
                                .setText("Swipe Card")
                                .setTabListener(this));
            } else {
                actionBar.addTab(
                        actionBar.newTab()
                                .setText(mAppSectionsPagerAdapter.getPageTitle(i))
                                .setTabListener(this));
            }
        }

    }

    public void openReaderSelectDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a reader:");
        builder.setCancelable(false);
        builder.setItems(R.array.reader_type, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                switch (which) {
                    case 0:
                        readerType = 0;
                        initializeReader(uniMagReader.ReaderType.UM_OR_PRO);
                        Toast.makeText(getApplicationContext(), "UniMag / UniMag Pro selected", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        readerType = 1;
                        initializeReader(uniMagReader.ReaderType.SHUTTLE);
                        Toast.makeText(getApplicationContext(), "UniMag II / Shuttle selected", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        readerType = 1;
                        initializeReader(uniMagReader.ReaderType.SHUTTLE);
                        Toast.makeText(getApplicationContext(), "UniMag II / Shuttle selected", Toast.LENGTH_SHORT).show();
                        break;
                }
                showAboutInfo();
            }
        });
        builder.create().show();
    }

    @Override
    public void onPause() {
        if(myUniMagReader!=null)
        {
            //stop swipe card when the application goes to background
            myUniMagReader.stopSwipeCard();
        }
        hideTopDialog();
        hideSwipeTopDialog();
        super.onPause();
    }
    @Override
    public void onResume() {
        if(myUniMagReader!=null)
        {
            if(isSaveLogOptionChecked==true)
                myUniMagReader.setSaveLogEnable(true);
            else
                myUniMagReader.setSaveLogEnable(false);
        }
        if(itemStartSC!=null)
            itemStartSC.setEnabled(true);
        isWaitingForCommandResult=false;
        super.onResume();
    }
    @Override
    public void onDestroy() {
        if (myUniMagReader != null)
            myUniMagReader.release();
        super.onDestroy();
        if (isExitButtonPressed)
        {
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    public synchronized void onActivityResult(final int requestCode, int resultCode, final Intent data) {

        if (resultCode == Activity.RESULT_OK) {

            String strTmpFileName = data.getStringExtra(FileDialog.RESULT_PATH);;
            if (requestCode == REQUEST_GET_XML_FILE) {

                if(!isFileExist(strTmpFileName))
                {
                    //headerTextView.setText("Warning");
                    //textAreaTop.setText("Please copy the XML file 'IDT_uniMagCfg.xml' into root path of SD card.");
                    //textAreaBottom.setText("");
                    return  ;
                }
                if (!strTmpFileName.endsWith(".xml")){
                    //headerTextView.setText("Warning");
                    //textAreaTop.setText("Please select a file with .xml file extension.");
                    //textAreaBottom.setText("");
                    return  ;
                }

                /////////////////////////////////////////////////////////////////////////////////
                // loadingConfigurationXMLFile() method may connect to server to download xml file.
                // Network operation is prohibited in the UI Thread if target API is 11 or above.
                // If target API is 11 or above, please use AsyncTask to avoid errors.
                myUniMagReader.setXMLFileNameWithPath(strTmpFileName);
                if (myUniMagReader.loadingConfigurationXMLFile(false)) {
                    //headerTextView.setText("Command Info");
                    //textAreaTop.setText("Reload XML file succeeded.");
                    //textAreaBottom.setText("");
                }
                else {
                    //headerTextView.setText("Warning");
                    //textAreaTop.setText("Please select a correct file and try again.");
                    //textAreaBottom.setText("");
                }
            }
            else if (requestCode == REQUEST_GET_BIN_FILE)
            {
                if(!isFileExist(strTmpFileName))
                {
                    //headerTextView.setText("Warning");
                    //textAreaTop.setText("Please copy the BIN file into the SD card root path.");
                    //textAreaBottom.setText("");
                    return  ;
                }
                //set BIN file
                if(true==firmwareUpdateTool.setFirmwareBINFile(strTmpFileName))
                {
                    //headerTextView.setText("Command Info");
                    //textAreaTop.setText("Set the BIN file succeeded.");
                    //textAreaBottom.setText("");
                }
                else
                {
                    //headerTextView.setText("Command Info");
                    //textAreaTop.setText("Failed to set the BIN file, please check the file format.");
                    //textAreaBottom.setText("");
                }
            }
            else if(requestCode == REQUEST_GET_ENCRYPTED_BIN_FILE)
            {

                if(!isFileExist(strTmpFileName))
                {
                    //headerTextView.setText("Warning");
                    //textAreaTop.setText("Please copy the BIN file into the SD card root path.");
                    //textAreaBottom.setText("");
                    return  ;
                }
                //set BIN file
                if(true==firmwareUpdateTool.setFirmwareEncryptedBINFile(strTmpFileName))
                {
                    //headerTextView.setText("Command Info");
                    //textAreaTop.setText("Set the Encrypted BIN file succeeded.");
                    //textAreaBottom.setText("");
                }
                else
                {
                    //headerTextView.setText("Command Info");
                    //textAreaTop.setText("Failed to set the Encrypted BIN file, please check the file format.");
                    //textAreaBottom.setText("");
                }
            }
        }
    }

    public void initializeUI()
    {
        btnSwipeCard = (Button)findViewById(R.id.btn_swipeCard);
        //textAreaTop = (TextView)findViewById(R.id.text_area_top);
        //textAreaBottom = (EditText)findViewById(R.id.text_area_bottom);
        connectStatusTextView = (TextView)findViewById(R.id.status_text);
        //headerTextView = (TextView)findViewById(R.id.header_text);

        connectStatusTextView.setText("DISCONNECTED");
        connectStatusTextView.setTextColor(Color.parseColor("#FFF0250A"));
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN, WindowManager.LayoutParams. FLAG_FULLSCREEN);

        // Set Listener for "Swipe Card" Button
        btnSwipeCard.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    if (!LaunchSwipeSection.getAmount().matches("^(?!\\.?$)\\d{0,6}(\\.\\d{0,2})?$")) {
                        LaunchSwipeSection.amount.setError("Invalid Amount");
                        return;
                    }
                } catch (Exception e) {
                    LaunchSwipeSection.amount.setError("Invalid Amount");
                    return;
                }
                if (myUniMagReader != null) {
                    if (!isWaitingForCommandResult) {
                        //while (KSN == null) {
                        if (true == myUniMagReader.sendCommandGetNextKSN())
                            prepareToSendCommand(uniMagReaderMsg.cmdGetNextKSN);
                        //}
                        beginTime = getCurrentTime();

                        if (myUniMagReader.startSwipeCard()) {
                            //textAreaTop.setText("Please wait for reader to be ready");
                            //textAreaBottom.setText("");
                        }
                    }
                }
            }
        });
    }

    public void initializeReader(uniMagReader.ReaderType type)
    {
        if(myUniMagReader!=null){
            myUniMagReader.unregisterListen();
            myUniMagReader.release();
            myUniMagReader = null;
        }
        myUniMagReader = new uniMagReader(this,this,type);

        if (myUniMagReader == null)
            return;

        myUniMagReader.setVerboseLoggingEnable(true);
        myUniMagReader.registerListen();

        //load the XML configuratin file
        String fileNameWithPath = getConfigurationFileFromRaw();
        if(!isFileExist(fileNameWithPath)) {
            fileNameWithPath = null;
        }

            /////////////////////////////////////////////////////////////////////////////////
            // Network operation is prohibited in the UI Thread if target API is 11 or above.
            // If target API is 11 or above, please use AsyncTask to avoid errors.
            myUniMagReader.setXMLFileNameWithPath(fileNameWithPath);
            myUniMagReader.loadingConfigurationXMLFile(true);
            /////////////////////////////////////////////////////////////////////////////////

    }

    private String getConfigurationFileFromRaw( ){
        return getXMLFileFromRaw("idt_unimagcfg_default.xml");
    }

    // If 'idt_unimagcfg_default.xml' file is found in the 'raw' folder, it returns the file path.
    private String getXMLFileFromRaw(String fileName ){
        //the target filename in the application path
        String fileNameWithPath = null;
        fileNameWithPath = fileName;

        try {
            InputStream in = getResources().openRawResource(R.raw.idt_unimagcfg_default);
            int length = in.available();
            byte [] buffer = new byte[length];
            in.read(buffer);
            in.close();
            deleteFile(fileNameWithPath);
            FileOutputStream fout = openFileOutput(fileNameWithPath, MODE_PRIVATE);
            fout.write(buffer);
            fout.close();

            // to refer to the application path
            File fileDir = this.getFilesDir();
            fileNameWithPath = fileDir.getParent() + java.io.File.separator + fileDir.getName();
            fileNameWithPath += java.io.File.separator+"idt_unimagcfg_default.xml";

        } catch(Exception e){
            e.printStackTrace();
            fileNameWithPath = null;
        }
        return fileNameWithPath;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK|| KeyEvent.KEYCODE_HOME==keyCode|| KeyEvent.KEYCODE_SEARCH==keyCode)){
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK|| KeyEvent.KEYCODE_HOME==keyCode|| KeyEvent.KEYCODE_SEARCH==keyCode)){

            return false;
        }	return super.onKeyMultiple(keyCode, repeatCount, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK|| KeyEvent.KEYCODE_HOME==keyCode|| KeyEvent.KEYCODE_SEARCH==keyCode)){
            return false;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId())
        {
            // when the 'swipe card' menu item clicked
            case (START_SWIPE_CARD):
            {
                //textAreaTop.setText("");
                //textAreaBottom.setText("");
                //itemStartSC.setEnabled(false);

                if(myUniMagReader!=null)
                    myUniMagReader.startSwipeCard();
                break;
            }
            // when the 'exit' menu item clicked
            case (EXIT_IDT_APP):
            {
                isExitButtonPressed = true;
                if(myUniMagReader!=null)
                {
                    myUniMagReader.unregisterListen();
                    myUniMagReader.stopSwipeCard();
                    myUniMagReader.release();
                }
                finish();
                break;
            }
            case (SUB_SELECT_READER):
            {
                openReaderSelectDialog();
                break;
            }
            // displays attached reader type
            case SUB_ATTACHED_TYPE:
                uniMagReader.ReaderType art = myUniMagReader.getAttachedReaderType();

                Log.e("Attached reader", "Returned reader type: " + art);

                if (art == null)
                {
                    //textAreaTop.setText("Please connect a reader first.");
                    //textAreaBottom.setText("");
                }
                else if(art== uniMagReader.ReaderType.UNKNOWN)
                {
                    //textAreaTop.setText("To get Attached Reader type, waiting for response.");
                    //textAreaBottom.setText("");
                }
                else
                {
                    //textAreaTop.setText("Attached Reader:\n   "+getReaderName(art));
                    //textAreaBottom.setText("");
                }
                break;
            // displays support status of all ID Tech readers
            case SUB_SUPPORT_STATUS:
                //print a list of reader:supported status pairs
                //textAreaTop.setText("Reader support status from cfg:\n");
                //for (uniMagReader.ReaderType rt : uniMagReader.ReaderType.values()) {
                    //if (rt == uniMagReader.ReaderType.UM || rt == uniMagReader.ReaderType.UM_PRO || rt == uniMagReader.ReaderType.UM_II || rt == uniMagReader.ReaderType.SHUTTLE)
                        //textAreaTop.append(getReaderName(rt)+" : "+myUniMagReader.getSupportStatus(rt)+"\n");
                //}
                //textAreaBottom.setText("");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        itemStartSC = menu.add(0,START_SWIPE_CARD, Menu.NONE, "Swipe Card");
        itemStartSC.setEnabled(true);
        sub = menu.addSubMenu(0,SETTINGS_ITEM, Menu.NONE,"Settings");
        itemSubSaveLog = sub.add(0,SUB_SAVE_LOG_ITEM, Menu.NONE,"Save Log option");
        itemSubUseAutoConfigProfile = sub.add(1, SUB_USE_AUTOCONFIG_PROFILE, Menu.NONE, "Use AutoConfig profile");
        itemSubSelectReader = sub.add(1, SUB_SELECT_READER, Menu.NONE, "Change reader type");
        itemSubLoadXML = sub.add(1,SUB_LOAD_XML, Menu.NONE,"Reload XML");
        itemSubStartAutoConfig = sub.add(4,SUB_START_AUTOCONFIG, Menu.NONE,"Start AutoConfig");
        itemSubStopAutoConfig = sub.add(6,SUB_STOP_AUTOCONFIG, Menu.NONE,"Stop AutoConfig");
        sub.add(Menu.NONE,SUB_ATTACHED_TYPE, Menu.NONE,"Get attached type");
        sub.add(Menu.NONE,SUB_SUPPORT_STATUS, Menu.NONE,"Get support status");
        itemSubSaveLog.setCheckable(true);
        itemSubUseAutoConfigProfile.setCheckable(true);
        itemSubLoadXML.setEnabled(true);
        itemSubStartAutoConfig.setEnabled(true);
        itemSubStopAutoConfig.setEnabled(true);
        itemDelLogs = menu.add(0,DELETE_LOG_ITEM, Menu.NONE,"Delete Logs");
        itemDelLogs.setEnabled(true);
        itemAbout = menu.add(0,ABOUT_ITEM, Menu.NONE,"About");
        itemAbout.setEnabled(true);
        itemExitApp = menu.add(0,EXIT_IDT_APP, Menu.NONE,"Exit");
        itemExitApp.setEnabled(true);
        return super.onCreateOptionsMenu(menu);
    }

    // Returns reader name based on abbreviations
    private String getReaderName(uniMagReader.ReaderType rt){
        switch(rt){
            case UM:
                return "UniMag";
            case UM_PRO:
                return "UniMag Pro";
            case UM_II:
                return "UniMag II";
            case SHUTTLE:
                return "Shuttle";
            case UM_OR_PRO:
                return "UniMag or UniMag Pro";
        }
        return "Unknown";

    }

    public uniMagReader.ReaderType getAttachedReaderType(int uniMagUnit) {
        switch (uniMagUnit) {
            case StateList.uniMag2G3GPro:
                return uniMagReader.ReaderType.UM_OR_PRO;
            case StateList.uniMagII:
                return uniMagReader.ReaderType.UM_II;
            case StateList.uniMagShuttle:
                return uniMagReader.ReaderType.SHUTTLE;
            case StateList.uniMagUnkown:
            default:
                return uniMagReader.ReaderType.UNKNOWN;
        }
    }
    private void showAboutInfo()
    {
        String strManufacture = myUniMagReader.getInfoManufacture();
        String strModel = myUniMagReader.getInfoModel();
        String strDevice = android.os.Build.DEVICE;
        String strSDKVerInfo = myUniMagReader.getSDKVersionInfo();
        String strXMLVerInfo = myUniMagReader.getXMLVersionInfo();
        String selectedReader;
        if (readerType == 0)
            selectedReader = "UniMag/UniMag Pro";
        else if (readerType == 1)
            selectedReader = "UniMag II/Shuttle";
        else
            selectedReader = "Unknown";

        //headerTextView.setText("SDK Info");
        //textAreaBottom.setText("");
        String strOSVerInfo = android.os.Build.VERSION.RELEASE;
        //textAreaTop.setText("Phone: "+strManufacture+"\n"+"Model: "+strModel+"\nDevice: "+strDevice+"\nSDK Ver: "+strSDKVerInfo+"\nXML Ver: "+strXMLVerInfo+"\nOS Version: "+strOSVerInfo+"\nReader Type: "+selectedReader);

    }
    private Runnable doShowTimeoutMsg = new Runnable()
    {
        public void run()
        {
            if(itemStartSC!=null&&enableSwipeCard==true)
                itemStartSC.setEnabled(true);
            enableSwipeCard = false;
            showDialog(popupDialogMsg);
        }

    };
    // shows messages on the popup dialog
    private void showDialog(String strTitle)
    {
        try
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("UniMag");
            builder.setMessage(strTitle);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    };

    private Runnable doShowTopDlg = new Runnable()
    {
        public void run()
        {
            showTopDialog(popupDialogMsg);
        }
    };
    private Runnable doHideTopDlg = new Runnable()
    {
        public void run()
        {
            hideTopDialog( );
        }

    };
    private Runnable doShowErrorDlg = new Runnable()
    {
        public void run()
        {
            showErrorDialog("There was an error when swiping the card.");
        }
    };
    private Runnable doHideErrorDlg = new Runnable()
    {
        public void run()
        {
            hideErrorDialog();
        }

    };
    private Runnable doShowSwipeTopDlg = new Runnable()
    {
        public void run()
        {
            showSwipeTopDialog( );
        }
    };
    private Runnable doShowYESNOTopDlg = new Runnable()
    {
        public void run()
        {
            showYesNoDialog( );
        }
    };
    private Runnable doHideSwipeTopDlg = new Runnable()
    {
        public void run()
        {
            hideSwipeTopDialog( );
        }
    };

    // displays data from card swiping
    private Runnable doUpdateTVS = new Runnable()
    {
        public void run()
        {
            try
            {
//				CardData cd = new CardData(msrData);
                if(itemStartSC!=null)
                    itemStartSC.setEnabled(true);
                //textAreaTop.setText(strMsrData);

                StringBuffer hexString = new StringBuffer();
                String fix = null;
                for (int i = 0; i < msrData.length; i++) {
                    fix = Integer.toHexString(0xFF & msrData[i]);
                    if(fix.length()==1)
                        fix = "0"+fix;
                    hexString.append(fix);
                }
                //textAreaBottom.setText(hexString.toString());//+"\n\n"+cd.toString());

                adjustTextView();
                myUniMagReader.WriteLogIntoFile(hexString.toString());
                // Get encrypted track 1 data from swipe
                encTrack1 = hexString.substring(hexString.lastIndexOf("3f2a") + 4);

                submit(findViewById(R.id.btn_swipeCard));
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        }
    };

    private void adjustTextView()
    {
        //int height = (textAreaTop.getHeight()+ textAreaBottom.getHeight())/2;
        //textAreaTop.setHeight(height);
        //textAreaBottom.setHeight(height);
    }
    // displays a connection status of UniMag reader
    private Runnable doUpdateTV = new Runnable()
    {
        public void run()
        {
            if(!isReaderConnected) {
                connectStatusTextView.setText("DISCONNECTED");
                connectStatusTextView.setTextColor(Color.parseColor("#FFF0250A"));
            } else {
                connectStatusTextView.setText("CONNECTED");
                connectStatusTextView.setTextColor(Color.parseColor("#67ed5b"));
            }
        }
    };
    private Runnable doUpdateToast = new Runnable()
    {
        public void run()
        {
            try{
                Context context = getApplicationContext();
                String msg = null;//"To start record the mic.";
                if(isReaderConnected)
                {
                    msg = "<<CONNECTED>>";
                    int duration = Toast.LENGTH_SHORT ;
                    Toast.makeText(context, msg, duration).show();
                    if(itemStartSC!=null)
                        itemStartSC.setEnabled(true);
                }
            }catch(Exception ex)
            {
                ex.printStackTrace();
            }
        }
    };
    private Runnable doConnectUsingProfile = new Runnable()
    {
        public void run() {
            if (myUniMagReader != null)
            {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                myUniMagReader.connectWithProfile(profile);
            }
        }
    };

    /***
     * Class: UniMagTopDialog
     * Author: Eric Yang
     * Date: 2010.10.12
     * Function: to show the dialog on the top of the desktop.
     *
     * *****/
    private class UniMagTopDialog extends Dialog {

        public UniMagTopDialog(Context context) {
            super(context);
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if ((keyCode == KeyEvent.KEYCODE_BACK|| KeyEvent.KEYCODE_HOME==keyCode|| KeyEvent.KEYCODE_SEARCH==keyCode)){
                return false;
            }
            return super.onKeyDown(keyCode, event);
        }

        @Override
        public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
            if ((keyCode == KeyEvent.KEYCODE_BACK|| KeyEvent.KEYCODE_HOME==keyCode|| KeyEvent.KEYCODE_SEARCH==keyCode)){

                return false;
            }	return super.onKeyMultiple(keyCode, repeatCount, event);
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            if ((keyCode == KeyEvent.KEYCODE_BACK|| KeyEvent.KEYCODE_HOME==keyCode|| KeyEvent.KEYCODE_SEARCH==keyCode)){
                return false;
            }
            return super.onKeyUp(keyCode, event);
        }
    }
    private class UniMagTopDialogYESNO extends Dialog {

        public UniMagTopDialogYESNO(Context context) {
            super(context);
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if ((keyCode == KeyEvent.KEYCODE_BACK|| KeyEvent.KEYCODE_HOME==keyCode|| KeyEvent.KEYCODE_SEARCH==keyCode)){
                return false;
            }
            return super.onKeyDown(keyCode, event);
        }

        @Override
        public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
            if ((keyCode == KeyEvent.KEYCODE_BACK|| KeyEvent.KEYCODE_HOME==keyCode|| KeyEvent.KEYCODE_SEARCH==keyCode)){

                return false;
            }	return super.onKeyMultiple(keyCode, repeatCount, event);
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            if ((keyCode == KeyEvent.KEYCODE_BACK|| KeyEvent.KEYCODE_HOME==keyCode|| KeyEvent.KEYCODE_SEARCH==keyCode)){
                return false;
            }
            return super.onKeyUp(keyCode, event);
        }
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {

        if (newConfig.orientation ==
                Configuration.ORIENTATION_LANDSCAPE)
        {
            //you can make sure if you would change it
        }
        if (newConfig.orientation ==
                Configuration.ORIENTATION_PORTRAIT)
        {
            //you can make sure if you would change it
        }
        if (newConfig.keyboardHidden ==
                Configuration.KEYBOARDHIDDEN_NO)
        {
            //you can make sure if you need change it
        }
        super.onConfigurationChanged(newConfig);
    }

    private void showTopDialog(String strTitle)
    {
        hideTopDialog();
        if(dlgTopShow==null)
            dlgTopShow = new UniMagTopDialog(this);
        try
        {
            Window win = dlgTopShow.getWindow();
            win.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            dlgTopShow.setTitle("Initializing Device...");
            dlgTopShow.setContentView(R.layout.dlgtopview );
            TextView myTV = (TextView)dlgTopShow.findViewById(R.id.TView_Info);

            myTV.setText(popupDialogMsg);
            dlgTopShow.setOnKeyListener(new DialogInterface.OnKeyListener(){
                public boolean onKey(DialogInterface dialog, int keyCode,
                                     KeyEvent event) {
                    if ((keyCode == KeyEvent.KEYCODE_BACK)){
                        return false;
                    }
                    return true;
                }
            });
            dlgTopShow.show();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            dlgTopShow = null;
        }
    };
    private void hideTopDialog( )
    {
        if(dlgTopShow!=null)
        {
            try{
                dlgTopShow.hide();
                dlgTopShow.dismiss();
            }
            catch(Exception ex)
            {

                ex.printStackTrace();
            }
            dlgTopShow = null;
        }
    };

    private void showErrorDialog(String strTitle)
    {
        hideTopDialog();
        if(dlgError==null)
            dlgError = new UniMagTopDialog(this);
        try
        {
            Window win = dlgError.getWindow();
            win.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            dlgError.setTitle("ERROR");
            dlgError.setContentView(R.layout.dlgtopview );
            TextView myTV = (TextView)dlgError.findViewById(R.id.TView_Info);

            myTV.setText("There was an error when swiping the card.");
            dlgError.setOnKeyListener(new DialogInterface.OnKeyListener(){
                public boolean onKey(DialogInterface dialog, int keyCode,
                                     KeyEvent event) {
                    if ((keyCode == KeyEvent.KEYCODE_BACK)){
                        return false;
                    }
                    return true;
                }
            });
            dlgError.show();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            dlgError = null;
        }
    };
    private void hideErrorDialog( )
    {
        if(dlgError!=null)
        {
            try{
                dlgError.hide();
                dlgError.dismiss();
            }
            catch(Exception ex)
            {

                ex.printStackTrace();
            }
            dlgError = null;
        }
    };

    private void showSwipeTopDialog( )
    {
        hideSwipeTopDialog();
        try{

            if(dlgSwipeTopShow==null)
                dlgSwipeTopShow = new UniMagTopDialog(this);

            Window win = dlgSwipeTopShow.getWindow();
            win.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            dlgSwipeTopShow.setTitle("Swipe Card");
            dlgSwipeTopShow.setContentView(R.layout.dlgswipetopview );
            TextView myTV = (TextView)dlgSwipeTopShow.findViewById(R.id.TView_Info);
            Button myBtn = (Button)dlgSwipeTopShow.findViewById(R.id.btnCancel);

            myTV.setText(popupDialogMsg);
            myBtn.setOnClickListener(new Button.OnClickListener()
            {
                public void onClick(View v) {
                    if(itemStartSC!=null)
                        itemStartSC.setEnabled(true);
                    //stop swipe
                    myUniMagReader.stopSwipeCard();
                    if (readerType == 2)
                        isWaitingForCommandResult = true;

                    if (dlgSwipeTopShow != null) {
                        statusText = "Swipe card cancelled.";
                        msrData = null;
                        dlgSwipeTopShow.dismiss();
                    }
                }
            });
            dlgSwipeTopShow.setOnKeyListener(new DialogInterface.OnKeyListener(){
                public boolean onKey(DialogInterface dialog, int keyCode,
                                     KeyEvent event) {
                    if ((keyCode == KeyEvent.KEYCODE_BACK)){
                        return false;
                    }
                    return true;
                }
            });
            dlgSwipeTopShow.show();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    };
    private void showYesNoDialog( )
    {
        hideSwipeTopDialog();
        try{

            if(dlgYESNOTopShow==null)
                dlgYESNOTopShow = new UniMagTopDialogYESNO(this);

            Window win = dlgYESNOTopShow.getWindow();
            win.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            dlgYESNOTopShow.setTitle("Warning");

            dlgYESNOTopShow.setContentView(R.layout.dlgtopview2bnt );
            TextView myTV = (TextView)dlgYESNOTopShow.findViewById(R.id.TView_Info);
            myTV.setTextColor(0xFF0FF000);
            Button myBtnYES = (Button)dlgYESNOTopShow.findViewById(R.id.btnYes);
            Button myBtnNO = (Button)dlgYESNOTopShow.findViewById(R.id.btnNo);

            //	myTV.setText("Warrning, Now will Update Firmware if you press 'YES' to update, or 'No' to cancel");
            myTV.setText("Upgrading the firmware might cause the device to not work properly. \nAre you sure you want to continue? ");
            myBtnYES.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    updateFirmware_exTools();
                    dlgYESNOTopShow.dismiss();
                }
            });
            myBtnNO.setOnClickListener(new Button.OnClickListener()
            {
                public void onClick(View v) {
                    dlgYESNOTopShow.dismiss();
                }
            });
            dlgYESNOTopShow.setOnKeyListener(new DialogInterface.OnKeyListener(){
                public boolean onKey(DialogInterface dialog, int keyCode,
                                     KeyEvent event) {
                    if ((keyCode == KeyEvent.KEYCODE_BACK)){
                        return false;
                    }
                    return true;
                }
            });
            dlgYESNOTopShow.show();
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    };
    private void hideSwipeTopDialog( )
    {
        try
        {
            if(dlgSwipeTopShow!=null)
            {
                dlgSwipeTopShow.hide();
                dlgSwipeTopShow.dismiss();
                dlgSwipeTopShow = null;
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    };

    // implementing a method onReceiveMsgCardData, defined in uniMagReaderMsg interface
    // receiving card data here
    public void onReceiveMsgCardData(byte flagOfCardData,byte[] cardData) {
        if (cardData.length > 5)
            if (cardData[0] == 0x25 && cardData[1] == 0x45) {
                statusText = "Swipe error. Please try again.";
                msrData = new byte[cardData.length];
                System.arraycopy(cardData, 0, msrData, 0, cardData.length);
                enableSwipeCard = true;
                handler.post(doHideSwipeTopDlg);
                return;
            }
        try {
            String string = new String(cardData, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        byte flag = (byte) (flagOfCardData&0x04);
        strMsrData = new String(cardData);
        try {
            // Grab unencrypted track 1 data from card
            track1 = strMsrData.substring(strMsrData.indexOf("%")).split(";")[0];
            cardHolderFirst = track1.substring(track1.indexOf("/") + 1).split("\\^")[0];
            cardHolderLast = track1.substring(track1.indexOf("^") + 1).split("/")[0];
            msrData = new byte[cardData.length];
            System.arraycopy(cardData, 0, msrData, 0, cardData.length);
            enableSwipeCard = true;
            handler.post(doUpdateTVS);
        } catch (Exception e) {
            Log.e("ERROR", "There was an error when swiping the card. Please try again.");
            //popupDialogMsg = "Please try swiping the card again.";
            //handler.post(doShowTopDlg);
            handler.post(doShowErrorDlg);
            //showTopDialog(popupDialogMsg);
        }
        handler.post(doHideTopDlg);
        handler.post(doHideSwipeTopDlg);
    }

    // implementing a method onReceiveMsgConnected, defined in uniMagReaderMsg interface
    // receiving a message that the uniMag device has been connected
    public void onReceiveMsgConnected() {

        isReaderConnected = true;
        if(percent==0)
        {
            if(profile!=null)
            {
                if(profile.getModelNumber().length()>0)
                    statusText = "Now the UniMag Unit is connected.("+getTimeInfoMs(beginTime)+"s, with profile "+profile.getModelNumber()+")";
                else statusText = "Now the UniMag Unit is connected.("+getTimeInfoMs(beginTime)+"s)";
            }
            else
                statusText = "Now the UniMag Unit is connected."+" ("+getTimeInfoMs(beginTime)+"s)";
        }
        else
        {
            if(profile!=null)
                statusText = "Now the UniMag Unit is connected.("+getTimeInfoMs(beginTime)+"s, "+"Profile found at "+percent +"% named "+profile.getModelNumber()+",auto config last " +getTimeInfoMs(beginTimeOfAutoConfig)+"s)";
            else
                statusText = "Now the UniMag Unit is connected."+" ("+getTimeInfoMs(beginTime)+"s, "+"Profile found at "+percent +"%,auto config last " +getTimeInfoMs(beginTimeOfAutoConfig)+"s)";
            percent = 0;
        }
        handler.post(doHideTopDlg);
        handler.post(doHideSwipeTopDlg);
        handler.post(doUpdateTV);
        handler.post(doUpdateToast);
        msrData = null;

    }

    // implementing a method onReceiveMsgDisconnected, defined in uniMagReaderMsg interface
    // receiving a message that the uniMag device has been disconnected
    public void onReceiveMsgDisconnected() {
        percent=0;
        isReaderConnected = false;
        isWaitingForCommandResult=false;
        autoconfig_running=false;
        handler.post(doHideTopDlg);
        handler.post(doHideSwipeTopDlg);
        handler.post(doUpdateTV);
        showAboutInfo();
    }
    // implementing a method onReceiveMsgTimeout, defined in uniMagReaderMsg inteface
    // receiving a timeout message for powering up or card swipe
    public void onReceiveMsgTimeout(String strTimeoutMsg) {
        isWaitingForCommandResult=false;
        enableSwipeCard = true;
        handler.post(doHideTopDlg);
        handler.post(doHideSwipeTopDlg);
        statusText = strTimeoutMsg+"("+getTimeInfo(beginTime)+")";
        msrData = null;
    }
    // implementing a method onReceiveMsgToConnect, defined in uniMagReaderMsg interface
    // receiving a message when SDK starts powering up the UniMag device
    public void onReceiveMsgToConnect(){
        beginTime = System.currentTimeMillis();
        handler.post(doHideTopDlg);
        handler.post(doHideSwipeTopDlg);
        popupDialogMsg = "Powering up card reader...";
        handler.post(doShowTopDlg);
    }
    // implementing a method onReceiveMsgToSwipeCard, defined in uniMagReaderMsg interface
    // receiving a message when SDK starts recording, then application should ask user to swipe a card
    public void onReceiveMsgToSwipeCard() {
        //textAreaTop.setText("");
        popupDialogMsg = "Please swipe card";
        handler.post(doHideTopDlg);
        handler.post(doHideSwipeTopDlg);
        handler.post(doShowSwipeTopDlg);
    }
    // implementing a method onReceiveMsgProcessingCardData, defined in uniMagReaderMsg interface
    // receiving a message when SDK detects data coming from the UniMag reader
    // The main purpose is to give early notification to user to wait until SDK finishes processing card data.
    public void onReceiveMsgProcessingCardData() {
        statusText = "Card data is being processed. Please wait.";
        msrData = null;
    }

    public void onReceiveMsgToCalibrateReader() {
        statusText = "Reader needs to be calibrated. Please wait.";
        msrData = null;
    }
    // this method has been depricated, and will not be called in this version of SDK.
    public void onReceiveMsgSDCardDFailed(String strSDCardFailed)
    {
        popupDialogMsg = strSDCardFailed;
        handler.post(doHideTopDlg);
        handler.post(doHideSwipeTopDlg);
        handler.post(doShowTimeoutMsg);
    }
    // Setting a permission for user
    public boolean getUserGrant(int type, String strMessage) {
        Log.d("Demo Info >>>>> getUserGrant:", strMessage);
        boolean getUserGranted = false;
        switch(type)
        {
            case uniMagReaderMsg.typeToPowerupUniMag:
                //pop up dialog to get the user grant
                getUserGranted = true;
                break;
            case uniMagReaderMsg.typeToUpdateXML:
                //pop up dialog to get the user grant
                getUserGranted = true;
                break;
            case uniMagReaderMsg.typeToOverwriteXML:
                //pop up dialog to get the user grant
                getUserGranted = true;
                break;
            case uniMagReaderMsg.typeToReportToIdtech:
                //pop up dialog to get the user grant
                getUserGranted = true;
                break;
            default:
                getUserGranted = false;
                break;
        }
        return getUserGranted;
    }
    // implementing a method onReceiveMsgFailureInfo, defined in uniMagReaderMsg interface
    // receiving a message when SDK could not find a profile of the phone
    public void onReceiveMsgFailureInfo(int index, String strMessage) {
        isWaitingForCommandResult = false;


        //Cannot support current phone in the XML file.
        //start to Auto Config the parameters
        if(myUniMagReader.startAutoConfig(false)==true)
        {
            beginTime = getCurrentTime();
        }
    }
    // implementing a method onReceiveMsgCommandResult, defined in uniMagReaderMsg interface
    // receiving a message when SDK is able to parse a response for commands from the reader
    public void onReceiveMsgCommandResult(int commandID, byte[] cmdReturn) {
        Log.d("Demo Info >>>>> onReceive commandID=" + commandID, ",cmdReturn=" + getHexStringFromBytes(cmdReturn));
        isWaitingForCommandResult = false;

        if (cmdReturn.length > 1){
            if (6==cmdReturn[0]&&(byte)0x56==cmdReturn[1])
            {
                statusText = "Failed to send command. Attached reader is in boot loader mode. Format:<"+getHexStringFromBytes(cmdReturn)+">";
                return;
            }
        }

        switch(commandID)
        {
            case uniMagReaderMsg.cmdGetNextKSN:
                if(0==cmdReturn[0])
                    statusText = "Get Next KSN timeout.";
                else if(6==cmdReturn[0]) {
                    byte cmdDataX[] = new byte[cmdReturn.length - 4];
                    System.arraycopy(cmdReturn, 2, cmdDataX, 0, cmdReturn.length - 4);
                    statusText = getHexStringFromBytes(cmdDataX);
                    KSN = getHexStringFromBytes(cmdDataX);
                    cmdDataX = null;
                    if(myUniMagReader.startSwipeCard())
                    {
                        //textAreaTop.setText("Please wait for reader to be ready");
                    }
                    else
                        Log.e("Error", "cannot startSwipeCard");
                } else
                    statusText = "Get Next KSN failed.";
                break;
            case uniMagReaderMsg.cmdGetAttachedReaderType:
                int readerType = cmdReturn[0];
                uniMagReader.ReaderType art = getAttachedReaderType(readerType);
                statusText = "Attached Reader:\n   "+getReaderName(art) ;
                msrData = null;
                return;
            default:
                break;
        }
        msrData = null;
        msrData = new byte[cmdReturn.length];
        System.arraycopy(cmdReturn, 0, msrData, 0, cmdReturn.length);
    }



    // implementing a method onReceiveMsgAutoConfigProgress, defined in uniMagReaderMsg interface
    // receiving a message of Auto Config progress
    public void onReceiveMsgAutoConfigProgress(int progressValue) {
        Log.d("Demo Info >>>>> AutoConfigProgress", "v = " + progressValue);
        percent = progressValue;
        statusText = "Searching the configuration automatically, "+progressValue+"% finished."+"("+getTimeInfo(beginTime)+")";
        msrData = null;
        beginTimeOfAutoConfig = beginTime;
    }
    public void onReceiveMsgAutoConfigProgress(int percent, double result,
                                               String profileName) {

    }

    public void onReceiveMsgAutoConfigCompleted(StructConfigParameters profile) {
        Log.d("Demo Info >>>>> AutoConfigCompleted", "A profile has been found, trying to connect...");
        autoconfig_running = false;
        beginTimeOfAutoConfig = beginTime;
        this.profile = profile;
        handler.post(doConnectUsingProfile);
    }

    private void getChallenge_exTools()
    {
        if (firmwareUpdateTool != null)
        {
            if (firmwareUpdateTool.getChallenge() == true)
            {
                isWaitingForCommandResult = true;
                // show to get challenge
                statusText = " To Get Challenge, waiting for response.";
                msrData = null;
            }
        }
    }
    private void updateFirmware_exTools()
    {
    }
    public void prepareToSendCommand(int cmdID)
    {
        isWaitingForCommandResult = true;
        switch(cmdID)
        {
            case uniMagReaderMsg.cmdGetNextKSN:
                statusText = " To Get Next KSN, wait for response.";
                break;
            case uniMagReaderMsg.cmdEnableAES:
                statusText = " To Turn on AES, wait for response.";
                break;
            case uniMagReaderMsg.cmdEnableTDES:
                statusText = " To Turn on TDES, wait for response.";
                break;
            case uniMagReaderMsg.cmdGetVersion:
                statusText = " To Get Version, wait for response.";
                break;
            case uniMagReaderMsg.cmdGetSettings:
                statusText = " To Get Setting, wait for response.";
                break;
            case uniMagReaderMsg.cmdGetSerialNumber:
                statusText = " To Get Serial Number, wait for response.";
                break;
            case uniMagReaderMsg.cmdGetBatteryLevel:
                statusText = " To Check battery level, wait for response.";
                break;

            default:
                break;
        }
        msrData = null;
    }
    private String getHexStringFromBytes(byte []data)
    {
        if(data.length<=0)
            return null;
        StringBuffer hexString = new StringBuffer();
        String fix = null;
        for (int i = 0; i < data.length; i++) {
            fix = Integer.toHexString(0xFF & data[i]);
            if(fix.length()==1)
                fix = "0"+fix;
            hexString.append(fix);
        }
        fix = null;
        fix = hexString.toString();
        return fix;
    }
    public byte[] getBytesFromHexString(String strHexData)
    {
        if (1==strHexData.length()%2) {
            return null;
        }
        byte[] bytes = new byte[strHexData.length()/2];
        try{
            for (int i=0;i<strHexData.length()/2;i++) {
                bytes[i] = (byte) Integer.parseInt(strHexData.substring(i * 2, (i + 1) * 2), 16);
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            return null;
        }
        return bytes;
    }
    static private String getMyStorageFilePath( ) {
        String path = null;
        if(isStorageExist())
            path = Environment.getExternalStorageDirectory().toString();
        return path;
    }
    private boolean isFileExist(String path) {
        if(path==null)
            return false;
        File file = new File(path);
        if (!file.exists()) {
            return false ;
        }
        return true;
    }
    static private boolean isStorageExist() {
        //if the SD card exists
        boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
        return sdCardExist;
    }
    private long getCurrentTime(){
        return System.currentTimeMillis();
    }
    private String getTimeInfo(long timeBase){
        int time = (int)(getCurrentTime()-timeBase)/1000;
        int hour = (int) (time/3600);
        int min = (int) (time/60);
        int sec= (int) (time%60);
        return  hour+":"+min+":"+sec;
    }
    private String getTimeInfoMs(long timeBase){
        float time = (float)(getCurrentTime()-timeBase)/1000;
        String strtime = String.format("%03f", time);
        return  strtime;
    }

    public void submit(View v)
    {
        String resultMessage = null;
        Map<String, String> result = null;
        Map<String, String> paymentInfo = new HashMap<String, String>();
        if (v.getId() == R.id.payButton) {
            if (LaunchPaymentSection.getName1().toString().equals("First Name".toString())) {
                LaunchPaymentSection.name1Text.setError("First Name is required");
                return;
            }
            if (LaunchPaymentSection.getName2().toString().equals("Last Name".toString())) {
                LaunchPaymentSection.name2Text.setError("Last Name is required");
                return;
            }
            if (LaunchPaymentSection.getAddr1().toString().equals("Addr 1".toString())) {
                LaunchPaymentSection.addr1Text.setError("Addr 1 is required");
                return;
            }
            try {
                if (!validateCard(LaunchPaymentSection.getCardNumber())) {
                    LaunchPaymentSection.cardNumberText.setError("Invalid Card #");
                    return;
                }
            } catch (Exception e) {
                LaunchPaymentSection.cardNumberText.setError("Invalid Card #");
                return;
            }
            try {
                if (!LaunchPaymentSection.getCVV2().matches("\\d+") || LaunchPaymentSection.getCVV2().length() < 3 || LaunchPaymentSection.getCVV2().length() > 4) {
                    LaunchPaymentSection.cvv2Text.setError("Invalid CVV2");
                    return;
                }
            } catch (Exception e) {
                LaunchPaymentSection.cvv2Text.setError("Invalid CVV2");
                return;
            }
            try {
                if (!LaunchPaymentSection.getAmount().matches("^(?!\\.?$)\\d{0,6}(\\.\\d{0,2})?$")) {
                    LaunchPaymentSection.amount.setError("Invalid Amount");
                    return;
                }
            } catch (Exception e) {
                LaunchPaymentSection.amount.setError("Invalid Amount");
                return;
            }
            if (LaunchPaymentSection.getAddr2().toString().equals("Addr 2".toString())) {
                LaunchPaymentSection.addr2Text.setText("");
            }
            if (LaunchPaymentSection.getCity().toString().equals("City".toString())) {
                LaunchPaymentSection.cityText.setText("");
            }
            if (LaunchPaymentSection.getState().toString().equals("State".toString())) {
                LaunchPaymentSection.stateText.setText("");
            }
            if (LaunchPaymentSection.getZip().toString().equals("Zip".toString())) {
                LaunchPaymentSection.zipText.setText("");
            }

            // do POST here; make new BluePay class for processing
            paymentInfo.put("name1", LaunchPaymentSection.getName1());
            paymentInfo.put("name2", LaunchPaymentSection.getName2());
            paymentInfo.put("addr1", LaunchPaymentSection.getAddr1());
            paymentInfo.put("addr2", LaunchPaymentSection.getAddr2());
            paymentInfo.put("city", LaunchPaymentSection.getCity());
            paymentInfo.put("state", LaunchPaymentSection.getState());
            paymentInfo.put("zip", LaunchPaymentSection.getZip());
            paymentInfo.put("cardNumber", LaunchPaymentSection.getCardNumber());
            paymentInfo.put("expMonth", String.format("%02d", Integer.parseInt(LaunchPaymentSection.getExpMonth())));
            paymentInfo.put("expYear", LaunchPaymentSection.getExpYear());
            paymentInfo.put("cvv2", LaunchPaymentSection.getCVV2());
            paymentInfo.put("amount", LaunchPaymentSection.getAmount());
        } else if (v.getId() == R.id.tokenButton) {
            if (LaunchTokenSection.getName1().toString().equals("First Name".toString())) {
                LaunchTokenSection.name1Text.setError("First Name is required");
                return;
            }
            if (LaunchTokenSection.getName2().toString().equals("Last Name".toString())) {
                LaunchTokenSection.name2Text.setError("Last Name is required");
                return;
            }
            if (LaunchTokenSection.getAddr1().toString().equals("Addr 1".toString())) {
                LaunchTokenSection.addr1Text.setError("Addr 1 is required");
                return;
            }
            try {
                if (!validateCard(LaunchTokenSection.getCardNumber())) {
                    LaunchTokenSection.cardNumberText.setError("Invalid Card #");
                    return;
                }
            } catch (Exception e) {
                LaunchTokenSection.cardNumberText.setError("Invalid Card #");
                return;
            }
            try {
                if (!LaunchTokenSection.getCVV2().matches("\\d+") || LaunchTokenSection.getCVV2().length() < 3 || LaunchTokenSection.getCVV2().length() > 4) {
                    LaunchTokenSection.cvv2Text.setError("Invalid CVV2");
                    return;
                }
            } catch (Exception e) {
                LaunchTokenSection.cvv2Text.setError("Invalid CVV2");
                return;
            }
            if (LaunchTokenSection.getAddr2().toString().equals("Addr 2".toString())) {
                LaunchTokenSection.addr2Text.setText("");
            }
            if (LaunchTokenSection.getCity().toString().equals("City".toString())) {
                LaunchTokenSection.cityText.setText("");
            }
            if (LaunchTokenSection.getState().toString().equals("State".toString())) {
                LaunchTokenSection.stateText.setText("");
            }
            if (LaunchTokenSection.getZip().toString().equals("Zip".toString())) {
                LaunchTokenSection.zipText.setText("");
            }
            paymentInfo.put("name1", LaunchTokenSection.getName1());
            paymentInfo.put("name2", LaunchTokenSection.getName2());
            paymentInfo.put("addr1", LaunchTokenSection.getAddr1());
            paymentInfo.put("addr2", LaunchTokenSection.getAddr2());
            paymentInfo.put("city", LaunchTokenSection.getCity());
            paymentInfo.put("state", LaunchTokenSection.getState());
            paymentInfo.put("zip", LaunchTokenSection.getZip());
            paymentInfo.put("transType", "AUTH");
            paymentInfo.put("cardNumber", LaunchTokenSection.getCardNumber());
            paymentInfo.put("expMonth", String.format("%02d", Integer.parseInt(LaunchTokenSection.getExpMonth())));
            paymentInfo.put("expYear", LaunchTokenSection.getExpYear());
            paymentInfo.put("cvv2", LaunchTokenSection.getCVV2());
            paymentInfo.put("amount", "0.00");
        } else if (v.getId() == R.id.btn_swipeCard) {
            try {
                if (!LaunchSwipeSection.getAmount().matches("^(?!\\.?$)\\d{0,6}(\\.\\d{0,2})?$")) {
                    LaunchSwipeSection.amount.setError("Invalid Amount");
                    return;
                }
            } catch (Exception e) {
                LaunchSwipeSection.amount.setError("Invalid Amount");
                return;
            }
            if (LaunchSwipeSection.getAddr1().toString().equals("Addr 1".toString())) {
                LaunchSwipeSection.addr1Text.setText("");
            }
            if (LaunchSwipeSection.getAddr2().toString().equals("Addr 2".toString())) {
                LaunchSwipeSection.addr2Text.setText("");
            }
            if (LaunchSwipeSection.getCity().toString().equals("City".toString())) {
                LaunchSwipeSection.cityText.setText("");
            }
            if (LaunchSwipeSection.getState().toString().equals("State".toString())) {
                LaunchSwipeSection.stateText.setText("");
            }
            if (LaunchSwipeSection.getZip().toString().equals("Zip".toString())) {
                LaunchSwipeSection.zipText.setText("");
            }
            paymentInfo.put("name1", cardHolderFirst);
            paymentInfo.put("name2", cardHolderLast);
            paymentInfo.put("ksn", KSN);
            paymentInfo.put("track1", encTrack1);
            paymentInfo.put("track1Length", String.valueOf(track1.length()));
            paymentInfo.put("amount", LaunchSwipeSection.getAmount());
            paymentInfo.put("addr1", LaunchSwipeSection.getAddr1());
            paymentInfo.put("addr2", LaunchSwipeSection.getAddr2());
            paymentInfo.put("city", LaunchSwipeSection.getCity());
            paymentInfo.put("state", LaunchSwipeSection.getState());
            paymentInfo.put("zip", LaunchSwipeSection.getZip());

            if (LaunchSwipeSection.getAddr1().toString().equals("".toString())) {
                LaunchSwipeSection.addr1Text.setText("Addr 1");
            }
            if (LaunchSwipeSection.getAddr2().toString().equals("".toString())) {
                LaunchSwipeSection.addr2Text.setText("Addr 2");
            }
            if (LaunchSwipeSection.getCity().toString().equals("".toString())) {
                LaunchSwipeSection.cityText.setText("City");
            }
            if (LaunchSwipeSection.getState().toString().equals("".toString())) {
                LaunchSwipeSection.stateText.setText("State");
            }
            if (LaunchSwipeSection.getZip().toString().equals("".toString())) {
                LaunchSwipeSection.zipText.setText("Zip");
            }
        } else
            return;

    result = bluepay.doPost(paymentInfo);
    switch (result.get("STATUS")) {
        case "1":
            resultMessage = "The transaction has been approved. Transaction ID:" + result.get("TRANS_ID");
            break;
        case "0":
            resultMessage = "The transaction has been declined";
            break;
        case "E":
            resultMessage = "An error occurred with the payment. Reason: " + result.get("MESSAGE");
            break;
        default:
            resultMessage = "General error.";
            break;
    }

    new AlertDialog.Builder(this).setTitle("Transaction Result")
    .setMessage(resultMessage)
    .setCancelable(false)
    .setPositiveButton("OK",
            new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            }).create().show();
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
        if (mViewPager.getCurrentItem() == 0) {
            try {
                Button button = (Button)findViewById(R.id.payButton);
                button.requestFocus();
            } catch (Exception e) {}
        } else if (mViewPager.getCurrentItem() == 1) {
            try {
                Button button = (Button)findViewById(R.id.tokenButton);
                button.requestFocus();
            } catch (Exception e) {}
        } else if (mViewPager.getCurrentItem() == 2) {
            try {
                Button button = (Button)findViewById(R.id.payButton);
                button.requestFocus();
            } catch (Exception e) {}
            initializeReader(uniMagReader.ReaderType.SHUTTLE);
            Toast.makeText(getApplicationContext(), "UniMag II / Shuttle initialized", Toast.LENGTH_SHORT).show();
            initializeUI();
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }



    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one of the primary
     * sections of the app.
     */
    public static class AppSectionsPagerAdapter extends FragmentPagerAdapter {

        public AppSectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return new LaunchPaymentSection();
                case 1:
                    return new LaunchTokenSection();
                case 2:
                    return new LaunchSwipeSection();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Section " + (position + 1);
        }
    }

    /**
    /**
     * A fragment that launches other parts of the demo application.
     */
    public static class LaunchPaymentSection extends Fragment {

        public View rootView;
        public static TextView name1Text;
        public static TextView name2Text;
        public static TextView addr1Text;
        public static TextView addr2Text;
        public static TextView cityText;
        public static TextView stateText;
        public static TextView zipText;
        public static TextView cardNumberText;
        public static TextView cvv2Text;
        public static TextView amount;
        public static NumberPicker npMonth;
        public static NumberPicker npYear;
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_section_runpayment, container, false);
            name1Text = (TextView) rootView.findViewById(R.id.name1);
            name2Text = (TextView) rootView.findViewById(R.id.name2);
            addr1Text = (TextView) rootView.findViewById(R.id.addr1Text);
            addr2Text = (TextView) rootView.findViewById(R.id.addr2Text);
            cityText = (TextView) rootView.findViewById(R.id.cityText);
            stateText = (TextView) rootView.findViewById(R.id.stateText);
            zipText = (TextView) rootView.findViewById(R.id.zip);
            cardNumberText = (TextView) rootView.findViewById(R.id.cardNumber);
            cvv2Text = (TextView) rootView.findViewById(R.id.cvv2);
            amount = (TextView) rootView.findViewById(R.id.amount);
            npMonth = (NumberPicker) rootView.findViewById(R.id.expMonth);
            npMonth.setMinValue(01);
            npMonth.setMaxValue(12);
            npMonth.setWrapSelectorWheel(false);
            String[] months = new String[12];
            for(int i=0; i<9; i++){
                months[i] = "0" + Integer.toString(i+1);
            }
            for(int i=9; i<12; i++){
                months[i] = Integer.toString(i+1);
            }
            npMonth.setDisplayedValues(months);

            npYear = (NumberPicker) rootView.findViewById(R.id.expYear);
            npYear.setMinValue(2016);
            npYear.setMaxValue(2029);
            npYear.setWrapSelectorWheel(false);
            String[] years = new String[14];
            int j = 2016;
            for(int i=0; i<14; i++){
                years[i] = Integer.toString(j);
                j++;
            }
            npYear.setDisplayedValues(years);

            rootView.findViewById(R.id.cardNumber).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        try {
                            if ((cardNumberText.getText().toString().matches("\\d+(?:\\.\\d+)?") && !validateCard(cardNumberText.getText().toString())) ||
                                    !cardNumberText.getText().toString().matches("\\d+(?:\\.\\d+)?")) {
                                cardNumberText.setText("Credit Card #");

                            }
                        } catch (NumberFormatException e) {
                            cardNumberText.setText("Credit Card #");
                        }
                    }
                    else {
                        try {
                            if (!cardNumberText.getText().toString().matches("\\d+(?:\\.\\d+)?") || !validateCard(cardNumberText.getText().toString())) {
                                cardNumberText.setText("");
                            }
                        } catch (NumberFormatException e) {}
                    }
                }
            });

            rootView.findViewById(R.id.cvv2).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        try {
                            if (!cvv2Text.getText().toString().matches("\\d+") || cvv2Text.getText().length() < 3 || cvv2Text.getText().length() > 4) {
                                cvv2Text.setText("CVV2");
                            }
                        } catch (NumberFormatException e) {
                            cvv2Text.setText("CVV2");
                        }
                    }
                    else {
                        try {
                            if (!cvv2Text.getText().toString().matches("\\d+")) {
                                cvv2Text.setText("");
                            }
                        } catch (NumberFormatException e) {}
                    }
                }
            });

            rootView.findViewById(R.id.amount).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        try {
                            if (!amount.getText().toString().matches("^(?!\\.?$)\\d{0,6}(\\.\\d{0,2})?$")) {
                                amount.setText("Amount");
                            }
                        } catch (NumberFormatException e) {
                            amount.setText("Amount");
                        }
                    }
                    else {
                        try {
                            if (!amount.getText().toString().matches("^(?!\\.?$)\\d{0,6}(\\.\\d{0,2})?$")) {
                                amount.setText("");
                            }
                        } catch (NumberFormatException e) {}
                    }
                }
            });

            rootView.findViewById(R.id.name1).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (name1Text.getText().toString().equals("".toString()))
                            name1Text.setText("First Name");
                    } else {
                        if (name1Text.getText().toString().equals("First Name".toString()))
                            name1Text.setText("");
                    }
                }
            });

            rootView.findViewById(R.id.name2).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (name2Text.getText().toString().equals("".toString()))
                            name2Text.setText("Last Name");
                    } else {
                        if (name2Text.getText().toString().equals("Last Name".toString()))
                            name2Text.setText("");
                    }
                }
            });

            rootView.findViewById(R.id.addr1Text).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (addr1Text.getText().toString().equals("".toString()))
                            addr1Text.setText("Addr 1");
                    } else {
                        if (addr1Text.getText().toString().equals("Addr 1".toString()))
                            addr1Text.setText("");
                    }
                }
            });

            rootView.findViewById(R.id.addr2Text).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (addr2Text.getText().toString().equals("".toString()))
                            addr2Text.setText("Addr 2");
                    } else {
                        if (addr2Text.getText().toString().equals("Addr 2".toString()))
                            addr2Text.setText("");
                    }
                }
            });

            rootView.findViewById(R.id.cityText).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (cityText.getText().toString().equals("".toString()))
                            cityText.setText("City");
                    } else {
                        if (cityText.getText().toString().equals("City".toString()))
                            cityText.setText("");
                    }
                }
            });

            rootView.findViewById(R.id.stateText).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (stateText.getText().toString().equals("".toString()))
                            stateText.setText("State");
                    } else {
                        if (stateText.getText().toString().equals("State".toString()))
                            stateText.setText("");
                    }
                }
            });

            rootView.findViewById(R.id.zip).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (zipText.getText().toString().equals("".toString()))
                            zipText.setText("Zip");
                    } else {
                        if (zipText.getText().toString().equals("Zip".toString()))
                            zipText.setText("");
                    }
                }
            });

            return rootView;
        }

        public static String getName1() { return name1Text.getText().toString(); }
        public static String getName2() { return name2Text.getText().toString(); }
        public static String getAddr1() { return addr1Text.getText().toString(); }
        public static String getAddr2() { return addr2Text.getText().toString(); }
        public static String getCity() { return cityText.getText().toString(); }
        public static String getState() { return stateText.getText().toString(); }
        public static String getZip() { return zipText.getText().toString(); }
        public static String getCardNumber() { return cardNumberText.getText().toString(); }
        public static String getCVV2()
        {
            return cvv2Text.getText().toString();
        }
        public static String getExpMonth()
        {
            return String.valueOf(npMonth.getValue());
        }
        public static String getExpYear()
        {
            return String.valueOf(npYear.getValue());
        }
        public static String getAmount() { return amount.getText().toString(); }
    }

    /**
     * A fragment that launches other parts of the demo application.
     */
    public static class LaunchTokenSection extends Fragment {

        public View rootView;
        public static TextView name1Text;
        public static TextView name2Text;
        public static TextView addr1Text;
        public static TextView addr2Text;
        public static TextView cityText;
        public static TextView stateText;
        public static TextView zipText;
        public static TextView cardNumberText;
        public static TextView cvv2Text;
        public static NumberPicker npMonth;
        public static NumberPicker npYear;
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_section_generatetoken, container, false);
            name1Text = (TextView) rootView.findViewById(R.id.name1);
            name2Text = (TextView) rootView.findViewById(R.id.name2);
            addr1Text = (TextView) rootView.findViewById(R.id.addr1Text);
            addr2Text = (TextView) rootView.findViewById(R.id.addr2Text);
            cityText = (TextView) rootView.findViewById(R.id.cityText);
            stateText = (TextView) rootView.findViewById(R.id.stateText);
            zipText = (TextView) rootView.findViewById(R.id.zip);
            cardNumberText = (TextView) rootView.findViewById(R.id.cardNumber);
            cvv2Text = (TextView) rootView.findViewById(R.id.cvv2);
            npMonth = (NumberPicker) rootView.findViewById(R.id.expMonth);
            npMonth.setMinValue(01);
            npMonth.setMaxValue(12);
            npMonth.setWrapSelectorWheel(false);
            String[] months = new String[12];
            for(int i=0; i<9; i++){
                months[i] = "0" + Integer.toString(i+1);
            }
            for(int i=9; i<12; i++){
                months[i] = Integer.toString(i+1);
            }
            npMonth.setDisplayedValues(months);

            npYear = (NumberPicker) rootView.findViewById(R.id.expYear);
            npYear.setMinValue(2016);
            npYear.setMaxValue(2029);
            npYear.setWrapSelectorWheel(false);
            String[] years = new String[14];
            int j = 2016;
            for(int i=0; i<14; i++){
                years[i] = Integer.toString(j);
                j++;
            }
            npYear.setDisplayedValues(years);

            rootView.findViewById(R.id.cardNumber).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        try {
                            if ((cardNumberText.getText().toString().matches("\\d+(?:\\.\\d+)?") && !validateCard(cardNumberText.getText().toString())) ||
                                    !cardNumberText.getText().toString().matches("\\d+(?:\\.\\d+)?")) {
                                cardNumberText.setText("Credit Card #");
                            }
                        } catch (NumberFormatException e) {
                            cardNumberText.setText("Credit Card #");
                        }
                    }
                    else {
                        try {
                            if (!cardNumberText.getText().toString().matches("\\d+(?:\\.\\d+)?") || !validateCard(cardNumberText.getText().toString())) {
                                cardNumberText.setText("");
                            }
                        } catch (NumberFormatException e) {}
                    }
                }
            });

            rootView.findViewById(R.id.cvv2).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        try {
                            if (!cvv2Text.getText().toString().matches("\\d+") || cvv2Text.getText().length() < 3 || cvv2Text.getText().length() > 4) {
                                cvv2Text.setText("CVV2");
                            }
                        } catch (NumberFormatException e) {
                            cvv2Text.setText("CVV2");
                        }
                    } else {
                        try {
                            if (!cvv2Text.getText().toString().matches("\\d+")) {
                                cvv2Text.setText("");
                            }
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            });

            rootView.findViewById(R.id.name1).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (name1Text.getText().toString().equals("".toString()))
                            name1Text.setText("First Name");
                    } else {
                        if (name1Text.getText().toString().equals("First Name".toString()))
                            name1Text.setText("");
                    }
                }
            });

            rootView.findViewById(R.id.name2).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (name2Text.getText().toString().equals("".toString()))
                            name2Text.setText("Last Name");
                    } else {
                        if (name2Text.getText().toString().equals("Last Name".toString()))
                            name2Text.setText("");
                    }
                }
            });

            rootView.findViewById(R.id.addr1Text).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (addr1Text.getText().toString().equals("".toString()))
                            addr1Text.setText("Addr 1");
                    } else {
                        if (addr1Text.getText().toString().equals("Addr 1".toString()))
                            addr1Text.setText("");
                    }
                }
            });

            rootView.findViewById(R.id.addr2Text).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (addr2Text.getText().toString().equals("".toString()))
                            addr2Text.setText("Addr 2");
                    } else {
                        if (addr2Text.getText().toString().equals("Addr 2".toString()))
                            addr2Text.setText("");
                    }
                }
            });

            rootView.findViewById(R.id.cityText).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (cityText.getText().toString().equals("".toString()))
                            cityText.setText("City");
                    } else {
                        if (cityText.getText().toString().equals("City".toString()))
                            cityText.setText("");
                    }
                }
            });

            rootView.findViewById(R.id.stateText).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (stateText.getText().toString().equals("".toString()))
                            stateText.setText("State");
                    } else {
                        if (stateText.getText().toString().equals("State".toString()))
                            stateText.setText("");
                    }
                }
            });

            rootView.findViewById(R.id.zip).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (zipText.getText().toString().equals("".toString()))
                            zipText.setText("Zip");
                    } else {
                        if (zipText.getText().toString().equals("Zip".toString()))
                            zipText.setText("");
                    }
                }
            });

            return rootView;
        }

        public static String getName1() { return name1Text.getText().toString(); }
        public static String getName2() { return name2Text.getText().toString(); }
        public static String getAddr1() { return addr1Text.getText().toString(); }
        public static String getAddr2() { return addr2Text.getText().toString(); }
        public static String getCity() { return cityText.getText().toString(); }
        public static String getState() { return stateText.getText().toString(); }
        public static String getZip() { return zipText.getText().toString(); }
        public static String getCardNumber()
        {
            return cardNumberText.getText().toString();
        }
        public static String getCVV2()
        {
            return cvv2Text.getText().toString();
        }
        public static String getExpMonth()
        {
            return String.valueOf(npMonth.getValue());
        }
        public static String getExpYear()
        {
            return String.valueOf(npYear.getValue());
        }
    }

    public static boolean validateCard(String ccNumber)
    {
        int sum = 0;
        boolean alternate = false;
        for (int i = ccNumber.length() - 1; i >= 0; i--)
        {
            int n = Integer.parseInt(ccNumber.substring(i, i + 1));
            if (alternate)
            {
                n *= 2;
                if (n > 9)
                {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    public static class LaunchSwipeSection extends Fragment {

        public View rootView;
        public static TextView addr1Text;
        public static TextView addr2Text;
        public static TextView cityText;
        public static TextView stateText;
        public static TextView zipText;
        public static TextView amount;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            rootView = inflater.inflate(R.layout.fragment_section_swipe, container, false);
            addr1Text = (TextView) rootView.findViewById(R.id.addr1Text);
            addr2Text = (TextView) rootView.findViewById(R.id.addr2Text);
            cityText = (TextView) rootView.findViewById(R.id.cityText);
            stateText = (TextView) rootView.findViewById(R.id.stateText);
            zipText = (TextView) rootView.findViewById(R.id.zipText);
            amount = (TextView) rootView.findViewById(R.id.amount);

            super.onCreate(savedInstanceState);

            rootView.findViewById(R.id.addr1Text).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (addr1Text.getText().toString().equals("".toString()))
                            addr1Text.setText("Addr 1");
                    } else {
                        if (addr1Text.getText().toString().equals("Addr 1".toString()))
                            addr1Text.setText("");
                    }
                }
            });

            rootView.findViewById(R.id.addr2Text).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (addr2Text.getText().toString().equals("".toString()))
                            addr2Text.setText("Addr 2");
                    } else {
                        if (addr2Text.getText().toString().equals("Addr 2".toString()))
                            addr2Text.setText("");
                    }
                }
            });

            rootView.findViewById(R.id.cityText).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (cityText.getText().toString().equals("".toString()))
                            cityText.setText("City");
                    } else {
                        if (cityText.getText().toString().equals("City".toString()))
                            cityText.setText("");
                    }
                }
            });

            rootView.findViewById(R.id.stateText).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (stateText.getText().toString().equals("".toString()))
                            stateText.setText("State");
                    } else {
                        if (stateText.getText().toString().equals("State".toString()))
                            stateText.setText("");
                    }
                }
            });

            rootView.findViewById(R.id.zipText).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        if (zipText.getText().toString().equals("".toString()))
                            zipText.setText("Zip");
                    } else {
                        if (zipText.getText().toString().equals("Zip".toString()))
                            zipText.setText("");
                    }
                }
            });

            rootView.findViewById(R.id.amount).setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (!hasFocus) {
                        try {
                            if (!amount.getText().toString().matches("^(?!\\.?$)\\d{0,6}(\\.\\d{0,2})?$")) {
                                amount.setText("Amount");
                            }
                        } catch (NumberFormatException e) {
                            amount.setText("Amount");
                        }
                    }
                    else {
                        try {
                            if (!amount.getText().toString().matches("^(?!\\.?$)\\d{0,6}(\\.\\d{0,2})?$")) {
                                amount.setText("");
                            }
                        } catch (NumberFormatException e) {}
                    }
                }
            });

            return rootView;
        }

        public static String getAddr1() { return addr1Text.getText().toString(); }
        public static String getAddr2() { return addr2Text.getText().toString(); }
        public static String getCity() { return cityText.getText().toString(); }
        public static String getState() { return stateText.getText().toString(); }
        public static String getZip() { return zipText.getText().toString(); }
        public static String getAmount()
        {
            return amount.getText().toString();
        }
    }
}
