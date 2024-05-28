package com.tjn.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Sensor {
    private Integer id;
    private Boolean state;
    private String forestName;
}
