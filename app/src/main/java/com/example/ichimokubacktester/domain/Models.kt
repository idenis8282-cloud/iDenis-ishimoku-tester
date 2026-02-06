package com.example.ichimokubacktester.domain

enum class ExecutionModel { ON_CLOSE, ON_NEXT_OPEN }
enum class PositionSizing { PCT_EQUITY, FIXED_USDT }
enum class Side { LONG, SHORT }
enum class ExitReason { SIGNAL, STOP_LOSS, TAKE_PROFIT }

data class Candle(
    val time: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
)

data class IchimokuParams(
    val tenkanLen: Int,
    val kijunLen: Int,
    val senkouBLen: Int,
    val useHTF15m: Boolean = true,
    val useHTF1h: Boolean = false,
    val useHTF4h: Boolean = false
)

data class BacktestConfig(
    val symbol: String,
    val timeframeMinutes: Int = 5,
    val startMs: Long,
    val endMs: Long,
    val initialBalanceUsdt: Double = 1000.0,
    val leverage: Int = 5,
    val allowShort: Boolean = true,
    val executionModel: ExecutionModel = ExecutionModel.ON_NEXT_OPEN,
    val feeRate: Double = 0.0004,
    val slippageRate: Double = 0.0002,
    val sizing: PositionSizing = PositionSizing.PCT_EQUITY,
    val sizingValue: Double = 0.10
)

data class SignalBar(
    val time: Long,
    val buy: Boolean,
    val sell: Boolean,
    val longcr: Boolean,
    val shortcr: Boolean,
    val buyAllowed: Boolean,
    val sellAllowed: Boolean,
    val sig: Int
)

data class Trade(
    val entryTime: Long,
    val exitTime: Long,
    val side: Side,
    val entryPrice: Double,
    val exitPrice: Double,
    val qty: Double,
    val pnl: Double,
    val fees: Double,
    val reason: ExitReason
)

data class Position(
    val side: Side,
    val entryPrice: Double,
    val qty: Double,
    val entryTime: Long
)

data class BacktestReport(
    val trades: List<Trade>,
    val equityCurve: List<Pair<Long, Double>>,
    val netProfit: Double,
    val maxDrawdown: Double,
    val winRate: Double,
    val profitFactor: Double
)
