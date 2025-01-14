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

/**
 * Playbooks are the configuration of processes and variables for executing workflows
 *
 * @author matt rajkowski
 */
public class Playbook {

  final static long serialVersionUID = 8345648404174283570L;

  private String id = null;
  private String name = null;
  private Map<String, Object> vars = null;
  private TaskList taskList = null;

  public Playbook() {
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

  public Map<String, Object> getVars() {
    if (vars == null) {
      vars = new LinkedHashMap<>();
    }
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

  public TaskList getTaskList() {
    return taskList;
  }

  public void add(Task task) {
    if (taskList == null) {
      taskList = new TaskList();
    }
    taskList.add(task);
  }
}
