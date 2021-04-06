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

import org.assertj.core.api.Assertions;
import org.jeasy.flows.work.*;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ParallelFlowExecutorTest {

    @Test
    public void testExecute() {

        // given
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        HelloWorldWork work = new HelloWorldWork();

        TaskContext tc1 = new TaskContext(work);
        tc1.put(HelloWorldWork.STATUS_VAR, WorkStatus.COMPLETED);

        TaskContext tc2 = new TaskContext(work);
        tc2.put(HelloWorldWork.STATUS_VAR, WorkStatus.FAILED);

        WorkContext workContext = Mockito.mock(WorkContext.class);
    ParallelFlowExecutor parallelFlowExecutor = new ParallelFlowExecutor(executorService, 1, TimeUnit.SECONDS);

        // when
        List<WorkReport> workReports = parallelFlowExecutor.executeInParallel(Arrays.asList(tc1, tc2), workContext);
        executorService.shutdown();

        // then
        Assertions.assertThat(workReports).hasSize(2);
        Assertions.assertThat(workReports.get(0).getStatus()).isNotEqualTo(workReports.get(1).getStatus());
    }

    static class HelloWorldWork implements Work {

        public static final String STATUS_VAR = "STATUS";

        HelloWorldWork() {
        }

        @Override
        public String getName() {
            return "hello world work";
        }

        @Override
        public WorkReport execute(WorkContext workContext, TaskContext taskContext) {
            WorkStatus value = (WorkStatus) taskContext.get(STATUS_VAR);
            return new DefaultWorkReport(value, workContext);
        }
    }

}
