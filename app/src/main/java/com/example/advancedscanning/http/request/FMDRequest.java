package com.example.advancedscanning.http.request;

import lombok.Data;

@Data
public class FMDRequest {
    private Store store;
    private String operation;
    private PatientBag bag;
}
