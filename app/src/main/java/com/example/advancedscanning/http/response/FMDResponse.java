package com.example.advancedscanning.http.response;

import java.util.ArrayList;

import lombok.Data;

@Data
public class FMDResponse {
    private int bagId;
    private String bagLabel;
    private ArrayList<PackResponse> packResponses;
}
