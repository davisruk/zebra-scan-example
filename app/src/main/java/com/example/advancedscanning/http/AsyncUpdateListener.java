package com.example.advancedscanning.http;

import com.example.advancedscanning.http.request.FMDBarCode;

public interface AsyncUpdateListener {
    public void setFMDBarcodeData (FMDBarCode bc);
}
