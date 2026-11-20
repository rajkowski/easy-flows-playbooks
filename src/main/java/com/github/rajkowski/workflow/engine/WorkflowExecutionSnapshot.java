/*
 * The MIT License
 *
 *  Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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
package com.github.rajkowski.workflow.engine;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.rajkowski.workflow.flow.WorkFlow;
import com.github.rajkowski.workflow.playbook.Playbook;
import com.github.rajkowski.workflow.playbook.Task;
import com.github.rajkowski.workflow.work.WorkContext;
import com.github.rajkowski.workflow.work.WorkStatus;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Serializable view of a workflow execution that can be transferred to an external store and used to resume later.
 */
public class WorkflowExecutionSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

    private final String executionId;
    private final String workflowName;
    private final WorkStatus status;
    private final WorkContext workContext;
    private final transient WorkFlow workFlow;
    private final Playbook playbook;
    private final String pausedTaskId;

    public WorkflowExecutionSnapshot(String executionId, String workflowName, WorkStatus status,
            WorkContext workContext,
            WorkFlow workFlow) {
        this(executionId, workflowName, status, workContext, workFlow, null, null);
    }

    public WorkflowExecutionSnapshot(String executionId, String workflowName, WorkStatus status,
            WorkContext workContext,
            WorkFlow workFlow, Playbook playbook, String pausedTaskId) {
        this.executionId = executionId;
        this.workflowName = workflowName;
        this.status = status;
        this.workContext = workContext;
        this.workFlow = workFlow;
        this.playbook = playbook;
        this.pausedTaskId = pausedTaskId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public WorkStatus getStatus() {
        return status;
    }

    public WorkContext getWorkContext() {
        return workContext;
    }

    public WorkFlow getWorkFlow() {
        return workFlow;
    }

    /**
     * Get the playbook structure captured at snapshot time, as a JSON-serializable representation, so a
     * resumed execution continues against the exact same task list the paused run was using.
     *
     * @return the captured playbook, or {@code null} when not tracked
     */
    public Playbook getPlaybook() {
        return playbook;
    }

    /**
     * Get the hierarchical element id (e.g. "2.1") of the playbook task that was paused/waiting, so
     * resuming can continue right after it, even when it is nested inside a block or parallel task.
     *
     * @return the task's element id, or {@code null} when not tracked
     */
    public String getPausedTaskId() {
        return pausedTaskId;
    }

    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(new SnapshotData(executionId, workflowName, status,
                    workContext == null ? null : encodeWorkContext(workContext.getMap()), encodePlaybook(playbook),
                    pausedTaskId));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize workflow execution snapshot to JSON", e);
        }
    }

    public static WorkflowExecutionSnapshot fromJson(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("Workflow execution snapshot JSON cannot be null or blank");
        }
        try {
            SnapshotData snapshotData = OBJECT_MAPPER.readValue(json, SnapshotData.class);
            WorkContext context = new WorkContext();
            if (snapshotData.workContext != null) {
                context.put(decodeWorkContext(snapshotData.workContext));
            }
            Playbook decodedPlaybook = decodePlaybook(snapshotData.playbook);
            return new WorkflowExecutionSnapshot(snapshotData.executionId, snapshotData.workflowName,
                    snapshotData.status, context, null, decodedPlaybook, snapshotData.pausedTaskId);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to deserialize workflow execution snapshot from JSON", e);
        }
    }

    private static Map<String, Object> encodePlaybook(Playbook source) {
        if (source == null) {
            return null;
        }
        Map<String, Object> encoded = new LinkedHashMap<>();
        encoded.put("id", source.getId());
        encoded.put("name", source.getName());
        encoded.put("vars", source.getVars());
        encoded.put("taskList", encodeTaskList(source.getTaskList()));
        return encoded;
    }

    private static List<Object> encodeTaskList(List<Task> source) {
        if (source == null) {
            return null;
        }
        List<Object> encoded = new ArrayList<>();
        for (Task task : source) {
            encoded.add(encodeTask(task));
        }
        return encoded;
    }

    private static Map<String, Object> encodeTask(Task source) {
        Map<String, Object> encoded = new LinkedHashMap<>();
        encoded.put("id", source.getId());
        encoded.put("name", source.getName());
        encoded.put("data", source.getData());
        encoded.put("vars", source.getVars());
        encoded.put("when", source.getWhen());
        encoded.put("delay", source.getDelay());
        encoded.put("repeat", source.getRepeat());
        encoded.put("timeout", source.getTimeout());
        encoded.put("threads", source.getThreads());
        encoded.put("taskList", encodeTaskList(source.getTaskList()));
        return encoded;
    }

    private static Playbook decodePlaybook(Map<String, Object> source) {
        if (source == null) {
            return null;
        }
        Playbook playbook = new Playbook();
        playbook.setId((String) source.get("id"));
        playbook.setName((String) source.get("name"));
        playbook.setVars(asStringKeyedMap(source.get("vars")));
        for (Task task : decodeTaskList(source.get("taskList"))) {
            playbook.add(task);
        }
        return playbook;
    }

    private static List<Task> decodeTaskList(Object source) {
        List<Task> decoded = new ArrayList<>();
        if (source instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    decoded.add(decodeTask(asStringKeyedMap(map)));
                }
            }
        }
        return decoded;
    }

    private static Task decodeTask(Map<String, Object> source) {
        Task task = new Task();
        task.setId((String) source.get("id"));
        task.setName((String) source.get("name"));
        task.setData((String) source.get("data"));
        task.setVars(asStringKeyedMap(source.get("vars")));
        task.setWhen((String) source.get("when"));
        task.setDelay(asLong(source.get("delay")));
        task.setRepeat(asLong(source.get("repeat")));
        task.setTimeout(asLong(source.get("timeout")));
        task.setThreads(asInt(source.get("threads")));
        for (Task subTask : decodeTaskList(source.get("taskList"))) {
            task.add(subTask);
        }
        return task;
    }

    private static Map<String, Object> asStringKeyedMap(Object value) {
        if (!(value instanceof Map<?, ?> map) || map.isEmpty()) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private static long asLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private static int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static Map<String, Object> encodeWorkContext(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        Map<String, Object> encoded = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            encoded.put(entry.getKey(), encodeValue(entry.getValue()));
        }
        return encoded;
    }

    private static Map<String, Object> decodeWorkContext(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        Map<String, Object> decoded = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            decoded.put(entry.getKey(), decodeValue(entry.getValue()));
        }
        return decoded;
    }

    private static Object encodeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean
                || value instanceof Character) {
            return value;
        }
        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> encoded = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                encoded.put(String.valueOf(entry.getKey()), encodeValue(entry.getValue()));
            }
            return encoded;
        }
        if (value instanceof Iterable<?> iterable) {
            List<Object> encoded = new ArrayList<>();
            for (Object item : iterable) {
                encoded.add(encodeValue(item));
            }
            return encoded;
        }
        if (value.getClass().isArray()) {
            List<Object> encoded = new ArrayList<>();
            int length = java.lang.reflect.Array.getLength(value);
            for (int i = 0; i < length; i++) {
                encoded.add(encodeValue(java.lang.reflect.Array.get(value, i)));
            }
            return encoded;
        }

        Map<String, Object> beanProperties = OBJECT_MAPPER.convertValue(value, Map.class);
        Map<String, Object> typedValue = new LinkedHashMap<>();
        typedValue.put("@type", value.getClass().getName());
        typedValue.put("value", encodeValue(beanProperties));
        return typedValue;
    }

    private static Object decodeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            if (map.containsKey("@type") && map.containsKey("value")) {
                String typeName = String.valueOf(map.get("@type"));
                Object typedValue = decodeValue(map.get("value"));
                try {
                    Class<?> type = Class.forName(typeName);
                    return OBJECT_MAPPER.convertValue(typedValue, type);
                } catch (ClassNotFoundException e) {
                    return typedValue;
                }
            }
            Map<String, Object> decoded = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                decoded.put(String.valueOf(entry.getKey()), decodeValue(entry.getValue()));
            }
            return decoded;
        }
        if (value instanceof List<?> list) {
            List<Object> decoded = new ArrayList<>();
            for (Object item : list) {
                decoded.add(decodeValue(item));
            }
            return decoded;
        }
        return value;
    }

    private static class SnapshotData {
        private String executionId;
        private String workflowName;
        private WorkStatus status;
        private Map<String, Object> workContext;
        private Map<String, Object> playbook;
        private String pausedTaskId;

        private SnapshotData() {
        }

        private SnapshotData(String executionId, String workflowName, WorkStatus status,
                Map<String, Object> workContext, Map<String, Object> playbook,
                String pausedTaskId) {
            this.executionId = executionId;
            this.workflowName = workflowName;
            this.status = status;
            this.workContext = workContext;
            this.playbook = playbook;
            this.pausedTaskId = pausedTaskId;
        }

        public String getExecutionId() {
            return executionId;
        }

        public void setExecutionId(String executionId) {
            this.executionId = executionId;
        }

        public String getWorkflowName() {
            return workflowName;
        }

        public void setWorkflowName(String workflowName) {
            this.workflowName = workflowName;
        }

        public WorkStatus getStatus() {
            return status;
        }

        public void setStatus(WorkStatus status) {
            this.status = status;
        }

        public Map<String, Object> getWorkContext() {
            return workContext;
        }

        public void setWorkContext(Map<String, Object> workContext) {
            this.workContext = workContext;
        }

        public Map<String, Object> getPlaybook() {
            return playbook;
        }

        public void setPlaybook(Map<String, Object> playbook) {
            this.playbook = playbook;
        }

        public String getPausedTaskId() {
            return pausedTaskId;
        }

        public void setPausedTaskId(String pausedTaskId) {
            this.pausedTaskId = pausedTaskId;
        }
    }
}
