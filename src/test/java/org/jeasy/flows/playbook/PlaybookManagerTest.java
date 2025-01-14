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

import java.util.HashMap;
import java.util.Map;

import org.jeasy.flows.reader.YamlReader;
import org.jeasy.flows.work.LogTask;
import org.jeasy.flows.work.NoOpTask;
import org.jeasy.flows.work.SetTask;
import org.jeasy.flows.work.WorkReport;
import org.jeasy.flows.work.WorkStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PlaybookManagerTest {

  @Test
  void testSequentialRun() {
    String yaml = """
        id: sequential
        name: Sequential flow
        vars:
          log-message: 'this is the global message'
        workflow:
          - log: this is the data message
          - noop
          - log
          - log:
            message: 'this is an inline message'
        """;

    // Load the playbook
    Playbook playbook = YamlReader.readPlaybook(yaml);
    PlaybookManager.add(playbook);

    // Determine and cache the available task classes
    Map<String, String> taskLibrary = new HashMap<>();
    taskLibrary.put("noop", NoOpTask.class.getName());
    PlaybookManager.register(taskLibrary);

    // Execute the playbook
    WorkReport workReport = PlaybookManager.run(playbook.getId());
    Assertions.assertNotNull(workReport);
    Assertions.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
  }

  @Test
  void testParallelRun() {
    String yaml = """
        id: parallel
        name: Parallel flow
        vars:
        workflow:
          - log: Starting up
          - parallel:
            timeout: 10
            tasks:
              - log: print 1
              - log: print 2
              - log: print 3
              - log: print 4
          - log: Finished
        """;

    // Load the playbook
    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assertions.assertEquals("parallel", playbook.getId());
    PlaybookManager.add(playbook);

    // Determine and cache the available task classes
    Map<String, String> taskLibrary = new HashMap<>();
    taskLibrary.put("noop", NoOpTask.class.getName());
    PlaybookManager.register(taskLibrary);

    // Execute the playbook
    WorkReport workReport = PlaybookManager.run(playbook.getId());
    Assertions.assertNotNull(workReport);
    Assertions.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
  }

  @Test
  void testReadMeExample() {
    String yaml = """
        id: my-workflow
        name: Does something...
        workflow:
          - log: foo
            repeat: 3
          - parallel:
            threads: 2
            timeout: 5
            tasks:
              - log: hello
              - log: there
              - log: world
          - set: completed = true
          - log: ok
            when: completed == true
          - log: nok
            when: completed == false
        """;

    // Load the playbook(s)
    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assertions.assertEquals("my-workflow", playbook.getId());
    PlaybookManager.add(playbook);

    // Execute the playbook
    WorkReport workReport = PlaybookManager.run(playbook.getId());
    Assertions.assertNotNull(workReport);
    Assertions.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
  }

  @Test
  void testBlock1Example() {
    String yaml = """
        ---
        id: my-workflow
        name: Does something...
        workflow:
          - log: Repeating 3 times
            repeat: 3
          - set: condition1 = 0
          - block:
            - log: Repeating 2 times
              repeat: 2
              when: condition1 == 0
            - set: block1 = yes
          - block:
            - log: This block will be skipped
              when: 'condition1 == 1'
            - log: Must not be executed
            - set: block2 = yes
          - set: finished = yes
          - log: Finished
        """;

    // Load the playbook(s)
    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assertions.assertEquals("my-workflow", playbook.getId());
    PlaybookManager.add(playbook);

    Assertions.assertEquals(6, playbook.getTaskList().size());
    Assertions.assertEquals("block", playbook.getTaskList().get(2).getId());
    Assertions.assertEquals(2, playbook.getTaskList().get(2).getTasks().size());
    Assertions.assertEquals("log", playbook.getTaskList().get(2).getTasks().get(0).getId());
    Assertions.assertEquals("block", playbook.getTaskList().get(3).getId());
    Assertions.assertEquals(3, playbook.getTaskList().get(3).getTasks().size());
    Assertions.assertEquals("log", playbook.getTaskList().get(3).getTasks().get(0).getId());
    Assertions.assertEquals("log", playbook.getTaskList().get(5).getId());

    // Determine and cache the available task classes
    Map<String, String> taskLibrary = new HashMap<>();
    taskLibrary.put("set", SetTask.class.getName());
    taskLibrary.put("log", LogTask.class.getName());
    PlaybookManager.register(taskLibrary);

    // Execute the playbook
    WorkReport workReport = PlaybookManager.run(playbook.getId());
    Assertions.assertNotNull(workReport);
    Assertions.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
    Assertions.assertEquals("yes", workReport.getWorkContext().get("block1"));
    Assertions.assertNull(workReport.getWorkContext().get("block2"));
    Assertions.assertEquals("yes", workReport.getWorkContext().get("finished"));
  }

  @Test
  public void testBlock2Example() {
    String yaml = """
        ---
        id: my-workflow
        name: Does something...
        workflow:
          - log: Repeating 3 times
            repeat: 3
          - set: condition1 = 1
          - block:
            - log: Repeating 2 times
              repeat: 2
              when: condition1 == 0
            - set: block1 = yes
          - block:
            - log: This block will be executed
              when: 'condition1 == 1'
            - log: This is executed
            - set: block2 = yes
          - set: finished = yes
          - log: Finished
        """;

    // Load the playbook(s)
    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assertions.assertEquals("my-workflow", playbook.getId());
    PlaybookManager.add(playbook);

    Assertions.assertEquals(6, playbook.getTaskList().size());
    Assertions.assertEquals("block", playbook.getTaskList().get(2).getId());
    Assertions.assertEquals(2, playbook.getTaskList().get(2).getTasks().size());
    Assertions.assertEquals("log", playbook.getTaskList().get(2).getTasks().get(0).getId());
    Assertions.assertEquals("block", playbook.getTaskList().get(3).getId());
    Assertions.assertEquals(3, playbook.getTaskList().get(3).getTasks().size());
    Assertions.assertEquals("log", playbook.getTaskList().get(3).getTasks().get(0).getId());
    Assertions.assertEquals("log", playbook.getTaskList().get(5).getId());

    // Determine and cache the available task classes
    Map<String, String> taskLibrary = new HashMap<>();
    //    taskLibrary.put("set", SetTask.class.getName());
    //    taskLibrary.put("log", LogTask.class.getName());
    PlaybookManager.register(taskLibrary);

    // Execute the playbook
    WorkReport workReport = PlaybookManager.run(playbook.getId());
    Assertions.assertNotNull(workReport);
    Assertions.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
    Assertions.assertNull(workReport.getWorkContext().get("block1"));
    Assertions.assertEquals("yes", workReport.getWorkContext().get("block2"));
    Assertions.assertEquals("yes", workReport.getWorkContext().get("finished"));
  }

  @Test
  void testBlockWhenExample() {
    String yaml = """
        ---
        id: my-block-workflow
        name: Does something...
        workflow:
          - log: Repeating 3 times
            repeat: 3
          - set: condition1 = 1
          - block:
            when: condition1 == 1
            - log: Repeating 2 times
              repeat: 2
            - set: block1 = yes
          - block:
            when: condition1 == 0
            - log: THIS SHOULD NOT BE SHOWING
            - log: THIS SHOULD NOT BE SHOWING EITHER
            - set: block2 = yes
          - set: finished = yes
          - log: Finished
        """;

    // Load the playbook(s)
    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assertions.assertEquals("my-block-workflow", playbook.getId());
    PlaybookManager.add(playbook);

    Assertions.assertEquals(6, playbook.getTaskList().size());
    Assertions.assertEquals("block", playbook.getTaskList().get(2).getId());
    Assertions.assertEquals(3, playbook.getTaskList().get(2).getTasks().size());
    Assertions.assertEquals("log", playbook.getTaskList().get(2).getTasks().get(1).getId());
    Assertions.assertEquals("block", playbook.getTaskList().get(3).getId());
    Assertions.assertEquals(4, playbook.getTaskList().get(3).getTasks().size());
    Assertions.assertEquals("log", playbook.getTaskList().get(3).getTasks().get(1).getId());
    Assertions.assertEquals("log", playbook.getTaskList().get(5).getId());

    // Determine and cache the available task classes
    Map<String, String> taskLibrary = new HashMap<>();
    //taskLibrary.put("set", SetTask.class.getName());
    PlaybookManager.register(taskLibrary);

    // Execute the playbook
    WorkReport workReport = PlaybookManager.run(playbook.getId());
    Assertions.assertNotNull(workReport);
    Assertions.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
    Assertions.assertEquals("yes", workReport.getWorkContext().get("block1"));
    Assertions.assertNull(workReport.getWorkContext().get("block2"));
    Assertions.assertEquals("yes", workReport.getWorkContext().get("finished"));
  }
}
