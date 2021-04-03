package org.jeasy.flows.playbook;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Playbooks are the configuration of processes and variables for executing workflows
 *
 * @author matt rajkowski
 */
public class Playbook {

  final static long serialVersionUID = 8345648404174283570L;

  private String id = null;
  private String name = null;
  private Map<String, Object> vars = null;

  @JsonAlias({"tasks", "workflow"})
  private TaskList taskList = null;

  public Playbook() {
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<String, Object> getVars() {
    if (vars == null) {
      vars = new LinkedHashMap<>();
    }
    return vars;
  }

  public void setVars(Map<String, Object> vars) {
    this.vars = vars;
  }

  public void addVar(String name, Object value) {
    if (vars == null) {
      vars = new LinkedHashMap<>();
    }
    vars.put(name, value);
  }

  public TaskList getTaskList() {
    return taskList;
  }

  public void setTaskList(TaskList taskList) {
    this.taskList = taskList;
  }

  public void add(Task task) {
    if (taskList == null) {
      taskList = new TaskList();
    }
    taskList.add(task);
  }
}
