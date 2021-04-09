package org.jeasy.flows.playbook;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tasks are the configuration of the work which will be executed in workflows. Task ID must be unique.
 *
 * @author matt rajkowski
 */
public class Task {

  final static long serialVersionUID = 8345648404174283570L;
  private static final Logger LOGGER = LoggerFactory.getLogger(Task.class.getName());

  // Core properties
  private String id = null;
  private String name = null;
  private String data = null;
  private Map<String, Object> vars = null;

  // Optional properties
  private String when = null;
  private long delay = 0;
  private long repeat = 0;
  private long timeout = 10;
  private int threads = 2;

  // Tasks which have sub-tasks
  private TaskList taskList = null;

  public Task() {

  }

  public Task(String id) {
    this.id = id;
    LOGGER.debug("Task Created: " + id);
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

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public String getWhen() {
    return when;
  }

  public void setWhen(String when) {
    this.when = when;
  }

  public long getDelay() {
    return delay;
  }

  public void setDelay(long delay) {
    this.delay = delay;
  }

  public long getRepeat() {
    return repeat;
  }

  public void setRepeat(long repeat) {
    this.repeat = repeat;
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  public int getThreads() {
    return threads;
  }

  public void setThreads(int threads) {
    this.threads = threads;
  }

  public TaskList getTaskList() {
    return taskList;
  }

  public TaskList getTasks() {
    return taskList;
  }

  public boolean hasTasks() {
    return taskList != null && !taskList.isEmpty();
  }

  public void setTaskList(TaskList taskList) {
    this.taskList = taskList;
  }

  public void setTasks(TaskList taskList) {
    this.taskList = taskList;
  }

  public void add(Task task) {
    if (taskList == null) {
      taskList = new TaskList();
    }
    taskList.add(task);
  }

  public Map<String, Object> getVars() {
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

  @JsonAnySetter
  public void setDynamicProperty(String name, String value) {
    if (id == null) {
      LOGGER.debug("Task Created: " + name + "=" + value);
      this.id = name;
      if (value != null) {
        this.data = value;
      }
    } else {
      LOGGER.debug(" Variable added: " + name + "=" + value);
      addVar(name, value);
    }
  }

  @JsonSetter("block")
  public void setBlock(List<Map<String, String>> block) {
    // A task block has sub-tasks
    this.id = "block";
    if (taskList == null) {
      taskList = new TaskList();
    }
    // Store this block's tasks and properties
    for (Map<String, String> blockList : block) {
      Task task = new Task();
      taskList.add(task);
      boolean foundId = false;
      for (String name : blockList.keySet()) {
        String value = blockList.get(name);
        // Check for reserved names
        if ("name".equals(name)) {
          task.setName(value);
        } else if ("when".equals(name)) {
          // Determine if this task is a single 'when' task
          if (blockList.size() > 1) {
            task.setWhen(value);
          }
          // This is also a task due to serialization
          if (task.getId() == null) {
            task.setId(name);
            task.setData(value);
            // @note let a later name/value overwrite this
          }
        } else if ("delay".equals(name)) {
          task.setDelay(Long.parseLong(value));
        } else if ("repeat".equals(name)) {
          task.setRepeat(Long.parseLong(value));
        } else if ("timeout".equals(name)) {
          task.setTimeout(Long.parseLong(value));
        } else if ("threads".equals(name)) {
          task.setThreads(Integer.parseInt(value));
        } else if (value == null) {
          task.setId(name);
        } else if (!foundId) {
          task.setId(name);
          task.setData(value);
          foundId = true;
        } else {
          // add vars
          task.addVar(name, value);
        }
      }
    }
  }
}
