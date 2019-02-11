package com.example.advancedscanning.http.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class FMDRequest {
    private Store store;
    private String operation;
    private PatientBag bag;

    public void addPack(FMDBarCode bc) {
        if (bag == null) {
            bag = new PatientBag();
        }
        bag.addPack(bc);
    }

    public void setBagLabel (String label) {
        if (bag == null) {
            bag = new PatientBag();
        }
        bag.setLabelCode(label);
    }

    public void clearBag () {
        bag = new PatientBag();
    }
}
