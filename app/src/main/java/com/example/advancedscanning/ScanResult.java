package com.example.advancedscanning;

import com.example.advancedscanning.http.request.FMDBarCode;
import com.symbol.emdk.barcode.ScanDataCollection;

import lombok.Data;

@Data
public class ScanResult {

    private ScanDataCollection.LabelType barcodeType;
    private FMDBarCode fmdData;
    private String labelData;
    private boolean barcodeSupported;
    private String statusString;

    public boolean currentBarcodeIsDataMatrix() {
        return barcodeType.compareTo(ScanDataCollection.LabelType.DATAMATRIX) == 0;
    }

    public boolean currentBarcodeIsEAN13() {
        return barcodeType.compareTo(ScanDataCollection.LabelType.EAN13) == 0;
    }

    public boolean isValidFMDCode(){
        return currentBarcodeIsDataMatrix() && fmdData.isValid();
    }
}
