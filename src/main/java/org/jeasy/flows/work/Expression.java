package org.jeasy.flows.work;

import org.apache.commons.jexl3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Functions for working with expressions
 *
 * @author matt rajkowski
 */
public class Expression {

  private static final Logger LOGGER = LoggerFactory.getLogger(Expression.class.getName());

  private static final JexlEngine jexl = new JexlBuilder().create();

  public static boolean validate(WorkContext workContext, TaskContext taskContext, String expression) {
    boolean result = (Boolean) evaluate(workContext, taskContext, expression);
    LOGGER.info("Result is: " + result);
    return result;
  }

  public static Object evaluate(WorkContext workContext, TaskContext taskContext, String expression) {
    LOGGER.info("Evaluate: " + expression);
    if (expression == null) {
      return false;
    }

    JexlContext mapContext = new MapContext();
    for (Map.Entry<String, Object> entry : workContext.getEntrySet()) {
      mapContext.set(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, Object> entry : taskContext.getEntrySet()) {
      mapContext.set(entry.getKey(), entry.getValue());
    }

    return evaluate(mapContext, expression);
  }

  public static Object evaluate(Map<String, Object> context, String expression) {
    JexlContext mapContext = new MapContext();
    for (Map.Entry<String, Object> entry : context.entrySet()) {
      mapContext.set(entry.getKey(), entry.getValue());
    }
    return evaluate(mapContext, expression);
  }

  public static Object evaluate(JexlContext mapContext, String expression) {
    JexlScript compiledScript = jexl.createScript(expression);
    return compiledScript.execute(mapContext);
  }
}
