package org.jeasy.flows.work;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A task for validating a condition, assumes an expression
 *
 * @author matt rajkowski
 */
public class WhenTask implements Work {

  private static final Logger LOGGER = LoggerFactory.getLogger(WhenTask.class.getName());

  @Override
  public WorkReport execute(WorkContext workContext, TaskContext taskContext) {
    if (taskContext.getData() != null) {
      if (Expression.validate(workContext, taskContext, taskContext.getData())) {
        return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
      }
    }
    return new DefaultWorkReport(WorkStatus.FAILED, workContext);
  }
}
