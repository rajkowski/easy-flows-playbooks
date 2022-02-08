package org.jeasy.flows.reader;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class YamlLibraryTest {

  @Test
  public void testRead() {
    // Add task definitions
    String yaml = "---\n" +
        "- id: history\n" +
        "  name: Saves a history record to the repository\n" +
        "  class: com.example.HistoryTask\n" +
        "- id: email\n" +
        "  name: Sends an email\n" +
        "  class: com.example.EmailTask";

    Map<String, String> tasks = YamlReader.readTaskLibrary(yaml);
    Assert.assertNotNull(tasks);
    Assert.assertEquals(2, tasks.size());
  }
}
