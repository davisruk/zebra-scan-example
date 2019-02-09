package com.example.advancedscanning.http.response;

import java.util.ArrayList;

import lombok.Data;

@Data
public class PackResponse {
    private String code;
    private String description;
    private ArrayList<String> reasons;
    private FMDPackInfo pack;
}
