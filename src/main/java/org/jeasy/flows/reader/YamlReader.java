package org.jeasy.flows.reader;

import org.jeasy.flows.playbook.Playbook;
import org.jeasy.flows.playbook.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * YamlReader reads and parses Yaml workflows
 *
 * @author matt rajkowski
 */
public class YamlReader {

  private static final Logger LOGGER = LoggerFactory.getLogger(YamlReader.class.getName());

  public static Map<String, String> readTaskLibrary(String yaml) {

    if (yaml == null) {
      LOGGER.error("YAML is null");
      return null;
    }
    if (!yaml.contains("---") && !yaml.contains("id:")) {
      LOGGER.error("Missing --- or id:");
      return null;
    }

    // Use an array to move forward and back
    String[] lines = yaml.split("\n\r|\r|\n");

    boolean foundStart = false;
    int counter = -1;
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      ++counter;
      if ("---".equals(line)) {
        foundStart = true;
        continue;
      }
      if (line.trim().length() == 0) {
        continue;
      }
      if (line.startsWith("#")) {
        continue;
      }
      if (!foundStart) {
        if (line.trim().startsWith("- ") || line.contains(":")) {
          foundStart = true;
        }
      }
      if (!foundStart) {
        continue;
      }
      break;
    }

    if (!foundStart) {
      LOGGER.error("Data not found.");
      return null;
    }

    List<Map<String, String>> taskList = new ArrayList<Map<String, String>>();
    Map<String, String> task = null;
    boolean foundTask = false;
    for (int i = counter; i < lines.length; i++) {

      // At this point, continue on with reading the lines until the end of file
      String line = lines[i];

      // It's a property for the current task
      String propertyName = termOf(line);
      String propertyValue = valueOf(line);

      if (!foundTask) {
        if (line.trim().startsWith("- ") || line.contains(":")) {
          task = new HashMap<>();
          task.put(propertyName, propertyValue);
          taskList.add(task);
          foundTask = true;
          continue;
        }
      }

      if (line.trim().startsWith("- ")) {
        // New entry
        task = new HashMap<>();
        task.put(propertyName, propertyValue);
        taskList.add(task);
        continue;
      }

      if (task == null) {
        continue;
      }

      if (line.contains(":")) {
        task.put(propertyName, propertyValue);
      }
    }

    Map<String, String> taskLibrary = new HashMap<>();
    for (Map<String, String> entry : taskList) {
      taskLibrary.put(entry.get("id"), entry.get("class"));
    }
    return taskLibrary;
  }

  public static Playbook readPlaybook(String yaml) {
    return readPlaybooks(yaml).get(0);
  }

  public static List<Playbook> readPlaybooks(String yaml) {
    if (yaml == null) {
      LOGGER.error("YAML is null");
      return null;
    }
    if (!yaml.contains("---") && !yaml.contains("id:")) {
      LOGGER.error("Missing --- or id:");
      return null;
    }

    List<Playbook> playbookList = new ArrayList<>();

    // Use an array to move forward and back
    String[] lines = yaml.split("\n\r|\r|\n");

    Playbook playbook = null;
    boolean foundStart = false;
    int lineIndent = 0;

    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      if ("---".equals(line)) {
        foundStart = true;
        continue;
      }
      if (line.trim().length() == 0) {
        continue;
      }
      if (line.startsWith("#")) {
        continue;
      }
      if (!foundStart) {
        if (line.trim().startsWith("- id:") || line.trim().startsWith("id:")) {
          foundStart = true;
        }
      }
      if (!foundStart) {
        continue;
      }

      // Start a new playbook
      if (line.contains("- id:") || line.startsWith("id:")) {
        playbook = new Playbook();
        lineIndent = line.indexOf("id:");
        String value = valueOf(line);
        playbook.setId(value);
        playbookList.add(playbook);
        LOGGER.debug("Found new playbook... " + value);
        continue;
      }
      // To continue, a playbook is needed
      if (playbook == null) {
        continue;
      }

      int thisIndent = line.indexOf(line.trim());
      if (thisIndent != lineIndent) {
        continue;
      }

      // Start processing the children terms...
      String term = termOf(line);
      String value = valueOf(line);

      if ("name".equals(term)) {
        if (value.length() > 0) {
          LOGGER.debug("Found name... " + value);
          playbook.setName(value);
        }
        continue;
      }

      if ("vars".equals(term)) {
        LOGGER.debug("Found vars...");
        while (true) {
          if (i + 1 == lines.length) {
            break;
          }
          // Grab the next line
          String nextLine = lines[i + 1];
          // Verify the next line is further indented
          int nextIndent = nextLine.indexOf(nextLine.trim());
          if (nextIndent <= thisIndent) {
            break;
          }
          // It is so grab the variable and mark the line processed
          if (nextLine.contains(":")) {
            String varName = termOf(nextLine);
            String varValue = valueOf(nextLine);
            playbook.addVar(varName, varValue);
            LOGGER.debug(" Added var: " + varName + "=" + varValue);
          }
          i++;
        }

      } else if ("workflow".equals(term)) {

        LOGGER.debug("Found workflow...");
        boolean inBlock = false;
        int blockIndent = -1;
        Task task = null;
        Task blockTask = null;

        Deque<Task> blockTaskQueue = new LinkedList<>();
        Deque<Integer> blockIndentQueue = new LinkedList<>();

        while (true) {
          if (i + 1 == lines.length) {
            break;
          }
          // Grab the next line
          String nextLine = lines[i + 1];
          // Verify the next line is further indented
          int nextIndent = nextLine.indexOf(nextLine.trim());
          if (nextIndent < thisIndent) {
            break;
          }

          if (nextLine.trim().startsWith("- ")) {

            if (inBlock) {
              while (nextIndent <= blockIndent) {
                blockIndentQueue.removeLast();
                Integer previousLevel = blockIndentQueue.peekLast();
                if (previousLevel == null) {
                  blockIndent = -1;
                } else {
                  blockIndent = previousLevel;
                }

                blockTaskQueue.removeLast();
                blockTask = blockTaskQueue.peekLast();
                inBlock = (blockTask != null);
              }
            }


            // It's a new task or block
            String taskName = termOf(nextLine);

            if ("parallel".equals(taskName)) {

              task = new Task("parallel");
              task.setName("parallel");
              playbook.add(task);

              inBlock = true;
              blockIndent = nextIndent;
              blockIndentQueue.addLast(blockIndent);
              blockTask = task;
              blockTaskQueue.addLast(blockTask);

              LOGGER.debug("Added new 'parallel block' to playbook: " + taskName);

            } else if ("block".equals(taskName)) {

              // A new block was found
              task = new Task("block");
              task.setName("block");

              if (inBlock) {
                blockTask.add(task);
                LOGGER.debug("Added new 'block' to existing block: " + blockTask.getName());
              } else {
                playbook.add(task);
                LOGGER.debug("Added new 'block' to playbook: " + taskName);
              }

              inBlock = true;
              blockIndent = nextIndent;
              blockIndentQueue.addLast(blockIndent);
              blockTask = task;
              blockTaskQueue.addLast(blockTask);

            } else {

              // A new task was found
              task = new Task(taskName);
              String taskValue = valueOf(nextLine);
              if (taskValue != null) {
                task.setData(taskValue);
              }
              if (inBlock) {
                LOGGER.debug("Added new 'task' to block: " + taskName);
                blockTask.add(task);
              } else {
                LOGGER.debug("Added new 'task' to playbook: " + taskName);
                playbook.add(task);
              }
            }

          } else if (task != null) {
            // It's a property for the current task
            String propertyName = termOf(nextLine);
            String propertyValue = valueOf(nextLine);

            // Apply special parameters
            if ("repeat".equals(propertyName)) {
              task.setRepeat(Long.parseLong(propertyValue));
            } else if ("threads".equals(propertyName)) {
              task.setThreads(Integer.parseInt(propertyValue));
            } else if ("timeout".equals(propertyName)) {
              task.setTimeout(Long.parseLong(propertyValue));
            } else if ("delay".equals(propertyName)) {
              task.setDelay(Long.parseLong(propertyValue));
            } else if ("tasks".equals(propertyName)) {
              // These are the tasks for a parallel task
              // grab them on the next pass
            } else {
              if ("when".equals(propertyName)) {
                LOGGER.debug("Set '" + task.getId() + "' when: " + propertyValue);
                if (inBlock &&
                    ("block".equals(task.getId()) || "parallel".equals(task.getId()))) {
                  Task noop = new Task("noop");
                  noop.setWhen(propertyValue);
                  blockTask.add(noop);
                } else {
                  task.setWhen(propertyValue);
                }
              } else {

                // Check if this is a multi-line value (uses \n or " ")
                if (nextLine.endsWith("|") || nextLine.endsWith(">")) {
                  // Lines must be indented OR blank to be added
                  boolean useSpace = false;
                  if (nextLine.endsWith(">")) {
                    useSpace = true;
                  }
                  StringBuilder sb = new StringBuilder();
                  for (int j = i + 2; j < lines.length; ++j) {
                    String testLine = lines[j];
                    int testIndent = testLine.indexOf(testLine.trim());
                    if (testIndent == 0 || testIndent > nextIndent) {
                      i++;
                      if (sb.length() > 0) {
                        sb.append(useSpace ? " " : "\n");
                      }
                      sb.append(testLine.trim());
                    }
                  }
                  propertyValue = sb.toString();
                  LOGGER.debug("Adding '" + task.getId() + "' property: " + propertyName + "=" + propertyValue);
                  task.addVar(propertyName, propertyValue);
                  break;

                } else {
                  // Add as a property as a variable
                  LOGGER.debug("Adding '" + task.getId() + "' property: " + propertyName + "=" + propertyValue);
                  task.addVar(propertyName, propertyValue);
                }
              }
            }
          }
          i++;
        }
      }

    }
    return playbookList;
  }

  private static String termOf(String wholeLine) {
    if (wholeLine == null) {
      return null;
    }
    String term = wholeLine.trim();
    if (term.contains(":")) {
      term = term.substring(0, term.indexOf(":")).trim();
    }
    if (term.startsWith("- ")) {
      return term.substring(2).trim();
    }
    return term;
  }

  private static String valueOf(String wholeLine) {
    if (wholeLine == null) {
      return null;
    }
    if (!wholeLine.contains(":")) {
      return null;
    }
    String value = wholeLine.substring(wholeLine.indexOf(":") + 1).trim();
    if (value.length() == 0) {
      return null;
    }
    // Strip one occurrence of quotes
    if (value.length() > 1 &&
        (value.startsWith("'") && value.endsWith("'")) ||
        (value.startsWith("\"") && value.endsWith("\""))) {
      return value.substring(1, value.length() - 1);
    }
    return value;
  }
}
