package com.example.advancedscanning;

import android.os.AsyncTask;
import android.widget.EditText;

import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.ScannerResults;

import java.util.ArrayList;

import lombok.AllArgsConstructor;

// AsyncTask that configures the scanned data on background
// thread and updated the result on UI thread with scanned data and type of
// label
@AllArgsConstructor
class AsyncDataUpdate extends
        AsyncTask<ScanDataCollection, Void, String> {
    private EditText dataView;
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
                String barcodeData = data.getData();
                // Get the type of label being scanned
                ScanDataCollection.LabelType labelType = data.getLabelType();
                // Concatenate barcode data and label type
                if (!labelType.equals(ScanDataCollection.LabelType.DATAMATRIX)){
                    statusStr = "Not FMD Barcode. Type is " + labelType + ", data is: " + barcodeData;
                } else {
                    FMDBarCode fmd = FMDBarCode.buildFromGS1Data(barcodeData);
                    // barcode is a 2D matrix but still need to check if string was an FMD code
                    if (fmd.isValid()) {
                        statusStr = fmd.toString();
                    } else {
                        statusStr = "Not FMD Barcode. Type is " + labelType + ", data is: " + barcodeData;
                    }
                }
            }
        }
        // Return result to populate on UI thread
        return statusStr;
    }

    @Override
    protected void onPostExecute(String result) {
        dataView.getText().clear();
        dataView.append(result + "\n");
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(Void... values) {
    }
}