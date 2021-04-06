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

import org.jeasy.flows.work.TaskContext;
import org.jeasy.flows.work.WorkContext;
import org.jeasy.flows.work.WorkReport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A parallel flow executes a set of work units in parallel. A {@link ParallelFlow}
 * requires a {@link ExecutorService} to execute work units in parallel using multiple
 * threads.
 * 
 * <strong>It is the responsibility of the caller to manage the lifecycle of the
 * executor service.</strong>
 *
 * The status of a parallel flow execution is defined as:
 *
 * <ul>
 *     <li>{@link org.jeasy.flows.work.WorkStatus#COMPLETED}: If all work units have successfully completed</li>
 *     <li>{@link org.jeasy.flows.work.WorkStatus#FAILED}: If one of the work units has failed</li>
 * </ul>
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public class ParallelFlow extends AbstractWorkFlow {

    private final List<TaskContext> workUnits = new ArrayList<>();
    private final ParallelFlowExecutor workExecutor;

    public ParallelFlow(String name, List<TaskContext> workUnits, ParallelFlowExecutor parallelFlowExecutor) {
        super(name);
        this.workUnits.addAll(workUnits);
        this.workExecutor = parallelFlowExecutor;
    }

    /**
     * {@inheritDoc}
     */
    public ParallelFlowReport execute(WorkContext workContext) {
        return execute(workContext, null);
    }

    @Override
    public ParallelFlowReport execute(WorkContext workContext, TaskContext taskContext2) {
        ParallelFlowReport workFlowReport = new ParallelFlowReport();
        List<WorkReport> workReports = workExecutor.executeInParallel(workUnits, workContext);
        workFlowReport.addAll(workReports);
        return workFlowReport;
    }

    public static class Builder {

        private Builder() {
            // force usage of method aNewParallelFlow
        }
        
        public static NameStep aNewParallelFlow() {
            return new BuildSteps();
        }

        public interface NameStep extends ExecuteStep {
            ExecuteStep named(String name);
        }

        public interface ExecuteStep {
            WithStep execute(TaskContext... workUnits);
            WithStep execute(List<TaskContext> initialTaskContexts);
        }

        public interface WithStep {
            /**
             *  A {@link ParallelFlow} requires an {@link ExecutorService} to
             *  execute work units in parallel using multiple threads.
             *  
             *  <strong>It is the responsibility of the caller to manage the lifecycle
             *  of the executor service.</strong>
             *  
             * @param executorService to use to execute work units in parallel
             * @return the builder instance
             */
            TimeoutStep with(ExecutorService executorService);
        }

        public interface TimeoutStep {
            BuildStep timeout(long timeout, TimeUnit unit);
        }

        public interface BuildStep {
            ParallelFlow build();
        }

        private static class BuildSteps implements NameStep, ExecuteStep, WithStep, TimeoutStep, BuildStep {

            private String name;
            private final List<TaskContext> works;
            private ExecutorService executorService;
            private long timeout;
            private TimeUnit unit;

            public BuildSteps() {
                this.name = UUID.randomUUID().toString();
                this.works = new ArrayList<>();
            }

            @Override
            public ExecuteStep named(String name) {
                this.name = name;
                return this;
            }

            @Override
            public WithStep execute(TaskContext... workUnits) {
                this.works.addAll(Arrays.asList(workUnits));
                return this;
            }

            @Override
            public WithStep execute(List<TaskContext> workUnits) {
                this.works.addAll(workUnits);
                return this;
            }

            @Override
            public TimeoutStep with(ExecutorService executorService) {
                this.executorService = executorService;
                return this;
            }
            
            @Override
            public BuildStep timeout(long timeout, TimeUnit unit) {
                this.timeout = timeout;
                this.unit = unit;
                return this;
            }

            @Override
            public ParallelFlow build() {
                return new ParallelFlow(
                        this.name, this.works,
                        new ParallelFlowExecutor(this.executorService, timeout, unit));
            }
        }

    }
}
