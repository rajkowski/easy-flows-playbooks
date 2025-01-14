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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
      LOGGER.warn("A message was not found");
      return new DefaultWorkReport(WorkStatus.FAILED, workContext);
    }

    // Evaluate values within the message
    if (message.contains("{{") && message.contains("}}")) {
      message = String.valueOf(Expression.evaluate(workContext, taskContext, message));
    }

    LOGGER.debug(message);
    return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
  }
}
