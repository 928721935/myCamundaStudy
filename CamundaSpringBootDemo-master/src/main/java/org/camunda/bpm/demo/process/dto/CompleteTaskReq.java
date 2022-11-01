package org.camunda.bpm.demo.process.dto;

import lombok.Data;

@Data
public class CompleteTaskReq extends VariablesBaseReq {
    private String taskId;
    private String instanceId;
    private String operator;
    private String result;
}
