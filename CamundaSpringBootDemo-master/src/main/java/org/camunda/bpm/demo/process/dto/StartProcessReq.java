package org.camunda.bpm.demo.process.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class StartProcessReq extends VariablesBaseReq{
    @ApiModelProperty(value="流程定义ID")
    private String	processDefId;

    @ApiModelProperty(value="流程定义Key")
    private String	processDefKey;

    @ApiModelProperty(value="启动者")
    private String	starter;

}
