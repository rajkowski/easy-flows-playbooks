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
package com.github.rajkowski.workflow.work;

/**
 * A utility task for pausing a workflow while waiting on external approval or another human-driven event.
 *
 * @author Matt Rajkowski
 */
public class PauseTask implements Work {

    private final String reason;

    public PauseTask() {
        this("workflow paused waiting for approval");
    }

    public PauseTask(String reason) {
        this.reason = reason;
    }    

    @Override
    public WorkReport execute(WorkContext workContext, TaskContext taskContext) {
        if (workContext == null) {
            throw new IllegalArgumentException("workContext cannot be null");
        }
        String pauseReason = taskContext != null && taskContext.get("reason") != null
                ? String.valueOf(taskContext.get("reason"))
                : reason;
        workContext.put("workflow.pause.reason", pauseReason);
        workContext.put("workflow.pause.pending", true);
        return new DefaultWorkReport(WorkStatus.WAITING, workContext);
    }
}
