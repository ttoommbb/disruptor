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
package com.lmax.disruptor.raw

import com.lmax.disruptor.*
import com.lmax.disruptor.util.DaemonThreadFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/**
 * <pre>
 * UniCast a series of items between 1 publisher and 1 event processor.
 *
 * +----+    +-----+
 * | P1 |--->| EP1 |
 * +----+    +-----+
 *
 *
 * Queue Based:
 * ============
 *
 * put      take
 * +----+    +====+    +-----+
 * | P1 |--->| Q1 |<---| EP1 |
 * +----+    +====+    +-----+
 *
 * P1  - Publisher 1
 * Q1  - Queue 1
 * EP1 - EventProcessor 1
 *
 *
 * Disruptor:
 * ==========
 * track to prevent wrap
 * +------------------+
 * |                  |
 * |                  v
 * +----+    +====+    +====+   +-----+
 * | P1 |--->| RB |<---| SB |   | EP1 |
 * +----+    +====+    +====+   +-----+
 * claim      get    ^        |
 * |        |
 * +--------+
 * waitFor
 *
 * P1  - Publisher 1
 * RB  - RingBuffer
 * SB  - SequenceBarrier
 * EP1 - EventProcessor 1
 *
</pre> *
 */
class OneToOneRawBatchThroughputTestKotlin : AbstractPerfTestDisruptor() {
    private val executor = Executors.newSingleThreadExecutor(DaemonThreadFactory.INSTANCE)

    ///////////////////////////////////////////////////////////////////////////////////////////////

    private val sequencer = SingleProducerSequencer(BUFFER_SIZE, YieldingWaitStrategy())
    private val myRunnable = MyRunnable(sequencer)

    init {
        sequencer.addGatingSequences(myRunnable.sequence)
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////

    override fun getRequiredProcessorCount(): Int {
        return 2
    }

    @Throws(InterruptedException::class)
    override fun runDisruptorPass(): PerfTestContext {
        val perfTestContext = PerfTestContext()
        val batchSize = 10
        val latch = CountDownLatch(1)
        val expectedCount = myRunnable.sequence.get() + ITERATIONS * batchSize
        myRunnable.reset(latch, expectedCount)
        executor.submit(myRunnable)
        val start = System.currentTimeMillis()

        val sequencer = this.sequencer

        for (i in 0 until ITERATIONS) {
            val next = sequencer.next(batchSize)
            sequencer.publish(next - (batchSize - 1), next)
        }

        latch.await()
        val end = System.currentTimeMillis()
        perfTestContext.disruptorOps = ITERATIONS * 1000L * batchSize.toLong() / (end - start)
        waitForEventProcessorSequence(expectedCount)

        return perfTestContext
    }

    @Throws(InterruptedException::class)
    private fun waitForEventProcessorSequence(expectedCount: Long) {
        while (myRunnable.sequence.get() != expectedCount) {
            Thread.sleep(1)
        }
    }

    private class MyRunnable internal constructor(sequencer: Sequencer) : Runnable {
        private var latch: CountDownLatch? = null
        private var expectedCount: Long = 0
        internal var sequence = Sequence(-1)
        private val barrier: SequenceBarrier = sequencer.newBarrier()

        fun reset(latch: CountDownLatch, expectedCount: Long) {
            this.latch = latch
            this.expectedCount = expectedCount
        }

        override fun run() {
            val expected = expectedCount
            var processed: Long

            try {
                do {
                    processed = barrier.waitFor(sequence.get() + 1)
                    sequence.set(processed)
                } while (processed < expected)

                latch!!.countDown()
                sequence.set(processed)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    companion object {
        private const val BUFFER_SIZE = 1024 * 64
        private const val ITERATIONS = 1000L * 1000L * 200L

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val test = OneToOneRawBatchThroughputTestKotlin()
            test.testImplementations()
        }
    }
}
