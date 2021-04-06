package org.jeasy.flows.playbook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * TaskLists are collections of Tasks, a holder for a group of tasks used in the WorkflowEngine
 *
 * @author matt rajkowski
 */
public class TaskList extends ArrayList<Task> {

  final static long serialVersionUID = 8345648404174283570L;

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskList.class.getName());

  private boolean parallel = false;
  private long timeout = 0;

  public TaskList() {
  }

  public boolean isParallel() {
    return parallel;
  }

  public void setParallel(boolean parallel) {
    this.parallel = parallel;
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  public Task getParallelTask(int index) {
    int found = -1;
    for (Task task : this) {
      if (task.getId().equals("parallel")) {
        ++found;
        if (found == index) {
          return task;
        }
      }
    }
    return null;
  }

}
