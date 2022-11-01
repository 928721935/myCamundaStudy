package org.camunda.bpm.demo.process.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.demo.process.controller.MyController;
import org.camunda.bpm.demo.process.dto.CompleteTaskReq;
import org.camunda.bpm.demo.process.dto.DeployProcessReq;
import org.camunda.bpm.demo.process.dto.StartProcessReq;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.rest.dto.task.TaskDto;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

@Slf4j
@Service
public class MyService implements MyController {
    @Autowired
    private TaskService taskService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private HistoryService historyService;

    public void completeTask(CompleteTaskReq req) {
        Map<String, Object> variables = new HashMap<>();
        Optional.ofNullable(req.getVariables()).ifPresent(o->variables.putAll(o));
        variables.put("operator1", req.getOperator());
        if (req.getInstanceId()!=null){
            runtimeService.setVariables(req.getInstanceId(), variables);
        }
        taskService.complete(req.getTaskId(),variables);
    }

    @SneakyThrows
    public String deployProcess(DeployProcessReq req) {
        File file = new File(req.getFilePath());
        FileInputStream is = new FileInputStream(file);
//这种就是加载项目resources目录下的one.bpmn文件
        Deployment deploy = repositoryService.createDeployment()
                .name(req.getProcessName())
//                .addClasspathResource("one.bpmn")
                .addInputStream(file.getName(), is)
                .deploy();
        return deploy.getId();
//这种就可以使用自己拼接的xml字符串
//        Deployment deploy = repositoryService.createDeployment()
//                .name(definitionName)
//                .addString(名称+".bpmn",resource)
//                .deploy();
    }

    public String startProcess(StartProcessReq req) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("starter", req.getStarter());
        Optional.ofNullable(req.getVariables()).ifPresent(o->variables.putAll(o));
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(req.getProcessDefKey(),
                variables);
        return processInstance.getId();
    }

    @Override
    public List<TaskDto> getRunningTaskByInstatnceId(String id) {
        List<TaskDto> resultList = new ArrayList<TaskDto>();
        List<Task> taskList = taskService.createTaskQuery().processInstanceId(id).list();
        for (Task task : taskList) {
            TaskDto dto = new TaskDto();
            dto = TaskDto.fromEntity(task);
            resultList.add(dto);
        }
        return resultList;
    }

    @Override
    public List<HistoricTaskInstance> getHistoryTaskByInstatnceId(String id) {
        List<HistoricTaskInstance> resultList = new ArrayList<HistoricTaskInstance>();
        resultList = historyService.createHistoricTaskInstanceQuery().processInstanceId(id).list();
        return resultList;
    }
}
