package com.example.advancedscanning;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
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
import com.example.advancedscanning.http.AsyncUpdateListener;
import com.example.advancedscanning.http.request.FMDBarCode;
import com.example.advancedscanning.http.request.FMDRequest;
import com.example.advancedscanning.http.request.PatientBag;
import com.example.advancedscanning.http.request.RequestQueueSingleton;
import com.example.advancedscanning.http.request.Store;
import com.example.advancedscanning.http.response.FMDResponse;
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
    // CheckBox to set Decoder Param Code 11;
    private boolean isScanToneFirstTime;

    private Map<Integer, String> operations = new HashMap<Integer, String>();

    private RadioGroup radioGroupAction;

    private Gson gson;
    private FMDRequest fmdRequest;
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

        gson = new GsonBuilder().create();
        // Reference to UI elements
        statusTextView = findViewById(R.id.textViewStatus);
        dataView = findViewById(R.id.editText1);

        RadioButton radioUndispense = findViewById(R.id.radioUndispense);
        RadioButton radioVerify = findViewById(R.id.radioVerify);
        RadioButton radioDispense = findViewById(R.id.radioDispense);
        radioGroupAction = findViewById(R.id.actionGroup);

        operations.put(radioVerify.getId(), "verify");
        operations.put(radioUndispense.getId(), "undo-dispense");
        operations.put(radioDispense.getId(), "dispense");

        // Add onClick listener for scan button to enable soft scan through app
        addScanButtonListener();

        // The EMDKManager object will be created and returned in the callback.
        EMDKResults results = EMDKManager.getEMDKManager(
                getApplicationContext(), this);
        // Check the return status of getEMDKManager and update the status Text
        // View accordingly
        if (results.statusCode != EMDKResults.STATUS_CODE.SUCCESS) {
            statusTextView.setText("EMDKManager Request Failed");
        }
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
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
                    "Changes Appplied. Press Scan Button to start scanning...",
                    Toast.LENGTH_SHORT).show();

        } catch (ScannerException e) {
            statusTextView.setText(e.toString());
        }
    }

    @Override
    public void setFMDBarcodeData(FMDBarCode bc) {
        dataView.getText().clear();
        dataView.append(bc.toString() + "\n");
        if (bc.isValid()) {
            sendFMDRequest(bc);
        }
        else {
            // NOT WORKING - needs changing anyway
            // AsyncDataUpdate should return its own class
            // Barcode data could be a 1D bag label or a 2D FMD Code
            statusTextView.setText("Invalid FMD Code");
            statusTextView.append(bc.toString());
        }
    }

    private void sendFMDRequest (FMDBarCode bc) {
        String url = "http://10.7.37.70:8080/camel/fmd";
        //String url = "http://192.168.1.205:8080/camel/fmd";
        ArrayList packs = new ArrayList<FMDBarCode>();
        String operation = operations.get(radioGroupAction.getCheckedRadioButtonId());
        packs.add(bc);
        FMDRequest fmdReq = FMDRequest.builder()
                .operation(operation)
                .store(store)
                .bag(PatientBag.builder()
                        .labelCode("123456")
                        .packs(packs)
                        .build())
                .build();

        RequestQueueSingleton queue = RequestQueueSingleton.getInstance(this);
        try {
            JSONObject jsonObject = new JSONObject(gson.toJson(fmdReq));
            System.out.println(jsonObject.toString());
            JsonObjectRequest jobReq = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            FMDResponse fmdRes = gson.fromJson(response.toString(), FMDResponse.class);
                            System.out.println("Successful Request");
                            System.out.println(fmdRes.toString());
                            dataView.getText().clear();
                            dataView.append(fmdRes.toString() + "\n");
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
}
