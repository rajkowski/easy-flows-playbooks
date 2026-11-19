/*
 * The MIT License
 *
 *  Copyright 2026 Matt Rajkowski (https://github.com/rajkowski)
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

import java.util.UUID;

import org.jeasy.flows.work.DefaultWorkReport;
import org.jeasy.flows.work.WorkContext;
import org.jeasy.flows.work.WorkReport;
import org.jeasy.flows.work.WorkStatus;
import org.jeasy.flows.workflow.WorkFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class WorkFlowEngineImpl implements WorkFlowEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkFlowEngineImpl.class);

    public WorkReport run(WorkFlow workFlow, WorkContext workContext) {
        String executionId = UUID.randomUUID().toString();
        LOGGER.info("Running workflow '{}' [{}]", workFlow.getName(), executionId);

        WorkReport workReport = workFlow.execute(workContext);
        if (workReport == null) {
            workReport = new DefaultWorkReport(WorkStatus.IN_PROGRESS, workContext, null, executionId);
        }

        WorkStatus status = workReport.getStatus() == null ? WorkStatus.IN_PROGRESS : workReport.getStatus();

        if (WorkStatus.WAITING.equals(status) || WorkStatus.PAUSED.equals(status)) {
            LOGGER.info("Workflow '{}' is waiting for external input [{}]", workFlow.getName(), executionId);
        }

        return new DefaultWorkReport(status, workReport.getWorkContext(), workReport.getError(), executionId);
    }

    @Override
    public WorkReport resume(WorkflowExecutionSnapshot workflowExecutionSnapshot, WorkContext workContext) {
        if (workflowExecutionSnapshot == null) {
            throw new IllegalArgumentException("Workflow execution snapshot cannot be null");
        }

        String executionId = workflowExecutionSnapshot.getExecutionId();
        WorkContext resolvedContext = workContext == null ? workflowExecutionSnapshot.getWorkContext() : workContext;

        LOGGER.info("Restoring workflow '{}' [{}] from exported snapshot", workflowExecutionSnapshot.getWorkflowName(),
                executionId);
        return new DefaultWorkReport(WorkStatus.IN_PROGRESS, resolvedContext, null, executionId);
    }

}
