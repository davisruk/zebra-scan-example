package com.example.advancedscanning.http.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FMDRequest {
    private Store store;
    private String operation;
    private PatientBag bag;
}
