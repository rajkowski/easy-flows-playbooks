package org.jeasy.flows.work;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A task for evaluating an expression
 *
 * @author matt rajkowski
 */
public class EvaluateTask implements Work {

  private static final Logger LOGGER = LoggerFactory.getLogger(EvaluateTask.class.getName());

  @Override
  public WorkReport execute(WorkContext workContext, TaskContext taskContext) {
    if (taskContext.getData() != null) {
      Expression.evaluate(workContext, taskContext, taskContext.getData());
      return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
    }
    return new DefaultWorkReport(WorkStatus.FAILED, workContext);
  }
}
