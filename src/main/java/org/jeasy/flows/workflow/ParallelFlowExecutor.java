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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class ParallelFlowExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelFlowExecutor.class.getName());

    private final ExecutorService workExecutor;
    private final long timeout;
    private final TimeUnit unit;

    public ParallelFlowExecutor(ExecutorService workExecutor, long timeout, TimeUnit unit) {
        this.workExecutor = workExecutor;
        this.timeout = timeout;
        this.unit = unit;
    }

    public List<WorkReport> executeInParallel(List<TaskContext> workUnits, WorkContext workContext) {
        // Prepare tasks for parallel submission
        LOGGER.debug("tasks=" + workUnits.size() + "; timeout=" + timeout);
        List<Callable<WorkReport>> tasks = new ArrayList<>(workUnits.size());
        workUnits.forEach(work -> tasks.add(() -> work.execute(workContext, work)));

        // Submit work units and wait for results
        LOGGER.trace("Submit work units and wait for results");
        List<Future<WorkReport>> futures;
        try {
            futures = this.workExecutor.invokeAll(tasks);
        } catch (InterruptedException e) {
            throw new RuntimeException("The parallel flow was interrupted while executing work units", e);
        }
        LOGGER.debug("executor=" + workExecutor.toString());
        Map<TaskContext, Future<WorkReport>> workToReportFuturesMap = new HashMap<>();
        for (int index = 0; index < workUnits.size(); index++) {
            workToReportFuturesMap.put(workUnits.get(index), futures.get(index));
        }

        // Gather reports
        List<WorkReport> workReports = new ArrayList<>();
        for (Map.Entry<TaskContext, Future<WorkReport>> entry : workToReportFuturesMap.entrySet()) {
            try {
                WorkReport workReport;
                try {
                    workReport = entry.getValue().get(timeout, unit);
                } catch (TimeoutException e) {
                    workReport = new DefaultWorkReport(WorkStatus.FAILED, workContext);
                }
                workReports.add(workReport);
            } catch (InterruptedException e) {
                String message = String.format("The parallel flow was interrupted while waiting for the result of work unit '%s'", entry.getKey().getWork().getName());
                throw new RuntimeException(message, e);
            } catch (ExecutionException e) {
                String message = String.format("Unable to execute work unit '%s'", entry.getKey().getWork().getName());
                throw new RuntimeException(message, e);
            }
        }

        LOGGER.debug("Shutting down workExecutor");
        workExecutor.shutdown();
        return workReports;
    }
}
