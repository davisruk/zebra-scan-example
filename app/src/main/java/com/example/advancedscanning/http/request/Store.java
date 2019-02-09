package com.example.advancedscanning.http.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Store {
    private String id;
    private String name;
}
