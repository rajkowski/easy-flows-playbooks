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
package com.github.rajkowski.workflow.work;

/**
 * Execution report of a unit of work.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 */
public interface WorkReport {

    /**
     * Get work execution status.
     *
     * @return execution status
     */
    WorkStatus getStatus();

    /**
     * Get the execution identifier assigned to the workflow when it was started.
     *
     * @return execution identifier or {@code null} when not tracked
     */
    default String getExecutionId() {
        return null;
    }

    /**
     * Check whether the workflow is still running.
     *
     * @return {@code true} when the status is {@link WorkStatus#IN_PROGRESS}, {@code false} otherwise
     */
    default boolean isInProgress() {
        return WorkStatus.IN_PROGRESS.equals(getStatus());
    }

    /**
     * Check whether the workflow is waiting for external input or approval.
     *
     * @return {@code true} when the status is {@link WorkStatus#WAITING}
     */
    default boolean isWaiting() {
        return WorkStatus.WAITING.equals(getStatus());
    }

    /**
     * Check whether the workflow is complete.
     *
     * @return {@code true} when the status is {@link WorkStatus#COMPLETED}
     */
    default boolean isCompleted() {
        return WorkStatus.COMPLETED.equals(getStatus());
    }

    /**
     * Get error if any. Might be {@code null}, but usually not null when
     * the status is {@link WorkStatus#FAILED}. Typically the exception includes
     * the exit code that might be used to drive the flow execution accordingly.
     *
     * @return error
     */
    Throwable getError();

    /**
     * Get the last work context of the flow.
     *
     * @return last work context of the flow
     */
    WorkContext getWorkContext();

    /**
     * Get the hierarchical element id (e.g. "2.1") of the playbook task that produced this report, when known.
     *
     * @return the task's element id, or {@code null} when not tracked
     */
    default String getTaskId() {
        return null;
    }

}
