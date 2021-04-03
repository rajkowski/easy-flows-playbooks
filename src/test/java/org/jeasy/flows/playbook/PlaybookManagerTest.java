package org.jeasy.flows.playbook;

import org.jeasy.flows.reader.YamlReader;
import org.jeasy.flows.work.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class PlaybookManagerTest {

  @Test
  public void testSequentialRun() {
    String yaml =
        "id: sequential\n" +
            "name: Sequential flow\n" +
            "vars:\n" +
            "  log-message: 'this is the global message'\n" +
            "workflow:\n" +
            "  - log: this is the data message\n" +
            "  - noop\n" +
            "  - log\n" +
            "  - log:\n" +
            "    message: 'this is an inline message'\n";

    // Load the playbook
    Playbook playbook = YamlReader.readPlaybook(yaml);
    PlaybookManager.add(playbook);

    // Determine and cache the available task classes
    Map<String, String> taskLibrary = new HashMap<>();
    taskLibrary.put("noop", NoOpTask.class.getName());
    PlaybookManager.register(taskLibrary);

    // Add any programmatic objects for use in the workflows
    WorkContext workContext = new WorkContext();

    // Execute the playbook
    WorkReport workReport = PlaybookManager.run(playbook.getId(), workContext);
    Assert.assertNotNull(workReport);
    Assert.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
  }

  @Test
  public void testParallelRun() {
    String yaml =
        "id: parallel\n" +
            "name: Parallel flow\n" +
            "vars:\n" +
            "workflow:\n" +
            "  - log: Starting up\n" +
            "  - parallel:\n" +
            "    timeout: 10\n" +
            "    tasks:\n" +
            "      - log: print 1\n" +
            "      - log: print 2\n" +
            "      - log: print 3\n" +
            "      - log: print 4\n" +
            "  - log: Finished\n";

    // Load the playbook
    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assert.assertEquals("parallel", playbook.getId());
    PlaybookManager.add(playbook);

    // Determine and cache the available task classes
    Map<String, String> taskLibrary = new HashMap<>();
    taskLibrary.put("noop", NoOpTask.class.getName());
    PlaybookManager.register(taskLibrary);

    // Execute the playbook
    WorkReport workReport = PlaybookManager.run(playbook.getId());
    Assert.assertNotNull(workReport);
    Assert.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
  }

  @Test
  public void testReadMeExample() {
    String yaml =
        "id: my-workflow\n" +
            "name: Does something...\n" +
            "workflow:\n" +
            "  - log: foo\n" +
            "    repeat: 3\n" +
            "  - parallel:\n" +
            "    threads: 2\n" +
            "    timeout: 3\n" +
            "    tasks:\n" +
            "      - log: hello\n" +
//            "        repeat: 4\n" +
            "      - log: there\n" +
            "      - log: world\n" +
            "  - set: completed = true\n" +
            "  - log: ok\n" +
            "    when: completed = true\n" +
            "  - log: nok\n" +
            "    when: completed = false\n";

    // Load the playbook(s)
    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assert.assertEquals("my-workflow", playbook.getId());
    PlaybookManager.add(playbook);

    // Execute the playbook
    WorkReport workReport = PlaybookManager.run(playbook.getId());
    Assert.assertNotNull(workReport);
    Assert.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
  }

  @Test
  public void testBlock1Example() {
    String yaml =
        "---\n" +
        "id: my-workflow\n" +
            "name: Does something...\n" +
            "workflow:\n" +
            "  - log: Repeating 3 times\n" +
            "    repeat: 3\n" +
            "  - set: condition1 = 0\n" +
            "  - block:\n" +
            "    - log: Repeating 2 times\n" +
            "      repeat: 2\n" +
            "      when: condition1 = 0\n" +
            "    - set: block1 = yes\n" +
            "  - block:\n" +
            "    - log: This block will be skipped\n" +
            "      when: 'condition1 = 1'\n" +
            "    - log: Must not be executed\n" +
            "    - set: block2 = yes\n" +
            "  - set: finished = yes\n" +
            "  - log: Finished\n";

    // Load the playbook(s)
    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assert.assertEquals("my-workflow", playbook.getId());
    PlaybookManager.add(playbook);

    Assert.assertEquals(6, playbook.getTaskList().size());
    Assert.assertEquals("block", playbook.getTaskList().get(2).getId());
    Assert.assertEquals(2, playbook.getTaskList().get(2).getTasks().size());
    Assert.assertEquals("log", playbook.getTaskList().get(2).getTasks().get(0).getId());
    Assert.assertEquals("block", playbook.getTaskList().get(3).getId());
    Assert.assertEquals(3, playbook.getTaskList().get(3).getTasks().size());
    Assert.assertEquals("log", playbook.getTaskList().get(3).getTasks().get(0).getId());
    Assert.assertEquals("log", playbook.getTaskList().get(5).getId());

    // Determine and cache the available task classes
    Map<String, String> taskLibrary = new HashMap<>();
    taskLibrary.put("set", SetTask.class.getName());
    taskLibrary.put("log", LogTask.class.getName());
    PlaybookManager.register(taskLibrary);

    // Execute the playbook
    WorkReport workReport = PlaybookManager.run(playbook.getId());
    Assert.assertNotNull(workReport);
    Assert.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
    Assert.assertEquals("yes", workReport.getWorkContext().get("block1"));
    Assert.assertNull(workReport.getWorkContext().get("block2"));
    Assert.assertEquals("yes", workReport.getWorkContext().get("finished"));
  }

  @Test
  public void testBlock2Example() {
    String yaml =
        "---\n" +
            "id: my-workflow\n" +
            "name: Does something...\n" +
            "workflow:\n" +
            "  - log: Repeating 3 times\n" +
            "    repeat: 3\n" +
            "  - set: condition1 = 1\n" +
            "  - block:\n" +
            "    - log: Repeating 2 times\n" +
            "      repeat: 2\n" +
            "      when: condition1 = 0\n" +
            "    - set: block1 = yes\n" +
            "  - block:\n" +
            "    - log: This block will be executed\n" +
            "      when: 'condition1 = 1'\n" +
            "    - log: This is executed\n" +
            "    - set: block2 = yes\n" +
            "  - set: finished = yes\n" +
            "  - log: Finished\n";

    // Load the playbook(s)
    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assert.assertEquals("my-workflow", playbook.getId());
    PlaybookManager.add(playbook);

    Assert.assertEquals(6, playbook.getTaskList().size());
    Assert.assertEquals("block", playbook.getTaskList().get(2).getId());
    Assert.assertEquals(2, playbook.getTaskList().get(2).getTasks().size());
    Assert.assertEquals("log", playbook.getTaskList().get(2).getTasks().get(0).getId());
    Assert.assertEquals("block", playbook.getTaskList().get(3).getId());
    Assert.assertEquals(3, playbook.getTaskList().get(3).getTasks().size());
    Assert.assertEquals("log", playbook.getTaskList().get(3).getTasks().get(0).getId());
    Assert.assertEquals("log", playbook.getTaskList().get(5).getId());

    // Determine and cache the available task classes
    Map<String, String> taskLibrary = new HashMap<>();
//    taskLibrary.put("set", SetTask.class.getName());
//    taskLibrary.put("log", LogTask.class.getName());
    PlaybookManager.register(taskLibrary);

    // Execute the playbook
    WorkReport workReport = PlaybookManager.run(playbook.getId());
    Assert.assertNotNull(workReport);
    Assert.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
    Assert.assertNull(workReport.getWorkContext().get("block1"));
    Assert.assertEquals("yes", workReport.getWorkContext().get("block2"));
    Assert.assertEquals("yes", workReport.getWorkContext().get("finished"));
  }

  @Test
  public void testBlockWhenExample() {
    String yaml =
        "---\n" +
            "id: my-block-workflow\n" +
            "name: Does something...\n" +
            "workflow:\n" +
            "  - log: Repeating 3 times\n" +
            "    repeat: 3\n" +
            "  - set: condition1 = 1\n" +
            "  - block:\n" +
            "    when: condition1 = 1\n" +
            "    - log: Repeating 2 times\n" +
            "      repeat: 2\n" +
            "    - set: block1 = yes\n" +
            "  - block:\n" +
            "    when: condition1 = 0\n" +
            "    - log: This block will not be executed\n" +
            "    - log: This is not executed\n" +
            "    - set: block2 = yes\n" +
            "  - set: finished = yes\n" +
            "  - log: Finished\n";

    // Load the playbook(s)
    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assert.assertEquals("my-block-workflow", playbook.getId());
    PlaybookManager.add(playbook);

    Assert.assertEquals(6, playbook.getTaskList().size());
    Assert.assertEquals("block", playbook.getTaskList().get(2).getId());
    Assert.assertEquals(3, playbook.getTaskList().get(2).getTasks().size());
    Assert.assertEquals("log", playbook.getTaskList().get(2).getTasks().get(1).getId());
    Assert.assertEquals("block", playbook.getTaskList().get(3).getId());
    Assert.assertEquals(4, playbook.getTaskList().get(3).getTasks().size());
    Assert.assertEquals("log", playbook.getTaskList().get(3).getTasks().get(1).getId());
    Assert.assertEquals("log", playbook.getTaskList().get(5).getId());

    // Determine and cache the available task classes
    Map<String, String> taskLibrary = new HashMap<>();
    //taskLibrary.put("set", SetTask.class.getName());
    PlaybookManager.register(taskLibrary);

    // Execute the playbook
    WorkReport workReport = PlaybookManager.run(playbook.getId());
    Assert.assertNotNull(workReport);
    Assert.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
    Assert.assertEquals("yes", workReport.getWorkContext().get("block1"));
    Assert.assertNull(workReport.getWorkContext().get("block2"));
    Assert.assertEquals("yes", workReport.getWorkContext().get("finished"));
  }
}
