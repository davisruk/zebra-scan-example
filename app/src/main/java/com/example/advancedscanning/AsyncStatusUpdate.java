package com.example.advancedscanning;

import android.os.AsyncTask;
import android.widget.TextView;

import com.symbol.emdk.barcode.StatusData;

import lombok.AllArgsConstructor;

// AsyncTask that configures the current state of scanner on background
// thread and updates the result on UI thread
@AllArgsConstructor
class AsyncStatusUpdate  extends AsyncTask<StatusData, Void, String> {
    private TextView statusTextView;
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
