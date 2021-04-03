/*
 * The MIT License
 *
 *  Copyright (c) 2020, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The configuration and variables specific to the unit of work to execute. The TaskContext should not be re-used.
 *
 * @author matt rajkowski
 */
public class TaskContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskContext.class.getName());

  private Work work = null;
  private String data = null;
  private Map<String, Object> vars = new HashMap<>();
  private String when = null;

  public TaskContext(Work work) {
    this.work = work;
  }

  public TaskContext(Work work, String data) {
    this.work = work;
    this.data = data;
  }

  public Work getWork() {
    return work;
  }

  public void setWork(Work work) {
    this.work = work;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public Set<Map.Entry<String, Object>> getEntrySet() {
    return vars.entrySet();
  }

  public Object get(String key) {
    return vars.get(key);
  }

  public void put(String key, Object value) {
    vars.put(key, value);
  }

  public void put(Map<String, Object> vars) {
    if (vars == null) {
      return;
    }
    this.vars.putAll(vars);
  }

  public boolean containsKey(String key) {
    return vars.containsKey(key);
  }

  public WorkReport execute(WorkContext workContext, TaskContext taskContext) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Executing work: " + taskContext.getWork().getClass().getSimpleName());
    }
    return work.execute(workContext, taskContext);
  }

  public String getWhen() {
    return when;
  }

  public void setWhen(String when) {
    this.when = when;
  }

  @Override
  public String toString() {
    return "data=" + data + ";vars=" + vars + ";when=" + when + "}";
  }
}
