/*
 * The MIT License
 *
 *  Copyright 2021-2026 Matt Rajkowski (https://github.com/rajkowski)
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package com.github.rajkowski.workflow.playbook;

import static com.github.rajkowski.workflow.flow.ParallelFlow.Builder.aNewParallelFlow;
import static com.github.rajkowski.workflow.flow.RepeatFlow.Builder.aNewRepeatFlow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rajkowski.workflow.engine.WorkflowExecutionSnapshot;
import com.github.rajkowski.workflow.flow.ParallelFlow;
import com.github.rajkowski.workflow.flow.RepeatFlow;
import com.github.rajkowski.workflow.work.EvaluateTask;
import com.github.rajkowski.workflow.work.Expression;
import com.github.rajkowski.workflow.work.IndexedWorkReport;
import com.github.rajkowski.workflow.work.LogTask;
import com.github.rajkowski.workflow.work.NoOpTask;
import com.github.rajkowski.workflow.work.PauseTask;
import com.github.rajkowski.workflow.work.SetTask;
import com.github.rajkowski.workflow.work.TaskContext;
import com.github.rajkowski.workflow.work.WhenTask;
import com.github.rajkowski.workflow.work.Work;
import com.github.rajkowski.workflow.work.WorkContext;
import com.github.rajkowski.workflow.work.WorkReport;
import com.github.rajkowski.workflow.work.WorkStatus;

/**
 * The PlaybookManager initializes workflows from playbooks, and uses the WorkflowEngine to run them
 *
 * @author matt rajkowski
 */

public class PlaybookManager {

  final static long serialVersionUID = 8345648404174283570L;
  private static final Logger LOGGER = LoggerFactory.getLogger(PlaybookManager.class.getName());

  private static Map<String, Playbook> playbooks = new HashMap<>();
  private static Map<String, Object> taskInstances = new HashMap<>();
  private static JexlPermissions jexlPermissions = JexlPermissions.UNRESTRICTED;

  public static void register(Map<String, String> taskLibrary) {
    LOGGER.info("Registering classes...");
    if (!taskLibrary.containsKey("set")) {
      taskLibrary.put("set", SetTask.class.getName());
    }
    if (!taskLibrary.containsKey("when")) {
      taskLibrary.put("when", WhenTask.class.getName());
    }
    if (!taskLibrary.containsKey("evaluate")) {
      taskLibrary.put("evaluate", EvaluateTask.class.getName());
    }
    if (!taskLibrary.containsKey("log")) {
      taskLibrary.put("log", LogTask.class.getName());
    }
    if (!taskLibrary.containsKey("noop")) {
      taskLibrary.put("noop", NoOpTask.class.getName());
    }
    if (!taskLibrary.containsKey("pause")) {
      taskLibrary.put("pause", PauseTask.class.getName());
    }
    for (String className : taskLibrary.keySet()) {
      try {
        if (!taskInstances.containsKey(className)) {
          String taskClass = taskLibrary.get(className);
          Object classRef = Class.forName(taskClass).getDeclaredConstructor().newInstance();
          taskInstances.put(className, classRef);
          LOGGER.info("Added class: " + className + " = " + taskClass);
        }
      } catch (Exception e) {
        LOGGER.error("Class not found for '" + className + "': " + e.getMessage());
      }
    }
  }

  public static Playbook getPlaybook(String id) {
    return playbooks.get(id);
  }

  public static void add(Playbook playbook) {
    if (playbook == null) {
      LOGGER.error("Playbook is null");
      return;
    }
    playbooks.put(playbook.getId(), playbook);
  }

  public static void add(List<Playbook> playbookList) {
    if (playbookList == null) {
      LOGGER.error("Playbook list is null");
      return;
    }
    for (Playbook playbook : playbookList) {
      LOGGER.info("Adding playbook: " + playbook.getId());
      playbooks.put(playbook.getId(), playbook);
    }
  }

  public static WorkReport run(String playbookName) {
    return run(playbookName, null);
  }

  public static WorkReport run(String playbookName, WorkContext workContext) {
    Playbook playbook = playbooks.get(playbookName);
    return run(playbook, workContext);
  }

  public static WorkReport run(Playbook playbook) {
    return run(playbook, null);
  }

  public static WorkReport run(Playbook playbook, WorkContext workContext) {
    return run(playbook, workContext, null);
  }

  /**
   * Executes a playbook's tasks, resuming right after the exact task that previously paused/waited when
   * {@code resumeAfterElementId} is given. The element id is a hierarchical position (e.g. "2.1" for the
   * second sub-task of the top-level task at index 2), so a pause inside a nested block is resumed at the
   * precise sub-task rather than restarting the whole block.
   *
   * @param playbook            the playbook to execute
   * @param workContext         the work context to execute with, restored state included
   * @param resumeAfterElementId the element id of the task to resume after, or {@code null} to start from the beginning
   * @return the work report of the last executed task, decorated with its task element id when paused/waiting
   */
  public static WorkReport run(Playbook playbook, WorkContext workContext, String resumeAfterElementId) {
    if (playbook == null || playbook.getTaskList().isEmpty()) {
      LOGGER.error("Playbook is null or empty...");
      return null;
    }
    if (taskInstances == null || taskInstances.isEmpty()) {
      register(new HashMap<>());
    }
    // Assign hierarchical element ids (e.g. "2.1") to every task, purely from its position in the tree
    assignElementIds(playbook.getTaskList(), null);
    // Verify there is a matching work item and set the work item specific variables
    LOGGER.debug("Verifying playbook... '" + playbook.getId() + "'");
    for (Task task : playbook.getTaskList()) {
      LOGGER.debug("Checking: " + task.getId());
      if ("block".equals(task.getId()) || "parallel".equals(task.getId())) {
        // Verify the referenced TaskList
        for (Task blockTask : task.getTaskList()) {
          LOGGER.debug("  Checking: " + blockTask.getId());
          if (!taskInstances.containsKey(blockTask.getId())) {
            LOGGER.error("Block Task id not found: " + blockTask.getId());
            return null;
          }
        }
      } else if (!taskInstances.containsKey(task.getId())) {
        LOGGER.error("Task id not found: " + task.getId());
        return null;
      }
    }
    // Make sure workContext exists and has playbook vars
    if (workContext == null) {
      workContext = new WorkContext(playbook);
    }

    List<Task> taskList = playbook.getTaskList();
    int beginIndex = 0;
    // When resuming inside a block/parallel task, re-enter that same top-level task instead of skipping it
    String resumeWithinTaskElementId = null;
    if (resumeAfterElementId != null) {
      String[] parts = resumeAfterElementId.split("\\.", 2);
      int topIndex = Integer.parseInt(parts[0]);
      if (parts.length > 1) {
        beginIndex = topIndex;
        resumeWithinTaskElementId = resumeAfterElementId;
      } else {
        beginIndex = topIndex + 1;
      }
    }
    LOGGER.info("Executing workflow... " + playbook.getId() + " from task element " + beginIndex);

    WorkReport workReport = null;
    for (int index = beginIndex; index < taskList.size(); index++) {
      Task task = taskList.get(index);
      WorkReport stepReport = executeTopLevelTask(task, workContext, resumeWithinTaskElementId);
      resumeWithinTaskElementId = null;
      if (stepReport == null) {
        continue;
      }
      workReport = stepReport;
      if (WorkStatus.WAITING.equals(stepReport.getStatus()) || WorkStatus.PAUSED.equals(stepReport.getStatus())) {
        String pausedElementId = stepReport instanceof IndexedWorkReport ? stepReport.getTaskId() : task.getElementId();
        LOGGER.info("Task '" + task.getId() + "' at element " + pausedElementId
            + " is waiting for external input, stopping the workflow execution");
        return stepReport instanceof IndexedWorkReport ? stepReport
            : new IndexedWorkReport(stepReport, pausedElementId);
      }
      if (WorkStatus.FAILED.equals(stepReport.getStatus())) {
        // A 'block' reports FAILED when its own 'when' condition doesn't match; that only skips the
        // block itself, it must not stop the rest of the playbook
        if ("block".equals(task.getId())) {
          continue;
        }
        LOGGER.warn(
            "Task '" + task.getId() + "' at element " + task.getElementId() + " has failed, skipping subsequent tasks");
        break;
      }
    }
    return workReport;
  }

  /**
   * Resumes execution of the playbook structure captured in the snapshot, continuing right after the
   * exact task that previously paused/waited, including inside a nested block.
   *
   * @param snapshot the exported workflow execution snapshot to resume from
   * @return the work report produced by continuing execution
   */
  public static WorkReport resume(WorkflowExecutionSnapshot snapshot) {
    if (snapshot == null) {
      throw new IllegalArgumentException("Workflow execution snapshot cannot be null");
    }
    // Use the snapshot's own playbook copy so the resumed task list matches exactly what paused
    return run(snapshot.getPlaybook(), snapshot.getWorkContext(), snapshot.getPausedTaskId());
  }

  /**
   * Assigns each task a hierarchical element id (e.g. "2.1") purely from its position in the tree, so
   * the exact paused task can be identified and resumed, even when nested inside a block or parallel task.
   */
  private static void assignElementIds(List<Task> tasks, String parentElementId) {
    for (int i = 0; i < tasks.size(); i++) {
      Task task = tasks.get(i);
      String elementId = parentElementId == null ? String.valueOf(i) : parentElementId + "." + i;
      task.setElementId(elementId);
      if (task.hasTasks()) {
        assignElementIds(task.getTaskList(), elementId);
      }
    }
  }

  private static WorkReport executeTopLevelTask(Task task, WorkContext workContext, String resumeWithinTaskElementId) {
    // Create a TaskContext which contains objects for the task to use
    TaskContext taskContext = createTaskContext(task, (Work) taskInstances.get(task.getId()));
    // Determine if the task contains sub-tasks, for conditional or parallel tasks
    if (task.hasTasks()) {
      if ("block".equals(task.getId())) {
        LOGGER.debug("Executing block...");
        List<Task> blockTasks = task.getTaskList();
        int subBeginIndex = 0;
        if (resumeWithinTaskElementId != null) {
          // e.g. "2.1" -> resume right after the block's sub-task at index 1
          String[] parts = resumeWithinTaskElementId.split("\\.");
          subBeginIndex = Integer.parseInt(parts[1]) + 1;
        }
        WorkReport blockReport = null;
        for (int i = subBeginIndex; i < blockTasks.size(); i++) {
          Task blockTask = blockTasks.get(i);
          TaskContext blockTaskContext = createTaskContext(blockTask, (Work) taskInstances.get(blockTask.getId()));
          if (blockTaskContext.getWhen() != null) {
            boolean result = Expression.validate(workContext, blockTaskContext, blockTaskContext.getWhen());
            if (!result) {
              // The block's own 'when' condition doesn't match; stop the whole block
              break;
            }
          }
          WorkReport subReport;
          if (blockTask.getRepeat() > 0) {
            LOGGER.debug("Creating a RepeatFlow...");
            RepeatFlow repeatFlow = aNewRepeatFlow()
                .repeat(blockTaskContext)
                .times(blockTask.getRepeat())
                .build();
            subReport = repeatFlow.execute(workContext);
          } else {
            subReport = blockTaskContext.getWork().execute(workContext, blockTaskContext);
          }
          blockReport = subReport;
          if (subReport != null
              && (WorkStatus.WAITING.equals(subReport.getStatus())
                  || WorkStatus.PAUSED.equals(subReport.getStatus()))) {
            LOGGER.info("Block sub-task '" + blockTask.getId() + "' at element " + blockTask.getElementId()
                + " is waiting for external input, stopping the workflow execution");
            return new IndexedWorkReport(subReport, blockTask.getElementId());
          }
          if (subReport != null && WorkStatus.FAILED.equals(subReport.getStatus())) {
            LOGGER.warn("Block sub-task '" + blockTask.getId() + "' at element " + blockTask.getElementId()
                + " has failed, skipping subsequent block tasks");
            break;
          }
        }
        return blockReport;
      } else if ("parallel".equals(task.getId())) {
        // Construct a parallel workflow and execute it; concurrent sub-tasks have no meaningful mid-group
        // resume point, so a pause here resumes by restarting the whole parallel group
        List<TaskContext> parallelTaskContextList = new ArrayList<>();
        for (Task parallelTask : task.getTaskList()) {
          TaskContext parallelTaskContext = createTaskContext(parallelTask,
              (Work) taskInstances.get(parallelTask.getId()));
          parallelTaskContextList.add(parallelTaskContext);
        }
        ExecutorService executorService = Executors.newFixedThreadPool(task.getThreads());
        LOGGER.trace("Executor thread count: " + task.getThreads());
        LOGGER.debug("Creating a ParallelFlow...");
        ParallelFlow parallelFlow = aNewParallelFlow()
            .named(task.getId())
            .execute(parallelTaskContextList)
            .with(executorService)
            .timeout(task.getTimeout(), TimeUnit.SECONDS)
            .build();
        return parallelFlow.execute(workContext);
      } else {
        LOGGER.error("Unknown '" + task.getId() + "': This TASK has hanging TASKS!!");
        return null;
      }
    }

    // Just a task to be added, honoring its 'when' condition like the top-level sequential flow did
    if (taskContext.getWhen() != null) {
      boolean result = Expression.validate(workContext, taskContext, taskContext.getWhen());
      if (!result) {
        LOGGER.warn("Skipping " + task.getId() + ", condition not met: " + taskContext.getWhen());
        return null;
      }
    }
    if (task.getRepeat() > 0) {
      LOGGER.debug("Creating a RepeatFlow...");
      RepeatFlow repeatFlow = aNewRepeatFlow()
          .repeat(taskContext)
          .times(task.getRepeat())
          .build();
      return repeatFlow.execute(workContext);
    }
    return taskContext.getWork().execute(workContext, taskContext);
  }

  private static TaskContext createTaskContext(Task task, Work work) {
    TaskContext taskContext = new TaskContext(work);
    taskContext.setData(task.getData());
    taskContext.put(task.getVars());
    taskContext.setWhen(task.getWhen());
    return taskContext;
  }

  public static JexlPermissions getJexlPermissions() {
    return jexlPermissions;
  }

  public static void setJexlPermissions(JexlPermissions permissions) {
    jexlPermissions = permissions;
  }
}
