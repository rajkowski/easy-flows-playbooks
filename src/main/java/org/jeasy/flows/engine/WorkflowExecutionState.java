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
package org.jeasy.flows.engine;

import org.jeasy.flows.work.WorkContext;
import org.jeasy.flows.work.WorkStatus;
import org.jeasy.flows.workflow.WorkFlow;

public class WorkflowExecutionState {

    private final String executionId;
    private final String workflowName;
    private final WorkFlow workFlow;
    private WorkStatus status;
    private WorkContext workContext;

    public WorkflowExecutionState(String executionId, String workflowName, WorkStatus status, WorkContext workContext) {
        this(executionId, workflowName, status, workContext, null);
    }

    public WorkflowExecutionState(String executionId, String workflowName, WorkStatus status, WorkContext workContext,
            WorkFlow workFlow) {
        this.executionId = executionId;
        this.workflowName = workflowName;
        this.status = status;
        this.workContext = workContext;
        this.workFlow = workFlow;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public WorkFlow getWorkFlow() {
        return workFlow;
    }

    public WorkStatus getStatus() {
        return status;
    }

    public void setStatus(WorkStatus status) {
        this.status = status;
    }

    public WorkContext getWorkContext() {
        return workContext;
    }

    public void setWorkContext(WorkContext workContext) {
        this.workContext = workContext;
    }
}
