package com.example.advancedscanning.http.request;

import java.util.ArrayList;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class PatientBag {
    private String labelCode;
    private ArrayList<FMDBarCode> packs;

    public void addPack (FMDBarCode pack) {
        if (packs == null) {
            packs = new ArrayList<FMDBarCode>();
        }
        packs.add(pack);
    }
}
