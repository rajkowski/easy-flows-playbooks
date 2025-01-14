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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jeasy.flows.playbook.Playbook;

/**
 * Work execution context. This can be used to pass initial parameters to the
 * workflow and share data between work units.
 *
 * <strong>Work context instances are thread-safe.</strong>
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public class WorkContext {

  private final Map<String, Object> context = new ConcurrentHashMap<>();

  public WorkContext() {
  }

  public WorkContext(Playbook playbook) {
    put(playbook.getVars());
  }

  public void put(String key, Object value) {
    if (value != null) {
      context.put(key, value);
    }
  }

  public void put(Map<String, Object> vars) {
    if (vars == null || vars.isEmpty()) {
      return;
    }
    for (String var : vars.keySet()) {
      if (!context.containsKey(var)) {
        context.put(var, vars.get(var));
      }
    }
  }

  public Object get(String key) {
    return context.get(key);
  }

  public Set<Map.Entry<String, Object>> getEntrySet() {
    return context.entrySet();
  }

  public Map<String, Object> getMap() {
    return context;
  }

  public boolean containsKey(String key) {
    return context.containsKey(key);
  }

  @Override
  public String toString() {
    return "context=" + context + '}';
  }
}
