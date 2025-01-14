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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jeasy.flows.playbook.Playbook;
import org.jeasy.flows.playbook.PlaybookManager;
import org.jeasy.flows.playbook.Task;
import org.jeasy.flows.reader.YamlReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WhenTaskTest {

  @Test
  void testYamlExecute() {

    String arrayOfPlaybooks = """
        ---
        - id: form-submitted
          vars:
            formData: '{{ event.formData }}'
            location: '{{ event.location }}'
            emailTo: '{{ event.emailAddressesTo }}'
            generatedId: '{{ event.generatedId }}'
          workflow:
            - when: '{{ formData.flaggedAsSpam == false }}'
            - block:
              - when: '{{ emailTo != null && !\"\".equals(emailTo) }}'
              - log:
                toEmail: '{{ emailTo }}'
                message: 'Website {{ formData.formUniqueId }} form {{ generatedId }} submitted'
                template: 'cms/admin-form-submitted-notification'
            - block:
              - when: '{{ emailTo == null || \"\".equals(emailTo) }}'
              - log:
                toRole: 'admin'
                message: 'Website {{ formData.formUniqueId }} form {{ generatedId }} submitted'
                template: 'cms/admin-form-submitted-notification'
            - log: Finished
        """;

    // Load the playbook
    Playbook playbook = YamlReader.readPlaybooks(arrayOfPlaybooks).get(0);

    // Verify the playbook
    Assertions.assertEquals(4, playbook.getTaskList().size());
    // Assert.assertEquals("when", playbook.getTaskList().get(0).getName());
    Assertions.assertEquals("block", playbook.getTaskList().get(1).getName());
    Assertions.assertEquals("block", playbook.getTaskList().get(2).getName());

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
    Assertions.assertNotNull(workReport);
    Assertions.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
  }

}
