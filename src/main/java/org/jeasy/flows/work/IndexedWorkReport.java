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
package org.jeasy.flows.work;

/**
 * Decorates a {@link WorkReport} with the hierarchical element id of the playbook task it was produced at
 * (e.g. "2.1" for a task nested inside the top-level task at index 2), so a paused/waiting execution can
 * later be resumed at the exact same position, even inside a nested block.
 *
 * @author Matt Rajkowski
 */
public final class IndexedWorkReport implements WorkReport {

    private final WorkReport delegate;
    private final String taskId;

    public IndexedWorkReport(WorkReport delegate, String taskId) {
        this.delegate = delegate;
        this.taskId = taskId;
    }

    @Override
    public WorkStatus getStatus() {
        return delegate.getStatus();
    }

    @Override
    public String getExecutionId() {
        return delegate.getExecutionId();
    }

    @Override
    public Throwable getError() {
        return delegate.getError();
    }

    @Override
    public WorkContext getWorkContext() {
        return delegate.getWorkContext();
    }

    @Override
    public String getTaskId() {
        return taskId;
    }
}
