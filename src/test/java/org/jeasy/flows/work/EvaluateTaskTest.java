package org.jeasy.flows.work;

import org.assertj.core.api.Assertions;
import org.jeasy.flows.playbook.Playbook;
import org.jeasy.flows.playbook.PlaybookManager;
import org.jeasy.flows.reader.YamlReader;
import org.junit.Assert;
import org.junit.Test;

public class EvaluateTaskTest {

  @Test
  public void testYamlExecute() {
    String yaml =
        "---\n" +
            "id: expression-workflow\n" +
            "name: Does something...\n" +
            "workflow:\n" +
            "  - block:\n" +
            "    - when: '{{ person.age > 18 }}'\n" +
            "    - evaluate: '{{ person.setAdult(true); }}'\n" +
            "  - block:\n" +
            "    - when: '{{ person.isAdult == true }}'\n" +
            "    - log: Person is an adult\n" +
            "  - block:\n" +
            "    - when: '{{ person.isAdult == false }}'\n" +
            "    - log: Person is NOT an adult\n" +
            "  - set: 'finished = yes'\n" +
            "  - log: Finished\n";

    // Load the playbook
    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assert.assertEquals(5, playbook.getTaskList().size());
    PlaybookManager.add(playbook);

    // Add any programmatic objects for use in the workflows
    WorkContext workContext = new WorkContext();
    Person person = new Person("Name", 20);
    workContext.put("person", person);

    // Execute the playbook
    WorkReport workReport = PlaybookManager.run(playbook.getId(), workContext);
    Assert.assertNotNull(workReport);
    Assert.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
    Assert.assertTrue(person.isAdult());
  }

  @Test
  public void testExecute() {

    WhenTask whenTask = new WhenTask();
    EvaluateTask evaluateTask = new EvaluateTask();

    WorkContext workContext = new WorkContext();
    Person person = new Person("Name", 20);
    workContext.put("person", person);

    if (true) {
      TaskContext taskContext = new TaskContext(evaluateTask);
      taskContext.setData("person.age > 18");
      WorkReport workReport = whenTask.execute(workContext, taskContext);
      System.out.println(workReport.getStatus());
      Assertions.assertThat(workReport.getStatus()).isEqualTo(WorkStatus.COMPLETED);
    }
    if (true) {
      TaskContext taskContext = new TaskContext(evaluateTask);
      taskContext.setData("person.setAdult(true);");
      WorkReport workReport = evaluateTask.execute(workContext, taskContext);
      System.out.println(workReport.getStatus());
      Assertions.assertThat(workReport.getStatus()).isEqualTo(WorkStatus.COMPLETED);
    }

    Assert.assertTrue(person.isAdult());
  }

}
