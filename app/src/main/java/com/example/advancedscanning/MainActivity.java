package com.example.advancedscanning;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.advancedscanning.fmdslider.model.FMDSliderPageModel;
import com.example.advancedscanning.http.AsyncUpdateListener;
import com.example.advancedscanning.http.request.FMDBarCode;
import com.example.advancedscanning.http.request.FMDRequest;
import com.example.advancedscanning.http.request.PatientBag;
import com.example.advancedscanning.http.request.RequestQueueSingleton;
import com.example.advancedscanning.http.request.Store;
import com.example.advancedscanning.http.response.FMDPackInfo;
import com.example.advancedscanning.http.response.FMDResponse;
import com.example.advancedscanning.http.response.PackResponse;
import com.example.advancedscanning.utils.fragments.SmartFragmentStatePagerAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.EMDKResults;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.Scanner.DataListener;
import com.symbol.emdk.barcode.Scanner.StatusListener;
import com.symbol.emdk.barcode.ScannerConfig;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.ScannerInfo;
import com.symbol.emdk.barcode.StatusData;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class MainActivity extends AppCompatActivity implements AsyncUpdateListener, EMDKListener,
        StatusListener, DataListener{

    // Declare a variable to store EMDKManager object
    private EMDKManager emdkManager = null;
    // Declare a variable to store Barcode Manager object
    private BarcodeManager barcodeManager = null;
    // Declare a variable to hold scanner device to scan
    private Scanner scanner = null;
    // Button to scan barcodes through the app using soft trigger
    private Button scanButton;
    // Text view to display status of EMDK and Barcode Scanning Operations
    private TextView statusTextView = null;
    // Edit Text that is used to display scanned barcode data
    private EditText dataView = null;

    private Map<Integer, String> operations = new HashMap<Integer, String>();

    private RadioGroup radioGroupAction;
    private Button btnClearBag;
    private Button btnFmdRequest;
    private TextView textViewBagLabel;
    //private TextView textViewPacks;
    private ViewPager fmdPager;
    private SmartFragmentStatePagerAdapter fmdPagerAdapter;

    private Gson gson;
    private FMDRequest fmdRequest;
    private FMDResponse fmdRes;
    private String fmdHost;
    private FMDSliderPageModel fmdPageData;
    private Store store = Store.builder()
                                    .id("123")
                                    .name("Beeston")
                                .build();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // get fmd host from preferences
        PreferenceManager.setDefaultValues(this,
                R.xml.preferences, false);
        SharedPreferences sharedPref =
                PreferenceManager.getDefaultSharedPreferences(this);
        fmdHost = sharedPref.getString(SettingsActivity.KEY_PREF_FMD_SERVER, "");

        gson = new GsonBuilder().create();
        // Reference to UI elements
        statusTextView = findViewById(R.id.textViewStatus);
        textViewBagLabel = findViewById(R.id.textViewBagLabel);
        //textViewPacks = findViewById(R.id.textViewPacks);
        dataView = findViewById(R.id.editText1);

        RadioButton radioUndispense = findViewById(R.id.radioUndispense);
        RadioButton radioVerify = findViewById(R.id.radioVerify);
        RadioButton radioDispense = findViewById(R.id.radioDispense);
        radioGroupAction = findViewById(R.id.actionGroup);
        btnClearBag = findViewById(R.id.btn_clearBag);
        btnFmdRequest = findViewById(R.id.btn_sendBag);

        operations.put(radioVerify.getId(), "verify");
        operations.put(radioUndispense.getId(), "undo-dispense");
        operations.put(radioDispense.getId(), "dispense");

        // Add onClick listener for scan button to enable soft scan through app
        addScanButtonListener();

        btnFmdRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendFMDRequest();
            }
        });
        btnClearBag.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fmdRequest.clearBag();
                textViewBagLabel.setText("");
                dataView.getText().clear();
                //textViewPacks.setText("");
                initialiseFMDRequest();
                fmdRes = null;
            }
        });
        // The EMDKManager object will be created and returned in the callback.
        EMDKResults results = EMDKManager.getEMDKManager(
                getApplicationContext(), this);
        // Check the return status of getEMDKManager and update the status Text
        // View accordingly
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            statusTextView.setText("EMDKManager Request Failed");
        }
        initialiseFMDRequest();

        fmdPager = (ViewPager)findViewById(R.id.viewPager);
        fmdPagerAdapter = new FMDSlidePagerAdapter(getSupportFragmentManager());
        fmdPager.setAdapter(fmdPagerAdapter);

    }

    private void initialiseFMDRequest() {
        fmdRequest = new FMDRequest();
        fmdRequest.setStore(store);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onOpened(EMDKManager emdkManager) {
        this.emdkManager = emdkManager;

        // Get the Barcode Manager object
        barcodeManager = (BarcodeManager) this.emdkManager
                .getInstance(EMDKManager.FEATURE_TYPE.BARCODE);
        try {
            initializeScanner();
        } catch (ScannerException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onClosed() {
    // The EMDK closed abruptly. // Clean up the objects created by EMDK manager
        if (this.emdkManager != null) {

            this.emdkManager.release();
            this.emdkManager = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (barcodeManager != null)
            barcodeManager = null;

        if (emdkManager != null) {

            // Clean up the objects created by EMDK manager
            emdkManager.release();
            emdkManager = null;
        }
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        deInitScanner();
    }

    @Override
    public void onData(ScanDataCollection scanDataCollection) {
        // Use the scanned data, process it on background thread using AsyncTask
        // and update the UI thread with the scanned results
        new AsyncDataUpdate(this).execute(scanDataCollection);
    }

    @Override
    public void onStatus(StatusData statusData) {
        // process the scan status event on the background thread using
        // AsyncTask and update the UI thread with current scanner state
        new AsyncStatusUpdate(statusTextView).execute(statusData);
    }

    // Listener for scan button that uses soft scan to scan barcodes through app
    private void addScanButtonListener() {
        Button scanButton = findViewById(R.id.btn_scan);
        // On Touch listener for scan button that scans barcodes when pressed
        // and stops scanning when the button is released
        scanButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Scan Button Press Event
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    try {
                        // Enable Soft scan
                        scanner.triggerType = Scanner.TriggerType.SOFT_ONCE;
                        // cancel any pending reads before reading barcodes
                        if (scanner.isReadPending())
                            scanner.cancelRead();
                        // Puts the device in a state where it can scan barcodes
                        scanner.read();
                    } catch (ScannerException e) {
                        e.printStackTrace();
                    }
                    // Scan Button Release Event
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    try {
                        // cancel any pending reads before reading barcodes
                        if (scanner.isReadPending())
                            scanner.cancelRead();
                    } catch (ScannerException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }
        });
    }

    // Disable the scanner instance
    private void deInitScanner() {
        if (scanner != null) {
            try {
                scanner.cancelRead();
                scanner.removeDataListener(this);
                scanner.removeStatusListener(this);
                scanner.disable();
            } catch (ScannerException e) {
                // TODO Auto-generated catch block
                statusTextView.setText("Status: " + e.getMessage());
            }
            scanner = null;
        }
    }

    // Method to initialize and enable Scanner and its listeners
    private void initializeScanner() throws ScannerException {
        scanner = barcodeManager.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT);

        if (scanner != null) {
            setProfile();
            // Add data and status listeners
            scanner.addDataListener(this);
            scanner.addStatusListener(this);
            try {
                // Enable the scanner
                scanner.enable();
            } catch (ScannerException e) {
                // TODO Auto-generated catch block
                statusTextView.setText("Status: " + e.getMessage());
            }
        }
    }

    private void renderPacks() {
        //textViewPacks.setText("");
        if (fmdRes != null) {
            ArrayList<PackResponse> packResponses = fmdRes.getPackResponses();
            IntStream.range(0, packResponses.size()).forEach(idx -> {
                PackResponse pr = packResponses.get(idx);
                FMDPackInfo p = pr.getPack();
                //textViewPacks.append("Pack: " + idx + "\n");
                //textViewPacks.append("\tGTIN: " + p.getGtin() + "\n");
                //textViewPacks.append("\tSerial Number: " + p.getSerialNumber() + "\n");
                //textViewPacks.append("\tBatch: " + p.getBatch() + "\n");
                //textViewPacks.append("\tExpiry: " + p.getExpiry() + "\n");
                //textViewPacks.append("\tState: " + p.getPackState() + "\n");
                //textViewPacks.append("\tReasons:\n");
                /*
                pr.getReasons().forEach(s -> {
                    textViewPacks.append("\t\t" + s + "\n");
                });
                */
            });
        } else if (fmdRequest != null) {
            ArrayList<FMDBarCode> packs = fmdRequest.getBag().getPacks();
            IntStream.range(0, packs.size()).forEach(idx -> {
                FMDBarCode p = packs.get(idx);
                //textViewPacks.append("Pack: " + idx + "\n");
                //textViewPacks.append("\tGTIN: " + p.getGtin() + "\n");
                //textViewPacks.append("\tSerial Number: " + p.getSerialNumber() + "\n");
                //textViewPacks.append("\tBatch: " + p.getBatch() + "\n");
                //textViewPacks.append("\tExpiry: " + p.getExpiry() + "\n");
            });
        }

    }
    // Sets the user selected Barcode scanning Profile
    public void setProfile() {
        try {
            // cancel any pending asynchronous read calls before applying profile
            // and start reading barcodes
            if (scanner == null) return;
            if (scanner.isReadPending()) {
                scanner.cancelRead();
            }
            ScannerConfig config = scanner.getConfig();

            // Set code11
            config.decoderParams.code11.enabled = true;
            // Set code39
            config.decoderParams.code39.enabled = true;
            // Set code128
            config.decoderParams.code128.enabled = true;
            // set codeUPCA
            config.decoderParams.upca.enabled = true;
            // set EAN8
            config.decoderParams.ean8.enabled = true;
            // set EAN13
            config.decoderParams.ean13.enabled = true;

            // set Illumination Mode, which is available only for
            // INTERNAL_CAMERA1 device type
            config.readerParams.readerSpecific.cameraSpecific.illuminationMode = ScannerConfig.IlluminationMode.OFF;

            // set Vibration Mode (decodeHapticFeedback)
            config.scanParams.decodeHapticFeedback = false;
            // Set the Scan Tone selected from the Scan Tone Spinner
            config.scanParams.audioStreamType = ScannerConfig.AudioStreamType.RINGER;

            // Other selected scan tones from the drop-down
            config.scanParams.decodeAudioFeedbackUri = "system/media/audio/notifications/Vega.ogg";

            scanner.setConfig(config);

            // Starts an asynchronous Scan. The method will not turn
            // ON the
            // scanner. It will, however, put the scanner in a state
            // in which
            // the scanner can be turned ON either by pressing a
            // hardware
            // trigger or can be turned ON automatically.
            scanner.read();
            Toast.makeText(MainActivity.this,
                    "Changes Applied. Press Scan Button to start scanning...",
                    Toast.LENGTH_SHORT).show();

        } catch (ScannerException e) {
            statusTextView.setText(e.toString());
        }
    }

    @Override
    public void setScanResult(ScanResult sr) {
        dataView.getText().clear();
        if (!sr.isBarcodeSupported()){
            dataView.setText(sr.getStatusString());
        }
        if (sr.isValidFMDCode()) {
            FMDBarCode bc = sr.getFmdData();
            if (fmdRequest == null) {
                fmdRequest = new FMDRequest();
            }
            fmdRequest.addPack(bc);
            renderPacks();
        } else if (sr.currentBarcodeIsDataMatrix()) {
            dataView.setText(sr.getStatusString());
        } else if (sr.currentBarcodeIsEAN13()){
            fmdRequest.setBagLabel(sr.getLabelData());
            textViewBagLabel.setText(sr.getLabelData());
        } else{
            dataView.setText(sr.getStatusString());
        }
    }

    private void sendFMDRequest () {
        String url = "http://" + fmdHost + ":8080/camel/fmd";
        String operation = operations.get(radioGroupAction.getCheckedRadioButtonId());
        fmdRequest.setOperation(operation);

        RequestQueueSingleton queue = RequestQueueSingleton.getInstance(this);
        try {
            JSONObject jsonObject = new JSONObject(gson.toJson(fmdRequest));
            System.out.println(jsonObject.toString());
            JsonObjectRequest jobReq = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            fmdRes = gson.fromJson(response.toString(), FMDResponse.class);
                            System.out.println("Successful Request");
                            System.out.println(fmdRes.toString());
                            renderPacks();
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            System.out.println("Request Error");
                            System.out.println(error.toString());
                        }
                    });
            queue.addToRequestQueue(jobReq);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private class FMDSlidePagerAdapter extends SmartFragmentStatePagerAdapter {

        public FMDSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            return fmdPagerAdapter.getRegisteredFragment(position);
        }

        @Override
        public int getCount() {
            return 5;
        }

        // Register the fragment when the item is instantiated
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            registeredFragments.put(position, fragment);
            return fragment;
        }

    }
}
