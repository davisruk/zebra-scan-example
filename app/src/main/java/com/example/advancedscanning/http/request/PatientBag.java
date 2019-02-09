package com.example.advancedscanning.http.request;

import java.util.ArrayList;

import lombok.Data;

@Data
class PatientBag {
    private String labelCode;
    private ArrayList<FMDBarCode> packs;
}
