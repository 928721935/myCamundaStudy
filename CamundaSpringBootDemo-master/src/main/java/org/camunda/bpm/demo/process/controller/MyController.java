package org.camunda.bpm.demo.process.controller;

import io.swagger.annotations.ApiOperation;
import org.camunda.bpm.demo.process.dto.CompleteTaskReq;
import org.camunda.bpm.demo.process.dto.DeployProcessReq;
import org.camunda.bpm.demo.process.dto.StartProcessReq;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.rest.dto.task.TaskDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/my")
public interface MyController {
//    @Resource
//    MyService myService;

    @ApiOperation(value = "审批", notes = "审批")
    @RequestMapping(value = "/completeTask", method = RequestMethod.POST)
    public void completeTask(@RequestBody CompleteTaskReq req);

    @ApiOperation(value = "发布流程", notes = "发布流程")
    @ResponseBody
    @RequestMapping(value = "/deployProcess", method = RequestMethod.POST)
    public String deployProcess(@RequestBody DeployProcessReq req);

    @ApiOperation(value = "发起流程", notes = "发起流程")
    @RequestMapping(value = "/startProcess", method = RequestMethod.POST)
    public String startProcess(@RequestBody StartProcessReq req);

    @ApiOperation(value = "根据实例id查询当前任务", notes = "根据实例id查询当前任务")
    @RequestMapping(value = "/getRunningTaskByInstatnceId/{id}", method = RequestMethod.POST)
    public List<TaskDto> getRunningTaskByInstatnceId(@PathVariable String id);

    @ApiOperation(value = "根据实例id查询历史任务", notes = "根据实例id查询历史任务")
    @RequestMapping(value = "/getHistoryTaskByInstatnceId/{id}", method = RequestMethod.POST)
    public List<HistoricTaskInstance> getHistoryTaskByInstatnceId(@PathVariable String id);
}
