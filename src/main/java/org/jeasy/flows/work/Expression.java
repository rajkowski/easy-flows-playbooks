package org.jeasy.flows.work;

import org.apache.commons.jexl3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Functions for working with expressions
 *
 * @author matt rajkowski
 */
public class Expression {

  private static final Logger LOGGER = LoggerFactory.getLogger(Expression.class.getName());

  private static final JexlEngine jexl = new JexlBuilder().create();

  public static boolean validate(WorkContext workContext, TaskContext taskContext, String expression) {
    if (expression == null || expression.length() == 0) {
      return false;
    }
    boolean result = (Boolean) evaluate(workContext, taskContext, expression);
    LOGGER.info("Result is: " + result);
    return result;
  }

  public static Object evaluate(WorkContext workContext, TaskContext taskContext, String expression) {
    LOGGER.info("Evaluate: " + expression);
    if (expression == null || expression.length() == 0) {
      return expression;
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

  public static Object evaluate(JexlContext mapContext, String expression) {
    if (expression == null || expression.length() == 0) {
      return expression;
    }
    if (expression.contains("{{") && expression.contains("}}")) {
      int startIdx = expression.indexOf("{{");
      int endIdx = expression.indexOf("}}");
      int checkIdx2 = expression.indexOf("{{", startIdx + 2);
      if (startIdx > 0 || checkIdx2 > -1 || endIdx < expression.length() - 2) {
        // Combine multiple expressions into a string, and expressions within a string
        // {{ Hello }} {{ there }}
        // {{ Hello }} there
        // Hello {{ there }}
        return evaluateAll(mapContext, expression);
      } else {
        // Strip off the {{ }}
        // {{ Hello there }}
        expression = expression.substring(2, expression.length() - 2).trim();
      }
    }

    // Treat as an object
    JexlScript compiledScript = jexl.createScript(expression);
    return compiledScript.execute(mapContext);
  }

  private static Object evaluateAll(JexlContext mapContext, String property) {
    // Find all of the expressions
    Pattern pattern = Pattern.compile("\\{\\{(.*?)\\}\\}", Pattern.DOTALL);
    Matcher matcher = pattern.matcher(property);

    // Evaluate and replace the expressions with a result
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String expression = matcher.group(1).trim();
      LOGGER.debug("Expression found: " + expression);
      try {
        JexlScript compiledScript = jexl.createScript(expression);
        Object result = compiledScript.execute(mapContext);
        if (result != null) {
          String replacement = String.valueOf(result);
          matcher.appendReplacement(sb, replacement);
        } else {
          LOGGER.error("Expression not replaced: " + expression);
        }
      } catch (Exception e) {
        LOGGER.error("Expression error: " + expression, e);
      }
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

}
