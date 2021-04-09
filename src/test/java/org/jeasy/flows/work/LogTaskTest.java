package org.jeasy.flows.work;

import org.jeasy.flows.playbook.Playbook;
import org.jeasy.flows.playbook.PlaybookManager;
import org.jeasy.flows.reader.YamlReader;
import org.junit.Assert;
import org.junit.Test;

public class LogTaskTest {

  @Test
  public void testYamlExecute() {
    String yaml =
        "---\n" +
            "id: expression-workflow\n" +
            "name: Does something...\n" +
            "workflow:\n" +
            "  - block:\n" +
            "    - when: person.age > 18\n" +
            "    - evaluate: person.setAdult(true);\n" +
            "  - block:\n" +
            "    - when: person.isAdult == true\n" +
            "    - log: '{{ person.name }} is an adult? {{ person.adult }}'\n" +
            "  - block:\n" +
            "    - when: person.isAdult == false\n" +
            "    - log: '{{ person.name }} is NOT an adult'\n" +
            "  - set: finished = yes\n" +
            "  - log: Finished\n";

    // Load the playbook
    Playbook playbook = YamlReader.readPlaybook(yaml);
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

}
