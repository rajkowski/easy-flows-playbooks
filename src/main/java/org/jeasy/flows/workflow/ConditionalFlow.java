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

import java.util.UUID;

import org.jeasy.flows.work.NoOpTask;
import org.jeasy.flows.work.TaskContext;
import org.jeasy.flows.work.Work;
import org.jeasy.flows.work.WorkContext;
import org.jeasy.flows.work.WorkReport;
import org.jeasy.flows.work.WorkReportPredicate;

/**
 * A conditional flow is defined by 4 artifacts:
 *
 * <ul>
 *     <li>The work to execute first</li>
 *     <li>A predicate for the conditional logic</li>
 *     <li>The work to execute if the predicate is satisfied</li>
 *     <li>The work to execute if the predicate is not satisfied (optional)</li>
 * </ul>
 *
 * @see ConditionalFlow.Builder
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public class ConditionalFlow extends AbstractWorkFlow {

    private final TaskContext initialWorkUnit, nextOnPredicateSuccess, nextOnPredicateFailure;
    private final WorkReportPredicate predicate;

    ConditionalFlow(String name, TaskContext initialWorkUnit, TaskContext nextOnPredicateSuccess,
            TaskContext nextOnPredicateFailure, WorkReportPredicate predicate) {
        super(name);
        this.initialWorkUnit = initialWorkUnit;
        this.nextOnPredicateSuccess = nextOnPredicateSuccess;
        this.nextOnPredicateFailure = nextOnPredicateFailure;
        this.predicate = predicate;
    }

    /**
     * {@inheritDoc}
     */
    public WorkReport execute(WorkContext workContext) {
        WorkReport jobReport = initialWorkUnit.execute(workContext, initialWorkUnit);
        if (predicate.apply(jobReport)) {
            jobReport = nextOnPredicateSuccess.execute(workContext, nextOnPredicateSuccess);
        } else {
            if (nextOnPredicateFailure != null && !(nextOnPredicateFailure.getWork() instanceof NoOpTask)) { // else is optional
                jobReport = nextOnPredicateFailure.execute(workContext, nextOnPredicateFailure);
            }
        }
        return jobReport;
    }

    @Override
    public WorkReport execute(WorkContext workContext, TaskContext taskContext2) {
        WorkReport jobReport = initialWorkUnit.execute(workContext, initialWorkUnit);
        if (predicate.apply(jobReport)) {
            jobReport = nextOnPredicateSuccess.execute(workContext, nextOnPredicateSuccess);
        } else {
            if (nextOnPredicateFailure != null && !(nextOnPredicateFailure.getWork() instanceof NoOpTask)) { // else is optional
                jobReport = nextOnPredicateFailure.execute(workContext, nextOnPredicateFailure);
            }
        }
        return jobReport;
    }

    public static class Builder {

        private Builder() {
            // force usage of static method aNewConditionalFlow
        }

        public static NameStep aNewConditionalFlow() {
            return new BuildSteps();
        }

        public interface NameStep extends ExecuteStep {
            ExecuteStep named(String name);
        }

        public interface ExecuteStep {
            WhenStep execute(TaskContext initialWorkUnit);

            WhenStep execute(Work initialWorkUnit);
        }

        public interface WhenStep {
            ThenStep when(WorkReportPredicate predicate);
        }

        public interface ThenStep {
            OtherwiseStep then(TaskContext work);
        }

        public interface OtherwiseStep extends BuildStep {
            BuildStep otherwise(TaskContext work);
        }

        public interface BuildStep {
            ConditionalFlow build();
        }

        private static class BuildSteps implements NameStep, ExecuteStep, WhenStep, ThenStep, OtherwiseStep, BuildStep {

            private String name;
            private TaskContext initialWorkUnit, nextOnPredicateSuccess, nextOnPredicateFailure;
            private WorkReportPredicate predicate;

            BuildSteps() {
                this.name = UUID.randomUUID().toString();
                this.initialWorkUnit = new TaskContext(new NoOpTask());
                this.nextOnPredicateSuccess = new TaskContext(new NoOpTask());
                this.nextOnPredicateFailure = new TaskContext(new NoOpTask());
                this.predicate = WorkReportPredicate.ALWAYS_FALSE;
            }

            @Override
            public ExecuteStep named(String name) {
                this.name = name;
                return this;
            }

            @Override
            public WhenStep execute(TaskContext initialWorkUnit) {
                this.initialWorkUnit = initialWorkUnit;
                return this;
            }

            @Override
            public WhenStep execute(Work initialWorkUnit) {
                this.initialWorkUnit = new TaskContext(initialWorkUnit);
                return this;
            }

            @Override
            public ThenStep when(WorkReportPredicate predicate) {
                this.predicate = predicate;
                return this;
            }

            @Override
            public OtherwiseStep then(TaskContext work) {
                this.nextOnPredicateSuccess = work;
                return this;
            }

            @Override
            public BuildStep otherwise(TaskContext work) {
                this.nextOnPredicateFailure = work;
                return this;
            }

            @Override
            public ConditionalFlow build() {
                return new ConditionalFlow(this.name, this.initialWorkUnit,
                        this.nextOnPredicateSuccess, this.nextOnPredicateFailure,
                        this.predicate);
            }
        }
    }
}
