package com.lmax.disruptor.util

import ch.qos.logback.core.pattern.color.ANSIConstants
import com.google.common.collect.Range
import com.google.common.collect.RangeMap
import com.google.common.collect.TreeRangeMap

class AnsiStringBuilder(private val sequence: CharSequence) {

    @Suppress("UnstableApiUsage")
    private val ansiParts: RangeMap<Int, String> = TreeRangeMap.create()

    fun ansi(start: Int, end: Int, fg: String): AnsiStringBuilder {
        ansiParts.put(Range.closedOpen(start, end), fg)
        return this
    }

    fun print() {
        var i = 0

        sequence.lines().forEach { line ->
            var lineOffset = 9
            when {
                line.length < lineOffset -> println(line)
                else -> {
                    print(line.subSequence(0, lineOffset))
                    (lineOffset until lineOffset + 48 step 3).forEach {
                        val color = ansiParts[i]
                        if (color != null) {
                            print(ANSIConstants.ESC_START)
                            print(color)
                            print(ANSIConstants.ESC_END)
                        }
                        print(line.subSequence(it, it + 3))
                        if (color != null) {
                            print(ANSIConstants.ESC_START)
                            print(ANSIConstants.DEFAULT_FG)
                            print(ANSIConstants.ESC_END)
                        }
                        i++
                    }
                    println(line.subSequence(lineOffset + 48, line.length))
                }
            }
        }
    }
}

fun CharSequence.ansi(start: Int, end: Int, color: String): AnsiStringBuilder =
        AnsiStringBuilder(this).ansi(start, end, color)