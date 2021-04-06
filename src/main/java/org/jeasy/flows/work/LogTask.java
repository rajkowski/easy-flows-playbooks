package org.jeasy.flows.work;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An example unit of work for writing to the logger
 *
 * @author matt rajkowski
 */
public class LogTask implements Work {

  private static final Logger LOGGER = LoggerFactory.getLogger(LogTask.class.getName());

  public static final String GLOBAL_MESSAGE_VAR = "log-message";
  public static final String MESSAGE_VAR = "message";

  @Override
  public WorkReport execute(WorkContext workContext, TaskContext taskContext) {
    if (taskContext.getData() != null) {
      LOGGER.info(taskContext.getData());
    } else if (taskContext.containsKey(MESSAGE_VAR)) {
      LOGGER.info((String) taskContext.get(MESSAGE_VAR));
    } else if (workContext.containsKey(GLOBAL_MESSAGE_VAR)) {
      LOGGER.info((String) workContext.get(GLOBAL_MESSAGE_VAR));
    } else {
      return new DefaultWorkReport(WorkStatus.FAILED, workContext);
    }
    return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
  }
}
