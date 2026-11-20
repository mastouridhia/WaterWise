package com.waterwise.app.network.model;

import java.util.List;

/** JSON answer of GET species-list: { "data": [ ... ], "total": 13, ... } */
public class SpeciesListResponse {
    public List<SpeciesDto> data;
    public int total;
}
