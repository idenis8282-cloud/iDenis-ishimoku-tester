package com.example.ichimokubacktester.domain

fun resample(candles: List<Candle>, factor: Int): List<Candle> =
    candles.chunked(factor).mapNotNull { chunk ->
        if (chunk.isEmpty()) null else Candle(
            time = chunk.first().time,
            open = chunk.first().open,
            high = chunk.maxOf { it.high },
            low = chunk.minOf { it.low },
            close = chunk.last().close,
            volume = chunk.sumOf { it.volume }
        )
    }

enum class HtfState { BULL, BEAR, NEUTRAL }

fun htfState(candles: List<Candle>, index: Int, ich: IchimokuCalculator, tk: Int, kj: Int, sk: Int): HtfState {
    if (index !in candles.indices) return HtfState.NEUTRAL
    val v = ich.calculate(candles, index, tk, kj, sk) ?: return HtfState.NEUTRAL
    val close = candles[index].close
    return when {
        close > maxOf(v.senkouA, v.senkouB) && v.senkouA > v.senkouB -> HtfState.BULL
        close < minOf(v.senkouA, v.senkouB) && v.senkouA < v.senkouB -> HtfState.BEAR
        else -> HtfState.NEUTRAL
    }
}

class IchimokuIDenisSignalEngine(private val params: IchimokuParams) {
    private val ich = IchimokuCalculator()

    fun run(candles5m: List<Candle>): List<SignalBar> {
        val out = ArrayList<SignalBar>(candles5m.size)
        var prevSig = 0

        val c15m = resample(candles5m, 3)
        val c1h = resample(candles5m, 12)
        val c4h = resample(candles5m, 48)

        fun isUpAt(i: Int): Boolean {
            val v = ich.calculate(candles5m, i, params.tenkanLen, params.kijunLen, params.senkouBLen) ?: return false
            return candles5m[i].close > maxOf(v.senkouA, v.senkouB)
        }
        fun isDownAt(i: Int): Boolean {
            val v = ich.calculate(candles5m, i, params.tenkanLen, params.kijunLen, params.senkouBLen) ?: return false
            return candles5m[i].close < minOf(v.senkouA, v.senkouB)
        }

        for (i in candles5m.indices) {
            val v = ich.calculate(candles5m, i, params.tenkanLen, params.kijunLen, params.senkouBLen)
            if (v == null) {
                out += SignalBar(candles5m[i].time, false, false, false, false, false, false, prevSig)
                continue
            }

            val buy = isUpAt(i) && (i == 0 || !isUpAt(i - 1))
            val sell = isDownAt(i) && (i == 0 || !isDownAt(i - 1))

            val sig = when {
                buy -> 1
                sell -> -1
                else -> prevSig
            }

            val longcr = sig == 1 && prevSig != 1
            val shortcr = sig == -1 && prevSig != -1

            val buyAllowed =
                (!params.useHTF15m || htfState(c15m, i / 3, ich, params.tenkanLen, params.kijunLen, params.senkouBLen) == HtfState.BULL) &&
                (!params.useHTF1h || htfState(c1h, i / 12, ich, params.tenkanLen, params.kijunLen, params.senkouBLen) == HtfState.BULL) &&
                (!params.useHTF4h || htfState(c4h, i / 48, ich, params.tenkanLen, params.kijunLen, params.senkouBLen) == HtfState.BULL)

            val sellAllowed =
                (!params.useHTF15m || htfState(c15m, i / 3, ich, params.tenkanLen, params.kijunLen, params.senkouBLen) == HtfState.BEAR) &&
                (!params.useHTF1h || htfState(c1h, i / 12, ich, params.tenkanLen, params.kijunLen, params.senkouBLen) == HtfState.BEAR) &&
                (!params.useHTF4h || htfState(c4h, i / 48, ich, params.tenkanLen, params.kijunLen, params.senkouBLen) == HtfState.BEAR)

            out += SignalBar(
                time = candles5m[i].time,
                buy = buy,
                sell = sell,
                longcr = longcr,
                shortcr = shortcr,
                buyAllowed = buyAllowed,
                sellAllowed = sellAllowed,
                sig = sig
            )

            prevSig = sig
        }

        return out
    }
}
