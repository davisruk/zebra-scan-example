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
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AsyncUpdateListener, EMDKListener,
        StatusListener, DataListener, CompoundButton.OnCheckedChangeListener {

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
    private CheckBox checkBoxCode11;
    // CheckBox to set Decoder Param Code 39;
    private CheckBox checkBoxCode39;
    // CheckBox to set Decoder Param Code 128;
    private CheckBox checkBoxCode128;
    // CheckBox to set Decoder Param Code UPCA;
    private CheckBox checkBoxCodeUPCA;
    // CheckBox to set Decoder Param EAN 8;
    private CheckBox checkBoxEAN8;
    // CheckBox to set Decoder Param EAN 13;
    private CheckBox checkBoxEAN13;
    // CheckBox to set Reader Param Illumination Mode;
    private CheckBox checkBoxIlluminationMode;
    // CheckBox to set Scan Param Vibration Mode (decodeHapticFeedback);
    private CheckBox checkBoxVibrationMode;
    // Drop Down for selecting scanner devices
    private Spinner deviceSelectionSpinner;
    // Drop Down for selecting the type of streaming on which the scan beep should be played
    private Spinner scanToneSpinner;
    // Array Adapter to hold arrays that are used in various drop downs
    private ArrayAdapter<String> spinnerDataAdapter;
    // List of supported scanner devices
    private List<ScannerInfo> deviceList;
    // Provides current scanner index in the device Selection Spinner
    private int scannerIndex = 0;
    // Boolean to avoid calling setProfile() method again in the scan tone listener
    private boolean isScanToneFirstTime;

    private CheckBox checkBoxDecommission;

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
        checkBoxCode11 = findViewById(R.id.checkBoxCode11);
        checkBoxCode39 = findViewById(R.id.checkBoxCode39);
        checkBoxCode128 = findViewById(R.id.checkBoxCode128);
        checkBoxCodeUPCA = findViewById(R.id.checkBoxUPCA);
        checkBoxEAN8 = findViewById(R.id.checkBoxEan8);
        checkBoxEAN13 = findViewById(R.id.checkBoxEan13);

        checkBoxDecommission = findViewById(R.id.checkBoxDecommission);

        checkBoxIlluminationMode = findViewById(R.id.illumination);
        checkBoxVibrationMode = findViewById(R.id.vibration);

        checkBoxCode11.setOnCheckedChangeListener(this);
        checkBoxCode39.setOnCheckedChangeListener(this);
        checkBoxCode128.setOnCheckedChangeListener(this);
        checkBoxCodeUPCA.setOnCheckedChangeListener(this);
        checkBoxEAN8.setOnCheckedChangeListener(this);
        checkBoxEAN13.setOnCheckedChangeListener(this);
        checkBoxIlluminationMode.setOnCheckedChangeListener(this);
        checkBoxVibrationMode.setOnCheckedChangeListener(this);

        checkBoxDecommission.setOnCheckedChangeListener(this);

        deviceSelectionSpinner = findViewById(R.id.device_selection_spinner);
        scanToneSpinner = findViewById(R.id.scan_tone_spinner);
        // Adapter to hold the list of scan tone options
        spinnerDataAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources()
                .getStringArray(R.array.scan_tone_array));
        spinnerDataAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Set adapter to scan tone drop down
        scanToneSpinner.setAdapter(spinnerDataAdapter);

        // Add onClick listener for scan button to enable soft scan through app
        addScanButtonListener();

        // On Item Click Listener of Scanner Devices Spinner
        addSpinnerScannerDevicesListener();

        // On Item Click Listener of Scan Tone Spinner
        addSpinnerScanToneListener();

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

        // Get the supported scanner devices
        enumerateScannerDevices();
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

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setProfile();
    }

    // Listener for Scanner Device Spinner
    private void addSpinnerScannerDevicesListener() {

        deviceSelectionSpinner
                .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> parent,
                                               View arg1, int position, long arg3) {

                        scannerIndex = position;
                        try {
                            deInitScanner();
                            initializeScanner();
                            setProfile();
                        } catch (ScannerException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                        // TODO Auto-generated method stub
                    }
                });


    }

    // Listener for Scan Tone Spinner
    private void addSpinnerScanToneListener() {
        scanToneSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View arg1,
                                       int position, long arg3) {
                // Ignore Scan Tone spinner firing of for the first time, which
                // is not required
                if (isScanToneFirstTime)
                    setProfile();
                else
                    isScanToneFirstTime = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
            }
        });
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
        if (deviceList.size() != 0) {
            scanner = barcodeManager.getDevice(deviceList.get(scannerIndex));
        } else {
            statusTextView
                    .setText("Status: "
                            + "Failed to get the specified scanner device! Please close and restart the application.");
        }

        if (scanner != null) {
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
            if (scanner.isReadPending()) {
                scanner.cancelRead();
            }
            ScannerConfig config = scanner.getConfig();

            // Set code11
            config.decoderParams.code11.enabled = checkBoxCode11.isChecked();
            // Set code39
            config.decoderParams.code39.enabled = checkBoxCode39.isChecked();
            // Set code128
            config.decoderParams.code128.enabled = checkBoxCode128.isChecked();
            // set codeUPCA
            config.decoderParams.upca.enabled = checkBoxCodeUPCA.isChecked();
            // set EAN8
            config.decoderParams.ean8.enabled = checkBoxEAN8.isChecked();
            // set EAN13
            config.decoderParams.ean13.enabled = checkBoxEAN13.isChecked();

            // set Illumination Mode, which is available only for
            // INTERNAL_CAMERA1 device type
            if (checkBoxIlluminationMode.isChecked()
                    && deviceSelectionSpinner.getSelectedItem().toString()
                    .contains("Camera")) {
                config.readerParams.readerSpecific.cameraSpecific.illuminationMode = ScannerConfig.IlluminationMode.ON;
            } else {
                config.readerParams.readerSpecific.cameraSpecific.illuminationMode = ScannerConfig.IlluminationMode.OFF;
            }

            // set Vibration Mode (decodeHapticFeedback)
            config.scanParams.decodeHapticFeedback = checkBoxVibrationMode.isChecked();
            // Set the Scan Tone selected from the Scan Tone Spinner
            config.scanParams.audioStreamType = ScannerConfig.AudioStreamType.RINGER;
            String scanTone = scanToneSpinner.getSelectedItem().toString();
            if (scanTone.contains("NONE")) {
                // Silent Mode (No scan tone will be played)
                config.scanParams.decodeAudioFeedbackUri = "";
            }
            else {
                // Other selected scan tones from the drop-down
                config.scanParams.decodeAudioFeedbackUri = "system/media/audio/notifications/"
                        + scanTone;
            }
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

    // Go through and get the available scanner devices
    private void enumerateScannerDevices() {
        if (barcodeManager != null) {
            List<String> friendlyNameList = new ArrayList<String>();
            int spinnerIndex = 0;
            // Set the default selection in the spinner
            int defaultIndex = 0;
            deviceList = barcodeManager.getSupportedDevicesInfo();

            if (deviceList.size() != 0) {
                Iterator<ScannerInfo> it = deviceList.iterator();
                while (it.hasNext()) {
                    ScannerInfo scnInfo = it.next();
                    friendlyNameList.add(scnInfo.getFriendlyName());
                    if (scnInfo.isDefaultScanner()) {
                        defaultIndex = spinnerIndex;
                    }
                    ++spinnerIndex;
                }
            } else {
                statusTextView
                        .setText("Status: "
                                + "Failed to get the list of supported scanner devices! Please close and restart the application.");
            }

            spinnerDataAdapter = new ArrayAdapter<String>(MainActivity.this,
                    android.R.layout.simple_spinner_item, friendlyNameList);
            spinnerDataAdapter
                    .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            deviceSelectionSpinner.setAdapter(spinnerDataAdapter);
            deviceSelectionSpinner.setSelection(defaultIndex);
        }
    }

    @Override
    public void setFMDBarcodeData(FMDBarCode bc) {
        dataView.getText().clear();
        dataView.append(bc.toString() + "\n");
        sendFMDRequest(bc);
    }

    private void sendFMDRequest (FMDBarCode bc) {
        String url = "http://192.168.1.205:8080/camel/fmd";
        ArrayList packs = new ArrayList<FMDBarCode>();
        packs.add(bc);
        FMDRequest fmdReq = FMDRequest.builder()
                .operation(checkBoxDecommission.isChecked() ? "undo-dispense" : "dispense")
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
