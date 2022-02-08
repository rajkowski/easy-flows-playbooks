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

  public TaskList() {
  }

}
