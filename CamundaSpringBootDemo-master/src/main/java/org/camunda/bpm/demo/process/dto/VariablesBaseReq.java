package org.camunda.bpm.demo.process.dto;

import lombok.Data;
import java.util.Map;

@Data
public class VariablesBaseReq {
    private Map<String,Object> variables;
}
