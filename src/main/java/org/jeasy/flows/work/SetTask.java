package org.jeasy.flows.work;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An example unit of work for writing to the logger
 *
 * @author matt rajkowski
 */
public class SetTask implements Work {

  private static final Logger LOGGER = LoggerFactory.getLogger(SetTask.class.getName());

  @Override
  public WorkReport execute(WorkContext workContext, TaskContext taskContext) {
    if (taskContext.getData() != null) {
      String[] field = taskContext.getData().split("=");
      LOGGER.debug("Setting field: " + field[0] + "=" + field[1]);
      workContext.put(field[0].trim(), field[1].trim());
      return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
    }
    return new DefaultWorkReport(WorkStatus.FAILED, workContext);
  }
}
