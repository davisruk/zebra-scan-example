package com.example.advancedscanning;

import android.os.AsyncTask;

import com.example.advancedscanning.http.AsyncUpdateListener;
import com.example.advancedscanning.http.request.FMDBarCode;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.ScannerResults;

import java.util.ArrayList;

// AsyncTask that configures the scanned data on background
// thread and updated the result on UI thread with scanned data and type of
// label

class AsyncDataUpdate extends
        AsyncTask<ScanDataCollection, Void, String> {
    private AsyncUpdateListener listener;
    private ScanResult sr;

    public AsyncDataUpdate (AsyncUpdateListener listener){
        this.listener = listener;
    }

    @Override
    protected String doInBackground(ScanDataCollection... params) {
        // create new ScanResult
        sr = new ScanResult();
        ScanDataCollection scanDataCollection = params[0];

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
                if (labelType.equals(ScanDataCollection.LabelType.EAN13)){
                    sr.setStatusString("EAN13 Scanned");
                    sr.setLabelData(barcodeData);
                    sr.setBarcodeType(ScanDataCollection.LabelType.EAN13);
                } else if (labelType.equals(ScanDataCollection.LabelType.DATAMATRIX)){
                    sr.setBarcodeType(ScanDataCollection.LabelType.DATAMATRIX);
                    sr.setFmdData(FMDBarCode.buildFromGS1Data(barcodeData));
                    if (sr.isValidFMDCode()) {
                        sr.setStatusString("Data Matrix Scanned");
                    } else {
                        sr.setStatusString("Non FMD Data Matrix Scanned. Data: " + barcodeData);
                    }


                } else {
                    sr.setStatusString("Non FMD App Barcode Scanned. Data: " + barcodeData);
                    sr.setBarcodeSupported(false);
                }
            }
        }
        // Return result to populate on UI thread
        return sr.getStatusString();
    }

    @Override
    protected void onPostExecute(String result) {
        listener.setScanResult(sr);
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(Void... values) {
    }
}