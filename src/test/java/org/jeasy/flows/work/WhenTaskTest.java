package org.jeasy.flows.work;

import org.jeasy.flows.playbook.Playbook;
import org.jeasy.flows.playbook.PlaybookManager;
import org.jeasy.flows.playbook.Task;
import org.jeasy.flows.reader.YamlReader;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class WhenTaskTest {

  @Test
  public void testYamlExecute() {

    String arrayOfPlaybooks =
        "---\n" +
            "- id: form-submitted\n" +
            "  vars:\n" +
            "    formData: '{{ event.formData }}'\n" +
            "    location: '{{ event.location }}'\n" +
            "    emailTo: '{{ event.emailAddressesTo }}'\n" +
            "    generatedId: '{{ event.generatedId }}'\n" +
            "  workflow:\n" +
            "    - when: '{{ formData.flaggedAsSpam == false }}'\n" +
            "    - block:\n" +
            "      - when: '{{ emailTo != null && !\"\".equals(emailTo) }}'\n" +
            "      - log:\n" +
            "        toEmail: '{{ emailTo }}'\n" +
            "        message: 'Website {{ formData.formUniqueId }} form {{ generatedId }} submitted'\n" +
            "        template: 'cms/admin-form-submitted-notification'\n" +
            "    - block:\n" +
            "      - when: '{{ emailTo == null || \"\".equals(emailTo) }}'\n" +
            "      - log:\n" +
            "        toRole: 'admin'\n" +
            "        message: 'Website {{ formData.formUniqueId }} form {{ generatedId }} submitted'\n" +
            "        template: 'cms/admin-form-submitted-notification'\n" +
            "    - log: Finished\n";

    // Load the playbook
    Playbook playbook = YamlReader.readPlaybooks(arrayOfPlaybooks).get(0);

    // Verify the playbook
    Assert.assertEquals(4, playbook.getTaskList().size());
//    Assert.assertEquals("when", playbook.getTaskList().get(0).getName());
    Assert.assertEquals("block", playbook.getTaskList().get(1).getName());
    Assert.assertEquals("block", playbook.getTaskList().get(2).getName());

    PlaybookManager.add(playbook);

    Task task = new Task("form-submitted");
    task.addVar("name", "value");

    // Add any programmatic objects for use in the workflows
    WorkContext workContext = new WorkContext();

/*
    record FormData(String formUniqueId, boolean flaggedAsSpam) {}
    FormData formData = new FormData("my-unique-id", false);

    record Event(FormData formData, String location, String emailAddressesTo, String generatedId) { }
    Event eventObject = new Event(formData, "location-value", "email@example.com", "1234567890");

    Map<String, Object> starterObjectMap = new LinkedHashMap<>();
    starterObjectMap.put("event", eventObject);

  */

    Map<String, Object> formDataMap = new HashMap<>();
    formDataMap.put("flaggedAsSpam", false);
    formDataMap.put("formUniqueId", "my-unique-id");

    Map<String, Object> eventMap = new HashMap<>();
    eventMap.put("formData", formDataMap);
    eventMap.put("location", "location-value");
    eventMap.put("emailAddressesTo", "email@example.com");
    eventMap.put("generatedId", "1234567890");

    Map<String, Object> starterObjectMap = new LinkedHashMap<>();
    starterObjectMap.put("event", eventMap);


    Expression.applyVarExpressionsToWorkContext(playbook, workContext, starterObjectMap);


    // Execute the playbook
    WorkReport workReport = PlaybookManager.run(playbook.getId(), workContext);
    Assert.assertNotNull(workReport);
    Assert.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
  }

}
