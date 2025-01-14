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
package org.jeasy.flows.engine;

import static org.jeasy.flows.engine.WorkFlowEngineBuilder.aNewWorkFlowEngine;
import static org.jeasy.flows.work.WorkReportPredicate.COMPLETED;
import static org.jeasy.flows.workflow.ConditionalFlow.Builder.aNewConditionalFlow;
import static org.jeasy.flows.workflow.ParallelFlow.Builder.aNewParallelFlow;
import static org.jeasy.flows.workflow.RepeatFlow.Builder.aNewRepeatFlow;
import static org.jeasy.flows.workflow.SequentialFlow.Builder.aNewSequentialFlow;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jeasy.flows.work.DefaultWorkReport;
import org.jeasy.flows.work.TaskContext;
import org.jeasy.flows.work.Work;
import org.jeasy.flows.work.WorkContext;
import org.jeasy.flows.work.WorkReport;
import org.jeasy.flows.work.WorkStatus;
import org.jeasy.flows.workflow.ConditionalFlow;
import org.jeasy.flows.workflow.ParallelFlow;
import org.jeasy.flows.workflow.RepeatFlow;
import org.jeasy.flows.workflow.SequentialFlow;
import org.jeasy.flows.workflow.WorkFlow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class WorkFlowEngineImplTest {

    private final WorkFlowEngine workFlowEngine = new WorkFlowEngineImpl();

    @Test
    void run() {
        // given
        WorkFlow workFlow = Mockito.mock(WorkFlow.class);
        WorkContext workContext = Mockito.mock(WorkContext.class);

        // when
        workFlowEngine.run(workFlow, workContext);

        // then
        Mockito.verify(workFlow).execute(workContext);
    }

    /**
     * The following tests are not really unit tests, but serve as examples of how to create a workflow and execute it
     */

    @Test
    void composeWorkFlowFromSeparateFlowsAndExecuteIt() {

        PrintMessageWork work = new PrintMessageWork();

        TaskContext tc1 = new TaskContext(work, "foo");
        TaskContext tc2 = new TaskContext(work, "hello");
        TaskContext tc3 = new TaskContext(work, "world");
        TaskContext tc4 = new TaskContext(work, "done");

        RepeatFlow repeatFlow = aNewRepeatFlow()
                .named("print foo 3 times")
                .repeat(tc1)
                .times(3)
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        ParallelFlow parallelFlow = aNewParallelFlow()
                .named("print 'hello' and 'world' in parallel")
                .execute(tc2, tc3)
                .with(executorService)
                .timeout(1, TimeUnit.SECONDS)
                .build();

        ConditionalFlow conditionalFlow = aNewConditionalFlow()
                .execute(parallelFlow)
                .when(COMPLETED)
                .then(tc4)
                .build();

        SequentialFlow sequentialFlow = aNewSequentialFlow()
                .execute(repeatFlow)
                .then(conditionalFlow)
                .build();

        WorkFlowEngine workFlowEngine = aNewWorkFlowEngine().build();
        WorkContext workContext = new WorkContext();
        WorkReport workReport = workFlowEngine.run(sequentialFlow, workContext);
        executorService.shutdown();
        Assertions.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
        System.out.println("workflow report = " + workReport);
    }

    @Test
    void defineWorkFlowInlineAndExecuteIt() {

        PrintMessageWork work = new PrintMessageWork();

        TaskContext tc1 = new TaskContext(work, "foo");
        TaskContext tc2 = new TaskContext(work, "hello");
        TaskContext tc3 = new TaskContext(work, "world");
        TaskContext tc4 = new TaskContext(work, "done");

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        WorkFlow workflow = aNewSequentialFlow()
                .execute(aNewRepeatFlow()
                        .named("print foo 3 times")
                        .repeat(tc1)
                        .times(3)
                        .build())
                .then(aNewConditionalFlow()
                        .execute(aNewParallelFlow()
                                .named("print 'hello' and 'world' in parallel")
                                .execute(tc2, tc3)
                                .with(executorService)
                                .timeout(1, TimeUnit.SECONDS)
                                .build())
                        .when(COMPLETED)
                        .then(tc4)
                        .build())
                .build();

        WorkFlowEngine workFlowEngine = aNewWorkFlowEngine().build();
        WorkContext workContext = new WorkContext();
        WorkReport workReport = workFlowEngine.run(workflow, workContext);
        executorService.shutdown();
        Assertions.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
        System.out.println("workflow report = " + workReport);
    }

    @Test
    void useWorkContextToPassInitialParametersAndShareDataBetweenWorkUnits() {

        WordCountWork work = new WordCountWork();
        TaskContext tc1 = new TaskContext(work, "hello foo hello you");
        tc1.put("partition", 1);
        TaskContext tc2 = new TaskContext(work, "hello bar");
        tc2.put("partition", 2);

        AggregateWordCountsWork work3 = new AggregateWordCountsWork();

        PrintWordCount work4 = new PrintWordCount();

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        WorkFlow workflow = aNewSequentialFlow()
                .execute(aNewParallelFlow()
                        .execute(tc1, tc2)
                        .with(executorService)
                        .timeout(1, TimeUnit.SECONDS)
                        .build())
                .then(work3)
                .then(work4)
                .build();

        WorkFlowEngine workFlowEngine = aNewWorkFlowEngine().build();
        WorkContext workContext = new WorkContext();
        WorkReport workReport = workFlowEngine.run(workflow, workContext);
        executorService.shutdown();
        Assertions.assertEquals(WorkStatus.COMPLETED, workReport.getStatus());
    }

    static class PrintMessageWork implements Work {

        public static final String MESSAGE_VAR = "totalCount";

        public PrintMessageWork() {
        }

        public String getName() {
            return "print message work";
        }

        public WorkReport execute(WorkContext workContext, TaskContext taskContext) {
            String message = (String) workContext.get(MESSAGE_VAR);
            if (message == null) {
                message = taskContext.getData();
            }
            System.out.println(message);
            return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
        }

    }

    static class WordCountWork implements Work {

        public static final String PARTITION_VAR = "partition";

        public WordCountWork() {
        }

        @Override
        public String getName() {
            return "count words in a given string";
        }

        @Override
        public WorkReport execute(WorkContext workContext, TaskContext taskContext) {
            int partition = (Integer) taskContext.get(PARTITION_VAR);
            String input = taskContext.getData();
            workContext.put("wordCountInPartition" + partition, input.split(" ").length);
            return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
        }
    }

    static class AggregateWordCountsWork implements Work {

        @Override
        public String getName() {
            return "aggregate word counts from partitions";
        }

        @Override
        public WorkReport execute(WorkContext workContext, TaskContext taskContext) {
            Set<Map.Entry<String, Object>> entrySet = workContext.getEntrySet();
            int sum = 0;
            for (Map.Entry<String, Object> entry : entrySet) {
                if (entry.getKey().contains("InPartition")) {
                    sum += (int) entry.getValue();
                }
            }
            workContext.put("totalCount", sum);
            return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
        }
    }

    static class PrintWordCount implements Work {

        @Override
        public String getName() {
            return "print total word count";
        }

        @Override
        public WorkReport execute(WorkContext workContext, TaskContext taskContext) {
            int totalCount = (int) workContext.get("totalCount");
            System.out.println(totalCount);
            return new DefaultWorkReport(WorkStatus.COMPLETED, workContext);
        }
    }
}
