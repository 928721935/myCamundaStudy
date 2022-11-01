package org.camunda.bpm.demo.process.dto;

import lombok.Data;

@Data
public class DeployProcessReq extends VariablesBaseReq {
    private String processName;
    private String filePath;
}
