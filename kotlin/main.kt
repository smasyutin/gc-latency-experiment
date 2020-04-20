import kotlin.system.measureNanoTime

// batch size for JVM taken from this article
// https://noelwelsh.com/posts/2020-02-13-benchmarking-graal-native-image.html
const val warmUpBatchSize = 10_000
const val msgSize = 1024

class Main {

    private fun createMessage(n: Int): ByteArray {
        val msg = ByteArray(msgSize)
        msg.fill(n.toByte())
        return msg
    }

    private fun pushMessage(c: Array<ByteArray>, id: Int): Long {
        return measureNanoTime {
            val m = createMessage(id)
            c[id] = m
        }
    }

    private fun runBatch(msgCount: Int, windowSize: Int) {
        println("Message count: $msgCount")
        println("Bugger size: $windowSize")

        val stats = Stats(msgCount)
        val elapsed = measureNanoTime {
            lateinit var c: Array<ByteArray>
            val allocated = measureNanoTime {
                c = Array(windowSize) { ByteArray(msgSize) }
            }
            for (i in 0 until msgCount) {
                stats.record(
                        pushMessage(c, i % windowSize)
                )
            }
            println("Allocation time: ${allocated / 1000}µs")
        }
        println("Push time stats:\n$stats")
        println("Total time: ${elapsed.toFloat() / 1_000_000_000}s")
    }

    private fun warmUp(windowSize: Int) {
        println("Warming-up...")
        runBatch(warmUpBatchSize, windowSize)
        println("...Warmed-up")
    }

    fun main(msgCount: Int, windowSize: Int) {
        warmUp(windowSize)
        runBatch(msgCount, windowSize)
    }

    private class Stats(val msgCount: Int) {
        var max: Long = 0
        // extended to 1s instead of 500ms
        var histogram = LongArray(500_000)

        fun record(tNano: Long) {
            val tMicro = (tNano / 1000).toInt()
            if (tMicro > max) {
                max = tMicro.toLong()
            }
            // nano to micro seconds
            var index = tMicro
            if (index >= histogram.size) {
                index = histogram.size -1
            }
            histogram[index]++
        }

        fun percentile(percent: Double): Int {
            val valuesCount = (msgCount * percent).toInt()
            var index = 0
            var n: Long = 0
            while (index < histogram.size && n < valuesCount) {
                n += histogram[index++]
            }
            return index
        }

        fun countLongerThanMs(ms: Int): Long {
            var result: Long = 0
            for (i in (ms * 1000) until histogram.size) {
                result += histogram[i]
            }
            return result
        }

        override fun toString(): String {
            return """max: ${max / 1000}ms
68.3 percentile: ${percentile(0.683)}µs
95.4 percentile: ${percentile(0.954)}µs
99.7 percentile: ${percentile(0.997)}µs
${countLongerThanMs(10)} longer than 10ms
${countLongerThanMs(30)} longer than 30ms
${countLongerThanMs(100)} longer than 100ms"""
        }
    }
}

fun main(args: Array<String>) {
    val main = Main()
    main.main(args[0].toInt(), args[1].toInt())
}

