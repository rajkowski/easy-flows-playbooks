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
package org.jeasy.flows.reader;

import java.util.List;

import org.jeasy.flows.playbook.Playbook;
import org.jeasy.flows.playbook.Task;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class YamlReaderTest {

  @Test
  void testRead() {
    String yaml = """
        id: blog_post_published
        name: Blog Post was published
        vars:
          api_key: "<YOUR_API_KEY>"
          api_key: "<DUPLICATE_API_KEY>"
        workflow:
        - when: "{{ blogPost.published != null }}"
        - history: "{{ blogPost.createdBy | name }} published a blog post: {{ blogPost.title }}"
        - email:
          subject: "New blog post: {{ blogPost.title }}"
          to: admins
          body: |
            A new blog post was just published by {{ blogPost.createdBy | name }}.

            Title: {{ blogPost.title }}
            Body:
            {{ blogPost.body | html_to_text }}
        """;

    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assertions.assertNotNull(playbook);
    Assertions.assertEquals("blog_post_published", playbook.getId());
    Assertions.assertEquals("Blog Post was published", playbook.getName());
    Assertions.assertEquals(1, playbook.getVars().size());
    Assertions.assertEquals("<DUPLICATE_API_KEY>", playbook.getVars().get("api_key"));
    Assertions.assertEquals(3, playbook.getTaskList().size());
    Assertions.assertEquals("admins", playbook.getTaskList().get(2).getVars().get("to"));
    Assertions.assertEquals("A new blog post was just published by {{ blogPost.createdBy | name }}.\n" +
        "\n" +
        "Title: {{ blogPost.title }}\n" +
        "Body:\n" +
        "{{ blogPost.body | html_to_text }}", playbook.getTaskList().get(2).getVars().get("body"));
  }

  @Test
  public void testConditionalRead() {
    String yaml = """
        ---
        id: conditional
        name: Conditional flow
        vars:
        workflow:
          - workItem1
          - set: condition1 = 0
          - block:
            - workItem2:
              repeat: 3
              when: '{{ condition1 < 0 }}'
          - block:
            - workItem3: hello
              when: '{{ condition1 >= 0}}'
            - workItem4: test
          - workItem5: finished
        """;

    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assertions.assertNotNull(playbook);
    Assertions.assertEquals("conditional", playbook.getId());
    Assertions.assertEquals("Conditional flow", playbook.getName());
    Assertions.assertEquals(0, playbook.getVars().size());
    Assertions.assertEquals(5, playbook.getTaskList().size());
    Assertions.assertEquals("workItem1", playbook.getTaskList().get(0).getId());
    Assertions.assertEquals("set", playbook.getTaskList().get(1).getId());
    Assertions.assertEquals("block", playbook.getTaskList().get(2).getId());
    Assertions.assertEquals("workItem2", playbook.getTaskList().get(2).getTasks().get(0).getId());
    Assertions.assertEquals("block", playbook.getTaskList().get(3).getId());
    Assertions.assertEquals("workItem3", playbook.getTaskList().get(3).getTasks().get(0).getId());
    Assertions.assertEquals("workItem4", playbook.getTaskList().get(3).getTasks().get(1).getId());
    Task block1 = playbook.getTaskList().get(3);
    Assertions.assertEquals(2, block1.getTaskList().size());
  }

  @Test
  void testSequentialRead() {
    String yaml = """
        id: sequential
        name: Sequential flow
        vars:
        workflow:
          - workItem1:
            repeat: 3
            delay: 10
          - workItem2
          - workItem3: something
        """;

    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assertions.assertNotNull(playbook);
    Assertions.assertEquals("sequential", playbook.getId());
    Assertions.assertEquals("Sequential flow", playbook.getName());
    Assertions.assertEquals(0, playbook.getVars().size());
    Assertions.assertEquals(3, playbook.getTaskList().size());
    Assertions.assertEquals("workItem1", playbook.getTaskList().get(0).getId());
    Assertions.assertEquals(3, playbook.getTaskList().get(0).getRepeat());
    Assertions.assertEquals(10, playbook.getTaskList().get(0).getDelay());
    Assertions.assertEquals("workItem2", playbook.getTaskList().get(1).getId());
    Assertions.assertEquals("workItem3", playbook.getTaskList().get(2).getId());
    Assertions.assertEquals("something", playbook.getTaskList().get(2).getData());
  }

  @Test
  void testParallelRead() {
    String yaml = """
        id: parallel
        name: Parallel flow
        vars:
        workflow:
          - task1:
          - parallel:
            timeout: 10
            when: '{{ something == else }}'
            tasks:
              - block:
                - workItem1:
                  when: '{{ websiteHits > 0 }}'
              - block:
                - workItem2:
          - workItem3
          - workItem4: 'some data'
        """;

    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assertions.assertNotNull(playbook);
    Assertions.assertEquals("parallel", playbook.getId());
    Assertions.assertEquals("Parallel flow", playbook.getName());
    Assertions.assertEquals(0, playbook.getVars().size());
    Assertions.assertEquals(4, playbook.getTaskList().size());
    Assertions.assertEquals("parallel", playbook.getTaskList().get(1).getId());
    Assertions.assertEquals(10, playbook.getTaskList().get(1).getTimeout());
    Assertions.assertEquals("workItem3", playbook.getTaskList().get(2).getId());
    Assertions.assertEquals("workItem4", playbook.getTaskList().get(3).getId());
    Assertions.assertNotNull(playbook.getTaskList().get(1).getTaskList());
    // A noop (parallel has 'when') and 2 blocks
    Assertions.assertEquals(3, playbook.getTaskList().get(1).getTaskList().size());
  }

  @Test
  void testReadMultiplePlaybooks() {
    String yaml = """
        # The playbook manager will execute flows for matching id's
        ---
        - id: blog-post-published
          workflow:
            - history:
              title: 'A blog post was published: {{ blogPost.title }}'
              user: '{{ user.id }}'
        - id: web-page-published
          workflow:
            - history:
              title: 'A web page was published: {{ webPage.title }}'
              user: '{{ user.id }}'
        - id: web-page-updated
          workflow:
            - history:
              title: 'A web page was updated: {{ webPage.title }}'
              user: '{{ user.id }}'
        """;

    List<Playbook> playbookList = YamlReader.readPlaybooks(yaml);
    Assertions.assertNotNull(playbookList);
    Assertions.assertEquals(3, playbookList.size());
    Assertions.assertEquals("blog-post-published", playbookList.get(0).getId());
    Assertions.assertEquals("web-page-published", playbookList.get(1).getId());
  }
}
