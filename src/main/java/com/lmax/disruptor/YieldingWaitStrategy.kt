/*
 * Copyright 2011 LMAX Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lmax.disruptor


/**
 * Yielding strategy that uses a Thread.yield() for [com.lmax.disruptor.EventProcessor]s waiting on a barrier
 * after an initially spinning.
 *
 *
 * This strategy will use 100% CPU, but will more readily give up the CPU than a busy spin strategy if other threads
 * require CPU resource.
 */
class YieldingWaitStrategy : WaitStrategy {

    @Throws(AlertException::class)
    override fun waitFor(
            sequence: Long, cursor: Sequence, dependentSequence: Sequence, barrier: SequenceBarrier): Long {
        var availableSequence: Long = -1

        for (i in 0 until SPIN_TRIES) {
            availableSequence = dependentSequence.get()
            if (availableSequence >= sequence) {
                return availableSequence
            }
            barrier.checkAlert()
        }
        Thread.yield()

        return availableSequence
    }

    override fun signalAllWhenBlocking() {}

    @Throws(AlertException::class)
    private fun applyWaitMethod(barrier: SequenceBarrier, counter: Int): Int {
        barrier.checkAlert()
        return when (counter) {
            0 -> {
                Thread.yield()
                0
            }
            else -> counter -1
        }
    }

    companion object {
        private const val SPIN_TRIES = 100
    }
}
