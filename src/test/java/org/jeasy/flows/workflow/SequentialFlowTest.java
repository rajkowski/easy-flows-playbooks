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

import java.util.Arrays;
import java.util.List;

import org.jeasy.flows.work.TaskContext;
import org.jeasy.flows.work.WorkContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SequentialFlowTest {

    @Test
    void testExecute() {
        // given
        TaskContext work1 = Mockito.mock(TaskContext.class);
        TaskContext work2 = Mockito.mock(TaskContext.class);
        TaskContext work3 = Mockito.mock(TaskContext.class);
        WorkContext workContext = Mockito.mock(WorkContext.class);
        SequentialFlow sequentialFlow = SequentialFlow.Builder.aNewSequentialFlow()
                .named("testFlow")
                .execute(work1)
                .then(work2)
                .then(work3)
                .build();

        // when
        sequentialFlow.execute(workContext);

        // then
        /*
        InOrder inOrder = Mockito.inOrder(work1, work2, work3);
        inOrder.verify(work1, Mockito.times(1)).execute(workContext, work1);
        inOrder.verify(work2, Mockito.times(1)).execute(workContext, work2);
        inOrder.verify(work3, Mockito.times(1)).execute(workContext, work3);
        */
    }

    @Test
    void testPassingMultipleWorkUnitsAtOnce() {
        // given
        TaskContext work1 = Mockito.mock(TaskContext.class);
        TaskContext work2 = Mockito.mock(TaskContext.class);
        TaskContext work3 = Mockito.mock(TaskContext.class);
        TaskContext work4 = Mockito.mock(TaskContext.class);
        WorkContext workContext = Mockito.mock(WorkContext.class);
        List<TaskContext> initialWorkUnits = Arrays.asList(work1, work2);
        List<TaskContext> nextWorkUnits = Arrays.asList(work3, work4);
        SequentialFlow sequentialFlow = SequentialFlow.Builder.aNewSequentialFlow()
                .named("testFlow")
                .execute(initialWorkUnits)
                .then(nextWorkUnits)
                .build();

        // when
        sequentialFlow.execute(workContext);

        // then
        /*
        InOrder inOrder = Mockito.inOrder(work1, work2, work3, work4);
        inOrder.verify(work1, Mockito.times(1)).execute(workContext, work1);
        inOrder.verify(work2, Mockito.times(1)).execute(workContext, work2);
        inOrder.verify(work3, Mockito.times(1)).execute(workContext, work3);
        inOrder.verify(work4, Mockito.times(1)).execute(workContext, work4);
        */
    }

}
