package com.example.advancedscanning.http.request;

import java.util.ArrayList;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PatientBag {
    private String labelCode;
    private ArrayList<FMDBarCode> packs;
}
