package org.jeasy.flows.work;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An example task for writing strings to the logger
 *
 * @author matt rajkowski
 */
public class LogTask implements Work {

  private static final Logger LOGGER = LoggerFactory.getLogger(LogTask.class.getName());

  public static final String GLOBAL_MESSAGE_VAR = "log-message";
  public static final String MESSAGE_VAR = "message";

  @Override
  public WorkReport execute(WorkContext workContext, TaskContext taskContext) {
    // Find the message
    String message = null;
    if (taskContext.getData() != null) {
      message = taskContext.getData();
    } else if (taskContext.containsKey(MESSAGE_VAR)) {
      message = (String) taskContext.get(MESSAGE_VAR);
    } else if (workContext.containsKey(GLOBAL_MESSAGE_VAR)) {
      message = (String) workContext.get(GLOBAL_MESSAGE_VAR);
    }
    if (message == null) {
      return new DefaultWorkReport(WorkStatus.FAILED, workContext);
    }

    // Evaluate values within the message
    if (message.contains("{{") && message.contains("}}")) {
      // Find all of the expressions
      Pattern pattern = Pattern.compile("\\{\\{(.*?)\\}\\}", Pattern.DOTALL);
      Matcher matcher = pattern.matcher(message);
      // Evaluate and replace the expressions with a result
      StringBuffer sb = new StringBuffer();
      while (matcher.find()) {
        String expression = matcher.group(1).trim();
        LOGGER.debug("Expression found: " + expression);
        String replacement = String.valueOf(Expression.evaluate(workContext, taskContext, expression));
        matcher.appendReplacement(sb, replacement);
      }
      matcher.appendTail(sb);
      message = sb.toString();
    }

    LOGGER.info(message);
    return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
  }
}
