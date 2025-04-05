/*
 * The MIT License
 *
 *  Copyright 2021 Matt Rajkowski (https://github.com/rajkowski)
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
