package com.lmax.disruptor

import ch.qos.logback.core.pattern.color.ANSIConstants
import com.lmax.disruptor.util.Util
import com.lmax.disruptor.util.ansi
import org.apache.commons.codec.binary.BinaryCodec
import org.apache.commons.codec.binary.Hex
import org.apache.commons.io.HexDump
import org.apache.commons.lang3.reflect.FieldUtils
import org.assertj.core.api.Assertions
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.reflect.Field
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.reflect.KClass
import kotlin.reflect.jvm.internal.impl.resolve.constants.ULongValue
import kotlin.test.assertEquals


class UnsafeTest {

    //    private val unsafe: Unsafe by lazy {
//        val field = Unsafe::class.java.getDeclaredField("theUnsafe")
//        field.isAccessible = true
//        field.get(null) as Unsafe
//    }
    private val unsafe = Util.getUnsafe()

    @Test
    fun `check unsafe acquired`() {
        Assertions.assertThat(unsafe).isNotNull
    }

    @Test
    fun `alloc test`() {
        var allocatedAddress = unsafe.allocateMemory(1L)
        unsafe.putByte(allocatedAddress, 100.toByte())
        val shortValue = unsafe.getByte(allocatedAddress)
        println(StringBuilder().append("Address:").append(allocatedAddress).append(" Value:").append(shortValue.toInt()))
        println("long value in the address: ${unsafe.getLong(allocatedAddress)}")
        /**
         * 重新分配一个long
         */
        allocatedAddress = unsafe.reallocateMemory(allocatedAddress, 8L)
        unsafe.putLong(allocatedAddress, 1024L)
        var longValue = unsafe.getLong(allocatedAddress)
        println(StringBuilder().append("Address:").append(allocatedAddress).append(" Value:").append(longValue))
        println("int value in the address: ${unsafe.getByte(allocatedAddress)}")
        println("int value in the address: ${unsafe.getByte(allocatedAddress + 1)}")
        println("int value in the address: ${unsafe.getByte(allocatedAddress + 2)}")
        println("int value in the address: ${unsafe.getByte(allocatedAddress + 3)}")
        /**
         * Free掉,这个数据可能脏掉
         */
        unsafe.freeMemory(allocatedAddress)
        longValue = unsafe.getLong(allocatedAddress)
        println(StringBuilder().append("Address:").append(allocatedAddress).append(" Value:").append(longValue)) //keep changes.

    }


    /**
     * MAX LONG
    16: ff:11111111   17: ff:11111111   18: ff:11111111   19: ff:11111111   20: ff:11111111   21: ff:11111111   22: ff:11111111   23: 7f:01111111
    MIN LONG
    16: 00:00000000   17: 00:00000000   18: 00:00000000   19: 00:00000000   20: 00:00000000   21: 00:00000000   22: 00:00000000   23: 80:10000000
    0 LONG
    16: 00:00000000   17: 00:00000000   18: 00:00000000   19: 00:00000000   20: 00:00000000   21: 00:00000000   22: 00:00000000   23: 00:00000000
    -1
    16: ff:11111111   17: ff:11111111   18: ff:11111111   19: ff:11111111   20: ff:11111111   21: ff:11111111   22: ff:11111111   23: ff:11111111

    disruptor use unsafe:
    return (E) UNSAFE.getObject(entries, REF_ARRAY_BASE + ((sequence & indexMask) << REF_ELEMENT_SHIFT));
     */
    @Test
    fun `class field test`() {
        data class SampleClass(val s: Short, val i: Int, val l: Long, val b: Boolean)

        fun lazyField(name: String): Lazy<Field> {
            return lazy {
                SampleClass::class.java.getDeclaredField(name)
            }
        }

        val sField by lazyField("s")
        val iField by lazyField("i")
        val lField by lazyField("l")
        val bField by lazyField("b")

        println("s field offset:${unsafe.objectFieldOffset(sField)}")
        val iFiledAddressShift = unsafe.objectFieldOffset(iField)
        println("i field offset:$iFiledAddressShift")
        println("l field offset:${unsafe.objectFieldOffset(lField)}")
        println("b field offset:${unsafe.objectFieldOffset(bField)}")
        val sampleClass = SampleClass(Short.MIN_VALUE, 5, Long.MAX_VALUE, true)

        val data = ByteArray(30)
        repeat(30) {
            unsafe.getByte(sampleClass, it.toLong()).apply {
                data[it] = this
                val bytes = byteArrayOf(this)
                print("${it.toString().padStart(2)}: ${Hex.encodeHexString(bytes)}:${BinaryCodec.toAsciiString(bytes)}   ")
                when (it % 8) {
                    7 -> println()
                    else -> {
                    }
                }
            }
        }
        println()

        println("-----------------print using hexdump ------------------")
        HexDump.dump(data, 0, System.out, 0)
        println("-----------------print using hexdump end ------------------")
        unsafe.getLong(sampleClass, unsafe.objectFieldOffset(lField)).apply(::println)

        //TODO: UNSIGNED LONG
        ULongValue(-1).toString().apply(::println)

        //TODO: get field offset
        val arrayBaseOffset = unsafe.arrayBaseOffset(Array<Any>::class.java)
        println("arrayBaseOffset: $arrayBaseOffset")
        val arrayIndexScale = unsafe.arrayIndexScale(Array<Any>::class.java)
        println("arrayIndexScale: $arrayIndexScale")


        RingBufferFields::class.let {
            it.printField("BUFFER_PAD")//32
            it.printField("REF_ARRAY_BASE")//144
            it.printField("REF_ELEMENT_SHIFT")//2
        }

        val sampleArr = arrayOf(sampleClass, sampleClass, sampleClass)
        val sampleAdd = unsafe.getInt(sampleArr, arrayBaseOffset)
        val sample2Add = unsafe.getInt(sampleArr, arrayBaseOffset + arrayIndexScale)
        val sample3Add = unsafe.getInt(sampleArr, arrayBaseOffset + arrayIndexScale*2)
        printDump("sampleArr", sampleArr, 50)
        assertEquals(sampleAdd, sample2Add)
        assertEquals(sampleAdd, sample3Add)
        println("sampleAdd = ${sampleAdd.toHexString()}")
//        println("sample2Add = ${sample2Add.toHexString()}")
//        println("sample3Add = ${sample3Add.toHexString()}")

/*
 private static final int BUFFER_PAD;
    private static final long REF_ARRAY_BASE;
    private static final int REF_ELEMENT_SHIFT;
    private static final Unsafe UNSAFE = Util.getUnsafe();

    static
    {
        final int scale = UNSAFE.arrayIndexScale(Object[].class);
        if (4 == scale)
        {
            REF_ELEMENT_SHIFT = 2; !!
        }
        else if (8 == scale)
        {
            REF_ELEMENT_SHIFT = 3;
        }
        else
        {
            throw new IllegalStateException("Unknown pointer size");
        }
        BUFFER_PAD = 128 / scale; = 32
        // Including the buffer pad in the array base offset
        REF_ARRAY_BASE = UNSAFE.arrayBaseOffset(Object[].class) + (BUFFER_PAD << REF_ELEMENT_SHIFT);
        144 = 16 + 128
        //BUFFER_PAD << REF_ELEMENT_SHIFT = BUFFER_PAD * scale = 128

        //return (E) UNSAFE.getObject(entries, REF_ARRAY_BASE + ((sequence & indexMask) << REF_ELEMENT_SHIFT));
    }
 */
    }

    private fun printDump(name: String, data: Any, limit: Int) {
        println("----------ready print dump: $name------------")
        val bytes = ByteArray(limit)
        repeat(limit) {
            bytes[it] = unsafe.getByte(data, it)
        }

        val stream = ByteArrayOutputStream(0)
        HexDump.dump(bytes, 0, PrintStream(stream), 0)
        val dump = stream.toString(Charsets.UTF_8.name())

        dump.ansi(0, 4, ANSIConstants.RED_FG)
                .ansi(4, 8, ANSIConstants.BLUE_FG)
                .ansi(12, 16, ANSIConstants.BLUE_FG)
                .ansi(16, 20, ANSIConstants.CYAN_FG)
                .ansi(20, 24, ANSIConstants.GREEN_FG)
                .print()
        println("----------ready print dump end: $name------------")
    }


    private fun printStaticFieldValue(clazz: Class<*>, field: String) {
        FieldUtils.readStaticField(clazz, field, true)
                .apply { println("${clazz.canonicalName}.$field: $this") }
    }

    private fun KClass<*>.printField(field: String) {
        printStaticFieldValue(this.java, field)
    }
}

private fun Long.toHexString(): String = Hex.encodeHexString(ByteBuffer.allocate(java.lang.Long.BYTES).order(ByteOrder.LITTLE_ENDIAN).putLong(this).array())
private fun Int.toHexString(): String = Hex.encodeHexString(ByteBuffer.allocate(java.lang.Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(this).array())

