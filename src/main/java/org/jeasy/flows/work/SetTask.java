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
package org.jeasy.flows.work;

import java.util.Map;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.jeasy.flows.playbook.PlaybookManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Task for putting an object into the work context
 *
 * @author matt rajkowski
 */
public class SetTask implements Work {

  private static final Logger LOGGER = LoggerFactory.getLogger(SetTask.class.getName());

  private static final JexlEngine jexl = new JexlBuilder().permissions(PlaybookManager.getJexlPermissions()).create();

  @Override
  public WorkReport execute(WorkContext workContext, TaskContext taskContext) {
    if (taskContext.getData() == null) {
      return new DefaultWorkReport(WorkStatus.FAILED, workContext);
    }
    // Determine the strings before and after the =
    int idx = taskContext.getData().indexOf("=");
    String property = taskContext.getData().substring(0, idx).trim();
    String value = taskContext.getData().substring(idx + 1).trim();

    if ((value.startsWith("'") && value.endsWith("'")) || (value.startsWith("\"") && value.endsWith("\""))) {
      value = value.substring(1, value.length() - 1);
    }

    // See if the value contains interpolation or a more complex expression.
    boolean isExpression = value.contains("{{") && value.contains("}}");
    if (!isExpression && value.startsWith("{{") && value.endsWith("}}")) {
      // Found an expression, but leave the value in the template form for the regular evaluation path.
      isExpression = true;
    } else if (!isExpression) {
      int sz = value.length();
      for (int i = 0; i < sz; ++i) {
        if (!Character.isLetterOrDigit(value.charAt(i))) {
          isExpression = true;
          break;
        }
      }
    }

    if (!isExpression) {
      LOGGER.debug("Setting field: " + property + "=" + value);
      workContext.put(property, value);
      return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
    }

    // Evaluate the expression or interpolation template.
    JexlContext mapContext = new MapContext();
    for (Map.Entry<String, Object> entry : workContext.getEntrySet()) {
      mapContext.set(entry.getKey(), entry.getValue());
    }
    for (Map.Entry<String, Object> entry : taskContext.getEntrySet()) {
      mapContext.set(entry.getKey(), entry.getValue());
    }
    Object result;
    if (value.contains("{{") && value.contains("}}")) {
      result = Expression.evaluate(mapContext, value);
    } else {
      JexlScript compiledScript = jexl.createScript(value);
      result = compiledScript.execute(mapContext);
    }
    LOGGER.debug("Setting field: " + property + "=" + result);
    workContext.put(property, result);

    return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
  }
}
