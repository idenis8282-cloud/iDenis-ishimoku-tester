package com.example.ichimokubacktester.domain

fun highest(values: List<Double>, index: Int, length: Int): Double? {
    if (index - length + 1 < 0) return null
    return values.subList(index - length + 1, index + 1).maxOrNull()
}

fun lowest(values: List<Double>, index: Int, length: Int): Double? {
    if (index - length + 1 < 0) return null
    return values.subList(index - length + 1, index + 1).minOrNull()
}

class IchimokuCalculator {
    private fun mid(src: List<Double>, index: Int, length: Int): Double? {
        if (index - length + 1 < 0) return null
        val slice = src.subList(index - length + 1, index + 1)
        return (slice.maxOrNull()!! + slice.minOrNull()!!) / 2.0
    }

    fun calculate(candles: List<Candle>, index: Int, tkLen: Int, kjLen: Int, skLen: Int): IchimokuValues? {
        val close = candles.map { it.close }
        val tenkan = mid(close, index, tkLen) ?: return null
        val kijun = mid(close, index, kjLen) ?: return null
        val senkouA = (tenkan + kijun) / 2.0

        val high = candles.map { it.high }
        val low = candles.map { it.low }
        val hh = highest(high, index, skLen) ?: return null
        val ll = lowest(low, index, skLen) ?: return null
        val senkouB = (hh + ll) / 2.0

        return IchimokuValues(tenkan, kijun, senkouA, senkouB)
    }
}

data class IchimokuValues(
    val tenkan: Double,
    val kijun: Double,
    val senkouA: Double,
    val senkouB: Double
)
