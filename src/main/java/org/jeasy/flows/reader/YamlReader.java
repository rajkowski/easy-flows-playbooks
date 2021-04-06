package org.jeasy.flows.reader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.jeasy.flows.playbook.Playbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * YamlReader reads and parses Yaml workflows
 *
 * @author matt rajkowski
 */
public class YamlReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(YamlReader.class.getName());

  public static Map<String, String> readTaskLibrary(String yaml) {
    try {
      YAMLFactory factory = new YAMLFactory();
      ObjectMapper mapper = new ObjectMapper(factory);
      List<Map<String, String>> tasks = mapper.readValue(yaml, new TypeReference<List<Map<String, String>>>(){});
      Map<String, String> taskLibrary = new HashMap<>();
      for (Map<String, String> entry : tasks) {
        taskLibrary.put(entry.get("id"), entry.get("class"));
      }
      return taskLibrary;
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      return null;
    }
  }

  public static List<Playbook> readPlaybooks(String yaml) {
    if (yaml == null) {
      return null;
    }
    try {
      YAMLFactory factory = new YAMLFactory();
      ObjectMapper mapper = new ObjectMapper(factory);
      List<Playbook> asList = mapper.readValue(yaml, new TypeReference<List<Playbook>>() {
      });
      return asList;
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      return null;
    }
  }

  public static Playbook readPlaybook(String yaml) {
    if (yaml == null) {
      return null;
    }
    if (yaml.contains("- block:")) {
      yaml = parseBlocks(yaml);
    }
    if (yaml.contains("- parallel:")) {
      // Handle parallel blocks differently
      return parseParallelWorkflow(yaml);
    } else {
      return parseCompleteWorkflow(yaml);
    }
  }

  private static Playbook parseCompleteWorkflow(String yaml) {
    try {
      YAMLFactory factory = new YAMLFactory();
      ObjectMapper mapper = new ObjectMapper(factory);
      return mapper.readValue(yaml, Playbook.class);
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      return null;
    }
  }

  private static Playbook parseParallelWorkflow(String yaml) {
    // Do a line reader, extract when parallel tasks start (tasks:) and when they end
    // (eof or indent level is same or less than - parallel)
    StringBuilder yamlModified = new StringBuilder();
    List<String> parallelTasks = new ArrayList<>();

    try (BufferedReader reader = new BufferedReader(new StringReader(yaml))) {
      boolean inParallelSection = false;
      boolean inParallelTasks = false;
      int parallelIndentIdx = -1;
      StringBuilder currentInlineTasks = null;
      String line = reader.readLine();
      while (line != null) {
        if (inParallelSection) {
          // check for parallel 'tasks'
          if (line.contains(" tasks:")) {
            inParallelTasks = true;
          }
          // check for end of parallel section
          if ((line.length() > (parallelIndentIdx + 1)) && line.charAt(parallelIndentIdx) == '-') {
            inParallelTasks = false;
            inParallelSection = false;
          }
        }

        if (!inParallelTasks) {
          // Treat this in the original workflow
          yamlModified.append(line).append(System.lineSeparator());
        } else {
          // Treat this as the inline tasks
          currentInlineTasks.append(line).append(System.lineSeparator());
        }

        // Determine if a parallel section has been found
        if (line.contains("- parallel:")) {
          inParallelSection = true;
          parallelIndentIdx = line.indexOf("- parallel:");
          if (currentInlineTasks != null) {
            parallelTasks.add(currentInlineTasks.toString());
          }
          currentInlineTasks = new StringBuilder();
        }

        // Continue with the next line
        line = reader.readLine();
      }
      if (currentInlineTasks != null && currentInlineTasks.length() > 0) {
        parallelTasks.add(currentInlineTasks.toString());
      }
    } catch (Exception e) {
      return null;
    }

    LOGGER.debug("Will process:\n---\n" + yamlModified.toString());

    Playbook playbook = null;
    try {
      // Process all the YAML into a playbook
      YAMLFactory factory = new YAMLFactory();
      ObjectMapper mapper = new ObjectMapper(factory);
      // Start with the main YAML
      playbook = mapper.readValue(yamlModified.toString(), Playbook.class);
      // Now review the parallel tasks
      int i = -1;
      for (String parallelTasksValue : parallelTasks) {
        LOGGER.debug("Parallel:\n---\n" + parallelTasksValue);
        i++;
        // Populate the parallel tasks, and combine with the created playbook
        Playbook thisPlaybook = mapper.readValue(parallelTasksValue, Playbook.class);
        playbook.getTaskList().getParallelTask(i).setTaskList(thisPlaybook.getTaskList());
      }
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      return null;
    }
    return playbook;
  }

  private static String parseBlocks(String yaml) {
    // Do a line reader, check if blocks use a 'when' which doesn't deserialize yet
    StringBuilder yamlModified = new StringBuilder();

    try (BufferedReader reader = new BufferedReader(new StringReader(yaml))) {

      boolean inBlockSection = false;
      boolean inBlockTasks = false;
      int blockIndentIdx = -1;

      String line = reader.readLine();
      while (line != null) {

        if (line.length() < 2) {
          // Skip it
          line = reader.readLine();
          continue;
        }
        if (line.startsWith("---")) {
          // Start over with the content
          yamlModified = new StringBuilder();
          line = reader.readLine();
          continue;
        }

        if (inBlockSection) {
          if (line.length() > blockIndentIdx) {
            if (line.substring(blockIndentIdx).startsWith("  when:")) {
              // rewrite the line
              line = line.replace("  when:", "  - when:");
            } else if (line.substring(blockIndentIdx).startsWith("- ")) {
              inBlockSection = false;
            }
          }
        }

        // Determine if a block section has been found
        if (line.contains("- block:")) {
          inBlockSection = true;
          blockIndentIdx = line.indexOf("- block:");
        }

        // Save and continue
        yamlModified.append(line).append(System.lineSeparator());
        line = reader.readLine();
      }
    } catch (Exception e) {
      return null;
    }

    return yamlModified.toString();
  }
}
