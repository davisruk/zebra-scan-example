package com.example.advancedscanning;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
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
import com.symbol.emdk.barcode.ScannerResults;
import com.symbol.emdk.barcode.StatusData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements EMDKListener,
        StatusListener, DataListener, CompoundButton.OnCheckedChangeListener {

    // Update the scan data on UI
    int dataLength = 0;
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
    // Drop Down for selecting the type of streaming on which the scan beep
// should
// be played
    private Spinner scanToneSpinner;
    // Array Adapter to hold arrays that are used in various drop downs
    private ArrayAdapter<String> spinnerDataAdapter;
    // List of supported scanner devices
    private List<ScannerInfo> deviceList;
    // Provides current scanner index in the device Selection Spinner
    private int scannerIndex = 0;
    // Boolean to avoid calling setProfile() method again in the scan tone
// listener
    private boolean isScanToneFirstTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Reference to UI elements
        statusTextView = findViewById(R.id.textViewStatus);
        dataView = findViewById(R.id.editText1);
        checkBoxCode11 = findViewById(R.id.checkBoxCode11);
        checkBoxCode39 = findViewById(R.id.checkBoxCode39);
        checkBoxCode128 = findViewById(R.id.checkBoxCode128);
        checkBoxCodeUPCA = findViewById(R.id.checkBoxUPCA);
        checkBoxEAN8 = findViewById(R.id.checkBoxEan8);
        checkBoxEAN13 = findViewById(R.id.checkBoxEan13);

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
// The EMDK closed abruptly. // Clean up the objects created by EMDK
// manager
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
        new AsyncDataUpdate().execute(scanDataCollection);
    }

    @Override
    public void onStatus(StatusData statusData) {
        // process the scan status event on the background thread using
        // AsyncTask and update the UI thread with current scanner state
        new AsyncStatusUpdate().execute(statusData);
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
            if (scanner.isReadPending())
                scanner.cancelRead();

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
            if (scanTone.contains("NONE"))
                // Silent Mode (No scan tone will be played)
                config.scanParams.decodeAudioFeedbackUri = "";
            else
                // Other selected scan tones from the drop-down
                config.scanParams.decodeAudioFeedbackUri = "system/media/audio/notifications/"
                        + scanTone;

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

    // AsyncTask that configures the scanned data on background
    // thread and updated the result on UI thread with scanned data and type of
    // label
    private class AsyncDataUpdate extends
            AsyncTask<ScanDataCollection, Void, String> {

        @Override
        protected String doInBackground(ScanDataCollection... params) {
            ScanDataCollection scanDataCollection = params[0];

            // Status string that contains both barcode data and type of barcode
            // that is being scanned
            String statusStr = "";

            // The ScanDataCollection object gives scanning result and the
            // collection of ScanData. So check the data and its status
            if (scanDataCollection != null
                    && scanDataCollection.getResult() == ScannerResults.SUCCESS) {

                ArrayList<ScanDataCollection.ScanData> scanData = scanDataCollection.getScanData();

                // Iterate through scanned data and prepare the statusStr
                for (ScanDataCollection.ScanData data : scanData) {
                    // Get the scanned data
                    String barcodeDate = data.getData();
                    // Get the type of label being scanned
                    ScanDataCollection.LabelType labelType = data.getLabelType();
                    // Concatenate barcode data and label type
                    statusStr = barcodeDate + " " + labelType;
                    statusStr = FMDBarCode.buildFromGS1Data(barcodeDate).toString();
                }
            }

            // Return result to populate on UI thread

            return statusStr;


        }

        @Override
        protected void onPostExecute(String result) {
            // Update the dataView EditText on UI thread with barcode data and
            // its label type
//            if (dataLength++ > 1) {
                // Clear the cache after 1 scan
                dataView.getText().clear();
//                dataLength = 0;
//            }
            dataView.append(result + "\n");
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    // AsyncTask that configures the current state of scanner on background
    // thread and updates the result on UI thread
    private class AsyncStatusUpdate extends AsyncTask<StatusData, Void, String> {

        @Override
        protected String doInBackground(StatusData... params) {
            // Get the current state of scanner in background
            StatusData statusData = params[0];
            String statusStr = "";
            StatusData.ScannerStates state = statusData.getState();
            // Different states of Scanner
            switch (state) {
                // Scanner is IDLE
                case IDLE:
                    statusStr = "The scanner enabled and its idle";
                    break;
                // Scanner is SCANNING
                case SCANNING:
                    statusStr = "Scanning..";
                    break;
                // Scanner is waiting for trigger press
                case WAITING:
                    statusStr = "Waiting for trigger press..";
                    break;
                // Scanner is not enabled
                case DISABLED:
                    statusStr = "Scanner is not enabled";
                    break;
                default:
                    break;
            }
            // Return result to populate on UI thread
            return statusStr;
        }

        @Override
        protected void onPostExecute(String result) {
            // Update the status text view on UI thread with current scanner
            // state
            statusTextView.setText(result);
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}
