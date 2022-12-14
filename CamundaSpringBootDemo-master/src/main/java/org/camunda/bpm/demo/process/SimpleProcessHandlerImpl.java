package org.camunda.bpm.demo.process;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.impl.persistence.entity.TaskEntity;
import org.camunda.bpm.engine.rest.dto.task.TaskDto;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.webapp.impl.security.auth.AuthenticationService;
import org.camunda.bpm.webapp.impl.security.auth.Authentications;
import org.camunda.bpm.webapp.impl.security.auth.UserAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SimpleProcessHandlerImpl implements SimpleProcessHandler {
	private static final Logger logger = LoggerFactory.getLogger(SimpleProcessHandlerImpl.class);

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private HistoryService historyService;
	

	// @Context
	// protected HttpServletRequest request;

	@Autowired
	private IdentityService identityService;

	@Override
	public List<TaskDto> simpleInitProcess(PscCommonProcessRequest pscCommonProcessRequest) throws Exception {
		String processInstanceId = null;
		List<TaskDto> resultList = new ArrayList<TaskDto>();
		Map<String, Object> variables = new HashMap<String, Object>();
		variables = pscCommonProcessRequest.getVariables();
		// variables.put("assigneeList030", Arrays.asList("kermit", "demo"));
		// variables.put("assigneeList040", Arrays.asList("kermit", "demo"));
		variables.put("starter", pscCommonProcessRequest.getStarter());
		variables.put("amount", "980");
		// ???????????????
		ProcessInstance processInstance = null;
		// ???????????????
		if (StringUtils.isNotBlank(pscCommonProcessRequest.getProcessDefKey())) {
			processInstance = runtimeService.startProcessInstanceByKey(pscCommonProcessRequest.getProcessDefKey(),
					variables);
		} else {
			processInstance = runtimeService.startProcessInstanceById(pscCommonProcessRequest.getProcessDefId(),
					variables);
		}
		// ????????????
		if (processInstance != null && StringUtils.isNotBlank(processInstance.getId())) {
			processInstanceId = processInstance.getId();
			resultList = simpleGetTasks(processInstanceId);
		} else {
			throw new Exception("???????????????????????????");
		}
		return resultList;
	}

	@Override
	public List<TaskDto> simpleStartProcess(PscCommonProcessRequest pscCommonProcessRequest, HttpServletRequest request)
			throws Exception {
		AuthenticationService authenticationService = new AuthenticationService();
		UserAuthentication authentication = (UserAuthentication) authenticationService.createAuthenticate("default",
				pscCommonProcessRequest.getStarter(), null, null);
		logger.info("authentication--------->" + authentication.getName());
		Authentications.revalidateSession(request, authentication);
		String processInstanceId = null;
		List<TaskDto> resultList = new ArrayList<TaskDto>();
		Map<String, Object> variables = new HashMap<String, Object>();
		variables = pscCommonProcessRequest.getVariables();
		// variables.put("assigneeList030", Arrays.asList("kermit", "demo"));
		// variables.put("assigneeList040", Arrays.asList("kermit", "demo"));
		variables.put("starter", pscCommonProcessRequest.getStarter());
		variables.put("amount", "980");
		ProcessInstance processInstance = null;
		// ???????????????
		if (StringUtils.isNotBlank(pscCommonProcessRequest.getProcessDefKey())) {
			processInstance = runtimeService.startProcessInstanceByKey(pscCommonProcessRequest.getProcessDefKey(),
					variables);
		} else {
			processInstance = runtimeService.startProcessInstanceById(pscCommonProcessRequest.getProcessDefId(),
					variables);
		}
		// ????????????
		if (processInstance != null && StringUtils.isNotBlank(processInstance.getId())) {
			processInstanceId = processInstance.getId();
			List<TaskDto> taskList = simpleGetTasks(processInstanceId);
			logger.info(JSON.toJSONString(taskList));
			if (taskList != null && taskList.size() == 1) {
				taskService.complete(taskList.get(0).getId(), variables);
				taskService.createComment(taskList.get(0).getId(), processInstanceId, "????????????");
				resultList = simpleGetTasks(processInstanceId);
			} else {
				throw new Exception("???????????????????????????" + taskList.size());
			}
		} else {
			throw new Exception("???????????????????????????");
		}
		return resultList;
	}

	@Override
	public List<HistoricTaskInstance> simpleGetHisTasks(String processDefKey) throws Exception {
		List<HistoricTaskInstance> resultList = new ArrayList<HistoricTaskInstance>();
		resultList = historyService.createHistoricTaskInstanceQuery().processDefinitionKey(processDefKey).list();
		return resultList;
	}

	@Override
	public List<TaskDto> simpleGetTaskIds(String processDefKey) throws Exception {
		List<TaskDto> resultList = new ArrayList<TaskDto>();
		List<Task> taskList = taskService.createTaskQuery().processDefinitionKey(processDefKey).list();
		for (Task task : taskList) {
			TaskDto dto = new TaskDto();
			dto = TaskDto.fromEntity(task);
			resultList.add(dto);
		}
		return resultList;
	}

	@Override
	public List<TaskDto> simpleApproveProcess(PscCommonTaskRequest pscCommonTaskRequest, HttpServletRequest request)
			throws Exception {
		AuthenticationService authenticationService = new AuthenticationService();
		UserAuthentication authentication = (UserAuthentication) authenticationService.createAuthenticate("default",
				pscCommonTaskRequest.getUserId(), null, null);
		logger.info("authentication--------->" + authentication.getName());
		Authentications.revalidateSession(request, authentication);
		List<TaskDto> taskList = new ArrayList<TaskDto>();
		Map<String, Object> variables = new HashMap<String, Object>();
		variables = pscCommonTaskRequest.getVariables();
		Map<String, Object> localVariables = new HashMap<String, Object>();
		localVariables = pscCommonTaskRequest.getLocalVariables();

		runtimeService.setVariables(pscCommonTaskRequest.getProcessInstId(), localVariables);
		if(StringUtils.isNoneBlank(pscCommonTaskRequest.getToActId())){
			taskService.complete(pscCommonTaskRequest.getTaskId(), variables);
			taskService.createComment(pscCommonTaskRequest.getTaskId(), pscCommonTaskRequest.getProcessInstId(), "??????????????????");
			ActivityInstance tree = runtimeService.getActivityInstance(pscCommonTaskRequest.getProcessInstId());
			runtimeService
				.createProcessInstanceModification(pscCommonTaskRequest.getProcessInstId())
				.cancelActivityInstance(getInstanceIdForActivity(tree, tree.getActivityId()))
				.startBeforeActivity(pscCommonTaskRequest.getToActId())
				.execute();
		}else{
			taskService.createComment(pscCommonTaskRequest.getTaskId(), pscCommonTaskRequest.getProcessInstId(), "????????????");
			taskService.complete(pscCommonTaskRequest.getTaskId(), variables);
			
		}
		
		taskList = simpleGetTasks(pscCommonTaskRequest.getProcessInstId());
		if (taskList != null && taskList.size() == 1) {
			taskService.setAssignee(taskList.get(0).getId(), pscCommonTaskRequest.getNextUserId());
		}
		taskList = simpleGetTasks(pscCommonTaskRequest.getProcessInstId());
		return taskList;
	}

	@Override
	public List<TaskDto> simpleUndoProcess(PscCommonTaskRequest pscCommonTaskRequest, HttpServletRequest request)
			throws Exception {
		AuthenticationService authenticationService = new AuthenticationService();
		UserAuthentication authentication = (UserAuthentication) authenticationService.createAuthenticate("default",
				pscCommonTaskRequest.getUserId(), null, null);
		logger.info("authentication--------->" + authentication.getName());
		Authentications.revalidateSession(request, authentication);
		List<TaskDto> taskList = new ArrayList<TaskDto>();

		ActivityInstance tree = runtimeService.getActivityInstance(pscCommonTaskRequest.getProcessInstId());
		taskService.createComment(pscCommonTaskRequest.getTaskId(), pscCommonTaskRequest.getProcessInstId(), "????????????");
		runtimeService
	      .createProcessInstanceModification(pscCommonTaskRequest.getProcessInstId())
	      .cancelActivityInstance(getInstanceIdForActivity(tree, tree.getActivityId()))
	      .startBeforeActivity(pscCommonTaskRequest.getTaskDefKey())
	      .execute();
		
		taskList = simpleGetTasks(pscCommonTaskRequest.getProcessInstId());
//		if (taskList != null && taskList.size() == 1) {
//			taskService.setAssignee(taskList.get(0).getId(), pscCommonTaskRequest.getNextUserId());
//		}
//		taskList = simpleGetTasks(pscCommonTaskRequest.getProcessInstId());
		return taskList;
	}
	
	@Override
	public List<TaskDto> simpleRollbackProcess(PscCommonTaskRequest pscCommonTaskRequest, HttpServletRequest request)
			throws Exception {
		String rejectType = pscCommonTaskRequest.getRejectType();
		if(StringUtils.isBlank(rejectType)){
			throw new Exception("???????????????????????????");
		}
		AuthenticationService authenticationService = new AuthenticationService();
		UserAuthentication authentication = (UserAuthentication) authenticationService.createAuthenticate("default",
				pscCommonTaskRequest.getUserId(), null, null);
		logger.info("authentication--------->" + authentication.getName());
		Authentications.revalidateSession(request, authentication);
		List<TaskDto> taskList = new ArrayList<TaskDto>();

		ActivityInstance tree = runtimeService.getActivityInstance(pscCommonTaskRequest.getProcessInstId());
		if(rejectType.equals(PscCommonTaskRequest.REJECT_TO_START)){
			List<HistoricActivityInstance> resultList = historyService
					.createHistoricActivityInstanceQuery()
					.processInstanceId(pscCommonTaskRequest.getProcessInstId())
					.activityType("userTask")
					.finished()
					.orderByHistoricActivityInstanceEndTime()
					.asc()
					.list();
			if (resultList == null || resultList.size() <= 0) {
				throw new Exception("?????????????????????");
			}
			pscCommonTaskRequest.setToActId(resultList.get(0).getActivityId());
		}else if(rejectType.equals(PscCommonTaskRequest.REJECT_TO_LAST)){
			List<HistoricActivityInstance> resultList = historyService
					.createHistoricActivityInstanceQuery()
					.processInstanceId(pscCommonTaskRequest.getProcessInstId())
					.activityType("userTask")
					.finished()
					.orderByHistoricActivityInstanceEndTime()
					.desc()
					.list();
			if (resultList == null || resultList.size() <= 0) {
				throw new Exception("?????????????????????");
			}
			pscCommonTaskRequest.setToActId(resultList.get(0).getActivityId());
		}else if(rejectType.equals(PscCommonTaskRequest.REJECT_TO_TARGET)){
			if(StringUtils.isBlank(pscCommonTaskRequest.getToActId())){
				throw new Exception("?????????????????????????????????");
			}
		}else{
			throw new Exception("????????????????????????????????????  1??????????????????2??????????????????3??????????????????");
		}
		
		taskService.createComment(pscCommonTaskRequest.getTaskId(), pscCommonTaskRequest.getProcessInstId(), "????????????");
		runtimeService
	      .createProcessInstanceModification(pscCommonTaskRequest.getProcessInstId())
	      .cancelActivityInstance(getInstanceIdForActivity(tree, pscCommonTaskRequest.getTaskDefKey()))
	      .startBeforeActivity(pscCommonTaskRequest.getToActId())
	      .execute();
		

		taskList = simpleGetTasks(pscCommonTaskRequest.getProcessInstId());
//		if (taskList != null && taskList.size() == 1) {
//			taskService.setAssignee(taskList.get(0).getId(), pscCommonTaskRequest.getNextUserId());
//		}
//		taskList = simpleGetTasks(pscCommonTaskRequest.getProcessInstId());
		return taskList;
	}
	
	@Override
	public List<TaskDto> simpleTerminateProcess(PscCommonTaskRequest pscCommonTaskRequest, HttpServletRequest request)
			throws Exception {
		AuthenticationService authenticationService = new AuthenticationService();
		UserAuthentication authentication = (UserAuthentication) authenticationService.createAuthenticate("default",
				pscCommonTaskRequest.getUserId(), null, null);
		logger.info("authentication--------->" + authentication.getName());
		Authentications.revalidateSession(request, authentication);
		List<TaskDto> taskList = new ArrayList<TaskDto>();

		ActivityInstance tree = runtimeService.getActivityInstance(pscCommonTaskRequest.getProcessInstId());
		taskService.createComment(pscCommonTaskRequest.getTaskId(), pscCommonTaskRequest.getProcessInstId(), "?????????????????????");
		runtimeService.deleteProcessInstance(pscCommonTaskRequest.getProcessInstId(),TaskEntity.DELETE_REASON_COMPLETED);
//		runtimeService
//	      .createProcessInstanceModification(pscCommonTaskRequest.getProcessInstId())
//	      .cancelActivityInstance(getInstanceIdForActivity(tree, pscCommonTaskRequest.getTaskDefKey()))
//	      .execute();
		taskList = simpleGetTasks(pscCommonTaskRequest.getProcessInstId());
//		if (taskList != null && taskList.size() == 1) {
//			taskService.setAssignee(taskList.get(0).getId(), pscCommonTaskRequest.getNextUserId());
//		}
//		taskList = simpleGetTasks(pscCommonTaskRequest.getProcessInstId());
		return taskList;
	}
	
	@Override
	public List<TaskDto> simpleRestartProcess(PscCommonTaskRequest pscCommonTaskRequest, HttpServletRequest request)
			throws Exception {
		AuthenticationService authenticationService = new AuthenticationService();
		UserAuthentication authentication = (UserAuthentication) authenticationService.createAuthenticate("default",
				pscCommonTaskRequest.getUserId(), null, null);
		logger.info("authentication--------->" + authentication.getName());
		Authentications.revalidateSession(request, authentication);
		List<TaskDto> taskList = new ArrayList<TaskDto>();
		String processDefId = pscCommonTaskRequest.getProcessDefId();
		if(StringUtils.isBlank(processDefId)){
			processDefId = historyService
	    		.createHistoricProcessInstanceQuery()
	    		.processInstanceId(pscCommonTaskRequest.getProcessInstId())
	    		.singleResult().getProcessDefinitionId();
		}
		System.out.println("processDefId---->"+processDefId);
	    runtimeService.restartProcessInstances(processDefId)
		    .startBeforeActivity(pscCommonTaskRequest.getTaskDefKey())
		    .initialSetOfVariables()
		    .processInstanceIds(pscCommonTaskRequest.getProcessInstId())
		    .execute();
		
		/*HistoricProcessInstanceQuery hisProcessInstQuery = historyService
		        .createHistoricProcessInstanceQuery()
		        .processDefinitionId(pscCommonTaskRequest.getProcessDefId());

	    runtimeService.restartProcessInstances(pscCommonTaskRequest.getProcessInstId())
		    .startBeforeActivity(pscCommonTaskRequest.getTaskDefKey())
		    .historicProcessInstanceQuery(hisProcessInstQuery)
		    .initialSetOfVariables()
		    .execute();*/
	    
		taskList = simpleGetTasks(pscCommonTaskRequest.getProcessInstId());
//		if (taskList != null && taskList.size() == 1) {
//			taskService.setAssignee(taskList.get(0).getId(), pscCommonTaskRequest.getNextUserId());
//		}
//		taskList = simpleGetTasks(pscCommonTaskRequest.getProcessInstId());
		return taskList;
	}
	
	@Override
	public List<TaskDto> simpleTurnOverProcess(PscCommonTaskRequest pscCommonTaskRequest, HttpServletRequest request)
			throws Exception {
		AuthenticationService authenticationService = new AuthenticationService();
		UserAuthentication authentication = (UserAuthentication) authenticationService.createAuthenticate("default",
				pscCommonTaskRequest.getUserId(), null, null);
		logger.info("authentication--------->" + authentication.getName());
		Authentications.revalidateSession(request, authentication);
		List<TaskDto> taskList = new ArrayList<TaskDto>();
		Task task = taskService.createTaskQuery().taskId(pscCommonTaskRequest.getTaskId()).singleResult();
	    task.setAssignee(pscCommonTaskRequest.getNextUserId());
	    taskService.saveTask(task);
	    String comment = pscCommonTaskRequest.getUserId()+"??????????????????"+pscCommonTaskRequest.getNextUserId()+"??????";
	    taskService.createComment(pscCommonTaskRequest.getTaskId(), pscCommonTaskRequest.getProcessInstId(),comment);
		taskList = simpleGetTasks(pscCommonTaskRequest.getProcessInstId());
//		if (taskList != null && taskList.size() == 1) {
//			taskService.setAssignee(taskList.get(0).getId(), pscCommonTaskRequest.getNextUserId());
//		}
//		taskList = simpleGetTasks(pscCommonTaskRequest.getProcessInstId());
		return taskList;
	}
	
	@Override
	public List<TaskDto> simpleJumpProcess(PscCommonTaskRequest pscCommonTaskRequest, HttpServletRequest request)
			throws Exception {
		AuthenticationService authenticationService = new AuthenticationService();
		UserAuthentication authentication = (UserAuthentication) authenticationService.createAuthenticate("default",
				pscCommonTaskRequest.getUserId(), null, null);
		logger.info("authentication--------->" + authentication.getName());
		Authentications.revalidateSession(request, authentication);
		List<TaskDto> taskList = new ArrayList<TaskDto>();
		
		ActivityInstance tree = runtimeService.getActivityInstance(pscCommonTaskRequest.getProcessInstId());
		if(StringUtils.isBlank(pscCommonTaskRequest.getJumpType())){
			throw new Exception("???????????????????????????");
		}
		if(StringUtils.isBlank(pscCommonTaskRequest.getToActId())){
			throw new Exception("???????????????????????????");
		}
		if(pscCommonTaskRequest.getJumpType().equals(PscCommonTaskRequest.JUMP_BACK)){
			taskService.createComment(pscCommonTaskRequest.getTaskId(), pscCommonTaskRequest.getProcessInstId(), "??????????????????");
			runtimeService
		      .createProcessInstanceModification(pscCommonTaskRequest.getProcessInstId())
		      .cancelActivityInstance(getInstanceIdForActivity(tree, pscCommonTaskRequest.getTaskDefKey()))
		      .startBeforeActivity(pscCommonTaskRequest.getToActId())
		      .execute();
		}else if(pscCommonTaskRequest.getJumpType().equals(PscCommonTaskRequest.JUMP_FORWARD)){
			taskService.complete(pscCommonTaskRequest.getTaskId(), pscCommonTaskRequest.getVariables());
			taskService.createComment(pscCommonTaskRequest.getTaskId(), pscCommonTaskRequest.getProcessInstId(), "??????????????????");
			ActivityInstance tree2 = runtimeService.getActivityInstance(pscCommonTaskRequest.getProcessInstId());
			runtimeService
				.createProcessInstanceModification(pscCommonTaskRequest.getProcessInstId())
				.cancelActivityInstance(getInstanceIdForActivity(tree2, tree2.getActivityId()))
				.startBeforeActivity(pscCommonTaskRequest.getToActId())
				.execute();
		}
		taskList = simpleGetTasks(pscCommonTaskRequest.getProcessInstId());
//		if (taskList != null && taskList.size() == 1) {
//			taskService.setAssignee(taskList.get(0).getId(), pscCommonTaskRequest.getNextUserId());
//		}
//		taskList = simpleGetTasks(pscCommonTaskRequest.getProcessInstId());
		return taskList;
	}
	
	public List<TaskDto> simpleGetTasks(String processInstId) throws Exception {
		List<TaskDto> resultList = new ArrayList<TaskDto>();
		List<Task> taskList = taskService.createTaskQuery().processInstanceId(processInstId).list();
		for (Task task : taskList) {
			TaskDto dto = new TaskDto();
			dto = TaskDto.fromEntity(task);
			resultList.add(dto);
		}
		return resultList;
	}

	public String getInstanceIdForActivity(ActivityInstance activityInstance, String activityId) {
		ActivityInstance instance = getChildInstanceForActivity(activityInstance, activityId);
		if (instance != null) {
			return instance.getId();
		}
		return null;
	}

	public ActivityInstance getChildInstanceForActivity(ActivityInstance activityInstance, String activityId) {
		if (activityId.equals(activityInstance.getActivityId())) {
			return activityInstance;
		}

		for (ActivityInstance childInstance : activityInstance.getChildActivityInstances()) {
			ActivityInstance instance = getChildInstanceForActivity(childInstance, activityId);
			if (instance != null) {
				return instance;
			}
		}

		return null;
	}
}
