package org.jeasy.flows.work;

import org.apache.commons.jexl3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Task for putting an object into the work context
 *
 * @author matt rajkowski
 */
public class SetTask implements Work {

  private static final Logger LOGGER = LoggerFactory.getLogger(SetTask.class.getName());

  private static final JexlEngine jexl = new JexlBuilder().create();

  @Override
  public WorkReport execute(WorkContext workContext, TaskContext taskContext) {
    if (taskContext.getData() == null) {
      return new DefaultWorkReport(WorkStatus.FAILED, workContext);
    }
    // Determine the strings before and after the =
    int idx = taskContext.getData().indexOf("=");
    String property = taskContext.getData().substring(0, idx).trim();
    String value = taskContext.getData().substring(idx + 1).trim();

    // See if the value is an expression
    boolean isExpression = false;
    if (value.startsWith("{{") && value.endsWith("}}")) {
      // Found an expression
      isExpression = true;
      value = value.substring(2, value.length() - 2);
    } else {
      int sz = value.length();
      for (int i = 0; i < sz; ++i) {
        if (!Character.isLetterOrDigit(value.charAt(i))) {
          isExpression = true;
        }
      }
    }

    if (!isExpression) {
      LOGGER.debug("Setting field: " + property + "=" + value);
      workContext.put(property, value);
      return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
    }

    // Evaluate the expression
    JexlContext mapContext = new MapContext();
    for (Map.Entry<String, Object> entry : workContext.getEntrySet()) {
      mapContext.set(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, Object> entry : taskContext.getEntrySet()) {
      mapContext.set(entry.getKey(), entry.getValue());
    }
    JexlScript compiledScript = jexl.createScript(value);
    Object result = compiledScript.execute(mapContext);
    LOGGER.debug("Setting field: " + property + "=" + result);
    workContext.put(property, result);

    return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
  }
}
