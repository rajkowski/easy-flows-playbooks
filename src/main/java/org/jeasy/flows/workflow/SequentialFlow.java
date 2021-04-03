/*
 * The MIT License
 *
 *  Copyright (c) 2020, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
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
package org.jeasy.flows.workflow;

import org.jeasy.flows.work.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.jeasy.flows.work.WorkStatus.FAILED;

/**
 * A sequential flow executes a set of work units in sequence.
 *
 * If a unit of work fails, next work units in the pipeline will be skipped.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public class SequentialFlow extends AbstractWorkFlow {

    private static final Logger LOGGER = LoggerFactory.getLogger(SequentialFlow.class.getName());

    private final List<TaskContext> taskContexts = new ArrayList<>();

    SequentialFlow(String name, List<TaskContext> taskContexts) {
        super(name);
        this.taskContexts.addAll(taskContexts);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkReport execute(WorkContext workContext) {
        return execute(workContext, null);
    }

    @Override
    public WorkReport execute(WorkContext workContext, TaskContext taskContext2) {
        WorkReport workReport = null;
        for (TaskContext taskContext : taskContexts) {
            if (taskContext.getWork() == null) {
                LOGGER.error("Task has no Work object!");
                continue;
            }
            if (taskContext.getWork().getClass() == null) {
                LOGGER.error("Task has no Work class!");
                continue;
            }
            // Determine if there is a 'when' condition that must be satisfied
            boolean canExecute = true;
            if (taskContext.getWhen() != null) {
                boolean result = When.validate(workContext, taskContext, taskContext.getWhen());
                if (!result) {
                    // If within a block, then break;
                    if ("block".equals(getName())) {
                        break;
                    }
                    // Else skip just this task
                    canExecute = false;
                    LOGGER.warn("Skipping " + taskContext.getWork().getClass().getSimpleName() + ", condition not met: " + taskContext.getWhen());
                }
            }
            if (canExecute) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Executing work: " + taskContext.getWork().getClass().getSimpleName());
                }
                workReport = taskContext.getWork().execute(workContext, taskContext);
                if (workReport != null && FAILED.equals(workReport.getStatus()) && !"block".equals(taskContext.getWork().getName())) {
                    LOGGER.warn("Work unit '{}' has failed, skipping subsequent work units", taskContext.getWork().getName());
                    break;
                }
            }
        }
        return workReport;
    }

    public static class Builder {

        private Builder() {
            // force usage of static method aNewSequentialFlow
        }

        public static NameStep aNewSequentialFlow() {
            return new BuildSteps();
        }

        public interface NameStep extends ExecuteStep {
            ExecuteStep named(String name);
        }

        public interface ExecuteStep {
            ThenStep execute(TaskContext taskContext);
            ThenStep execute(Work work);
            ThenStep execute(List<TaskContext> initialTaskContexts);
        }

        public interface ThenStep {
            ThenStep then(TaskContext nextTaskContext);
            ThenStep then(Work work);
            ThenStep then(List<TaskContext> nextTaskContexts);
            SequentialFlow build();
        }

        private static class BuildSteps implements NameStep, ExecuteStep, ThenStep {

            private String name;
            private final List<TaskContext> taskContexts;

            BuildSteps() {
                this.name = UUID.randomUUID().toString();
                this.taskContexts = new ArrayList<>();
            }

            public ExecuteStep named(String name) {
                this.name = name;
                return this;
            }

            @Override
            public ThenStep execute(TaskContext taskContext) {
                this.taskContexts.add(taskContext);
                return this;
            }

            @Override
            public ThenStep execute(Work work) {
                this.taskContexts.add(new TaskContext(work));
                return this;
            }

            @Override
            public ThenStep execute(List<TaskContext> initialTaskContexts) {
                for (TaskContext taskContext : initialTaskContexts) {
                    this.taskContexts.add(taskContext);
                }
                return this;
            }

            @Override
            public ThenStep then(TaskContext nextTaskContext) {
                this.taskContexts.add(nextTaskContext);
                return this;
            }

            @Override
            public ThenStep then(Work nextTaskContext) {
                this.taskContexts.add(new TaskContext(nextTaskContext));
                return this;
            }

            @Override
            public ThenStep then(List<TaskContext> nextTaskContexts) {
                for (TaskContext taskContext : nextTaskContexts) {
                    this.taskContexts.add(taskContext);
                }
                return this;
            }

            @Override
            public SequentialFlow build() {
                return new SequentialFlow(this.name, this.taskContexts);
            }
        }
    }
}
