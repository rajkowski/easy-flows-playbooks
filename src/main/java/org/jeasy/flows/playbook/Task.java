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
package org.jeasy.flows.playbook;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tasks are the configuration of the work which will be executed in workflows. Task ID must be unique.
 *
 * @author matt rajkowski
 */
public class Task {

  final static long serialVersionUID = 8345648404174283570L;
  private static final Logger LOGGER = LoggerFactory.getLogger(Task.class.getName());

  // Core properties
  private String id = null;
  private String name = null;
  private String data = null;
  private Map<String, Object> vars = null;

  // Optional properties
  private String when = null;
  private long delay = 0;
  private long repeat = 0;
  private long timeout = 10;
  private int threads = 2;

  // Tasks which have sub-tasks
  private TaskList taskList = null;

  public Task() {

  }

  public Task(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getData() {
    return data;
  }

  public void setData(String data) {
    this.data = data;
  }

  public String getWhen() {
    return when;
  }

  public void setWhen(String when) {
    this.when = when;
  }

  public long getDelay() {
    return delay;
  }

  public void setDelay(long delay) {
    this.delay = delay;
  }

  public long getRepeat() {
    return repeat;
  }

  public void setRepeat(long repeat) {
    this.repeat = repeat;
  }

  public long getTimeout() {
    return timeout;
  }

  public void setTimeout(long timeout) {
    this.timeout = timeout;
  }

  public int getThreads() {
    return threads;
  }

  public void setThreads(int threads) {
    this.threads = threads;
  }

  public TaskList getTaskList() {
    return taskList;
  }

  public TaskList getTasks() {
    return taskList;
  }

  public boolean hasTasks() {
    return taskList != null && !taskList.isEmpty();
  }

  public void add(Task task) {
    if (taskList == null) {
      taskList = new TaskList();
    }
    taskList.add(task);
  }

  public Map<String, Object> getVars() {
    return vars;
  }

  public void setVars(Map<String, Object> vars) {
    this.vars = vars;
  }

  public void addVar(String name, Object value) {
    if (vars == null) {
      vars = new LinkedHashMap<>();
    }
    vars.put(name, value);
  }

}
