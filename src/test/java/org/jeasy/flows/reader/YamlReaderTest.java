package org.jeasy.flows.reader;

import org.jeasy.flows.playbook.Playbook;
import org.jeasy.flows.playbook.Task;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class YamlReaderTest {

  @Test
  public void testRead() {
    String yaml =
        "id: blog_post_published\n" +
            "name: Blog Post was published\n" +
            "vars:\n" +
            "  api_key: \"<YOUR_API_KEY>\"\n" +
            "  api_key: \"<DUPLICATE_API_KEY>\"\n" +
            "workflow:\n" +
            "- something:\n" +
            "- something:\n" +
            "- condition: \"{{ blogPost.published }} != null\"\n" +
            "- history: \"{{ blogPost.createdBy | name }} published a blog post: {{ blogPost.title }}\"\n" +
            "- email:\n" +
            "  subject: \"New blog post: {{ blogPost.title }}\"\n" +
            "  to: admins\n" +
            "  body: |\n" +
            "    A new blog post was just published by {{ blogPost.createdBy | name }}.\n" +
            "\n" +
            "    Title: {{ blogPost.title }}\n" +
            "    Body:\n" +
            "    {{ blogPost.body | html_to_text }}";

    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assert.assertNotNull(playbook);
    Assert.assertEquals("blog_post_published", playbook.getId());
    Assert.assertEquals("Blog Post was published", playbook.getName());
    Assert.assertEquals(1, playbook.getVars().size());
    Assert.assertEquals("<DUPLICATE_API_KEY>", playbook.getVars().get("api_key"));
    Assert.assertEquals(5, playbook.getTaskList().size());
    Assert.assertEquals("admins", playbook.getTaskList().get(4).getVars().get("to"));
  }

  @Test
  public void testConditionalRead() {
    String yaml =
        "---\n" +
            "id: conditional\n" +
            "name: Conditional flow\n" +
            "vars:\n" +
            "workflow:\n" +
            "  - workItem1\n" +
            "  - set: condition1 = 0\n" +
            "  - block:\n" +
            "    - workItem2:\n" +
            "      repeat: 3\n" +
            "      when: '{{ condition1 }} < 0'\n" +
            "  - block:\n" +
            "    - workItem3: hello\n" +
            "      when: '{{ condition1 }} >= 0'\n" +
            "    - workItem4: test\n" +
            "  - workItem5: finished";

    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assert.assertNotNull(playbook);
    Assert.assertEquals("conditional", playbook.getId());
    Assert.assertEquals("Conditional flow", playbook.getName());
    Assert.assertEquals(0, playbook.getVars().size());
    Assert.assertEquals(5, playbook.getTaskList().size());
    Assert.assertEquals("workItem1", playbook.getTaskList().get(0).getId());
    Assert.assertEquals("set", playbook.getTaskList().get(1).getId());
    Assert.assertEquals("block", playbook.getTaskList().get(2).getId());
    Assert.assertEquals("workItem2", playbook.getTaskList().get(2).getTasks().get(0).getId());
    Assert.assertEquals("block", playbook.getTaskList().get(3).getId());
    Assert.assertEquals("workItem3", playbook.getTaskList().get(3).getTasks().get(0).getId());
    Assert.assertEquals("workItem4", playbook.getTaskList().get(3).getTasks().get(1).getId());
    Task block1 = playbook.getTaskList().get(3);
    Assert.assertEquals(2, block1.getTaskList().size());
  }

  @Test
  public void testSequentialRead() {
    String yaml =
        "id: sequential\n" +
            "name: Sequential flow\n" +
            "vars:\n" +
            "workflow:\n" +
            "  - workItem1:\n" +
            "    repeat: 3\n" +
            "    delay: 10\n" +
            "  - workItem2\n" +
            "  - workItem3: something";

    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assert.assertNotNull(playbook);
    Assert.assertEquals("sequential", playbook.getId());
    Assert.assertEquals("Sequential flow", playbook.getName());
    Assert.assertEquals(0, playbook.getVars().size());
    Assert.assertEquals(3, playbook.getTaskList().size());
    Assert.assertEquals("workItem1", playbook.getTaskList().get(0).getId());
    Assert.assertEquals(3, playbook.getTaskList().get(0).getRepeat());
    Assert.assertEquals(10, playbook.getTaskList().get(0).getDelay());
    Assert.assertEquals("workItem2", playbook.getTaskList().get(1).getId());
    Assert.assertEquals("workItem3", playbook.getTaskList().get(2).getId());
    Assert.assertEquals("something", playbook.getTaskList().get(2).getData());
  }

  @Test
  public void testParallelRead() {
    String yaml =
        "id: parallel\n" +
            "name: Parallel flow\n" +
            "vars:\n" +
            "workflow:\n" +
            "  - task1:\n" +
            "  - parallel:\n" +
            "    timeout: 10\n" +
            "    when: 'something == else'\n" +
            "    tasks:\n" +
            "      - block:\n" +
            "        - workItem1:\n" +
            "          when: '{{ websiteHits }} > 0'\n" +
            "      - block:\n" +
            "        - workItem2:\n" +
            "  - workItem3\n" +
            "  - workItem4: 'some data'\n";

    Playbook playbook = YamlReader.readPlaybook(yaml);
    Assert.assertNotNull(playbook);
    Assert.assertEquals("parallel", playbook.getId());
    Assert.assertEquals("Parallel flow", playbook.getName());
    Assert.assertEquals(0, playbook.getVars().size());
    Assert.assertEquals(4, playbook.getTaskList().size());

    // @todo check the block taskList

    for (Task task : playbook.getTaskList()) {
      System.out.println("Task: " + task.getId() + "=" + task.getData());
    }

    Assert.assertEquals("parallel", playbook.getTaskList().get(1).getId());
    Assert.assertEquals(10, playbook.getTaskList().get(1).getTimeout());
    Assert.assertEquals("workItem3", playbook.getTaskList().get(2).getId());
    Assert.assertEquals("workItem4", playbook.getTaskList().get(3).getId());
    Assert.assertNotNull(playbook.getTaskList().get(1).getTaskList());
    Assert.assertEquals(2, playbook.getTaskList().get(1).getTaskList().size());
  }

  @Test
  public void testReadMultiplePlaybooks() {
    String yaml =
        "# The playbook manager will execute flows for matching id's\n" +
            "---\n" +
            "- id: blog-post-published\n" +
            "  workflow:\n" +
            "    - history:\n" +
            "      title: 'A blog post was published: {{ blogPost.title }}'\n" +
            "      user: '{{ user.id }}'\n" +
            "- id: web-page-published\n" +
            "  workflow:\n" +
            "    - history:\n" +
            "      title: 'A web page was published: {{ webPage.title }}'\n" +
            "      user: '{{ user.id }}'\n" +
            "- id: web-page-updated\n" +
            "  workflow:\n" +
            "    - history:\n" +
            "      title: 'A web page was updated: {{ webPage.title }}'\n" +
            "      user: '{{ user.id }}'\n";

    List<Playbook> playbookList = YamlReader.readPlaybooks(yaml);
    Assert.assertNotNull(playbookList);
    Assert.assertEquals(3, playbookList.size());
    Assert.assertEquals("blog-post-published", playbookList.get(0).getId());
    Assert.assertEquals("web-page-published", playbookList.get(1).getId());
  }
}
