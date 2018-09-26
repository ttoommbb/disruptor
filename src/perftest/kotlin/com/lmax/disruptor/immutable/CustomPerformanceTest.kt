package com.lmax.disruptor.immutable

import com.lmax.disruptor.SingleProducerSequencer
import com.lmax.disruptor.YieldingWaitStrategy
import java.util.concurrent.locks.LockSupport
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

class CustomPerformanceTest {
    private val ringBuffer: CustomRingBuffer<SimpleEvent> = CustomRingBuffer(SingleProducerSequencer(Constants.SIZE, YieldingWaitStrategy()))

    fun run() {
        try {
            doRun()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    @Throws(InterruptedException::class)
    private fun doRun() {
        val batchEventProcessor = ringBuffer.createHandler(SimpleEventHandler())

        val t = Thread(batchEventProcessor)
        t.start()

        val iterations = Constants.ITERATIONS
        for (l in 0 until iterations) {
            val e = SimpleEvent(l, l, l, l)
            ringBuffer.put(e)
        }

        while (batchEventProcessor.sequence.get() != iterations - 1) {
            LockSupport.parkNanos(1)
        }

        batchEventProcessor.halt()
        t.join()
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            measureNanoTime {
                CustomPerformanceTest().run()
            }.apply {
                println("$this nanos")
            }
        }
    }

}
