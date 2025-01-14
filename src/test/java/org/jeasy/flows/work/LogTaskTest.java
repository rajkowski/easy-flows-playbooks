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

import org.jeasy.flows.playbook.Playbook;
import org.jeasy.flows.playbook.PlaybookManager;
import org.jeasy.flows.reader.YamlReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LogTaskTest {

  @Test
  void testYamlExecute() {
    String yaml = """
        ---
        id: expression-workflow
        name: Does something...
        workflow:
          - block:
            - when: person.age > 18
            - evaluate: person.setAdult(true);
          - block:
            - when: person.isAdult == true
            - log: '{{ person.name }} is an adult? {{ person.adult }}'
          - block:
            - when: person.isAdult == false
            - log: '{{ person.name }} is NOT an adult'
          - set: finished = yes
          - log: Finished
        """;

    // Load the playbook
    Playbook playbook = YamlReader.readPlaybook(yaml);
    PlaybookManager.add(playbook);

    // Add any programmatic objects for use in the workflows
    WorkContext workContext = new WorkContext();
    Person person = new Person("Name", 20);
    workContext.put("person", person);

    // Execute the playbook
    WorkReport workReport = PlaybookManager.run(playbook.getId(), workContext);
    Assertions.assertNotNull(workReport);
    Assertions.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
    Assertions.assertTrue(person.isAdult());
  }

}
