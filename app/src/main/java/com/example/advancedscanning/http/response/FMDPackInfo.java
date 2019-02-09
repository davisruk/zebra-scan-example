package com.example.advancedscanning.http.response;

import lombok.Data;

@Data
public class FMDPackInfo {
    private String gtin;
    private String batch;
    private String serialNumber;
    private String expiry;
    private boolean decommissioned;
    private String packState;
}
