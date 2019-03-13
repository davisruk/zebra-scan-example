package com.example.advancedscanning.fmdslider.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class FMDSliderPageModel {
    private int totalPages;
    private int currentPage;
    private String pageIndexInfo;
    private String lorumIpsum;

    private String getPageIndexInfo() {
        return currentPage + " of " + totalPages;
    }
}
