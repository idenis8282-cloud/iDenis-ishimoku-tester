package com.example.ichimokubacktester.domain

private fun calcQty(equity: Double, price: Double, config: BacktestConfig): Double {
    val notional = when (config.sizing) {
        PositionSizing.PCT_EQUITY -> equity * config.sizingValue
        PositionSizing.FIXED_USDT -> config.sizingValue
    }
    return (notional * config.leverage) / price
}

class BacktestEngine(private val config: BacktestConfig) {

    fun run(candles: List<Candle>, signals: List<SignalBar>): BacktestReport {
        var equity = config.initialBalanceUsdt
        var position: Position? = null

        val trades = mutableListOf<Trade>()
        val equityCurve = mutableListOf<Pair<Long, Double>>()

        for (i in 0 until candles.size - 1) {
            val next = candles[i + 1]
            val s = signals[i]

            if (position != null) {
                val shouldExit =
                    (position!!.side == Side.LONG && s.shortcr && s.sellAllowed) ||
                    (position!!.side == Side.SHORT && s.longcr && s.buyAllowed)

                if (shouldExit) {
                    val exitPrice = when (position!!.side) {
                        Side.LONG -> next.open * (1 - config.slippageRate)
                        Side.SHORT -> next.open * (1 + config.slippageRate)
                    }

                    val pnlRaw = when (position!!.side) {
                        Side.LONG -> (exitPrice - position!!.entryPrice) * position!!.qty
                        Side.SHORT -> (position!!.entryPrice - exitPrice) * position!!.qty
                    }

                    val fee = position!!.qty * (position!!.entryPrice + exitPrice) * config.feeRate
                    val pnl = pnlRaw - fee
                    equity += pnl

                    trades += Trade(
                        entryTime = position!!.entryTime,
                        exitTime = next.time,
                        side = position!!.side,
                        entryPrice = position!!.entryPrice,
                        exitPrice = exitPrice,
                        qty = position!!.qty,
                        pnl = pnl,
                        fees = fee,
                        reason = ExitReason.SIGNAL
                    )

                    position = null
                }
            }

            if (position == null) {
                if (s.longcr && s.buyAllowed) {
                    val entry = next.open * (1 + config.slippageRate)
                    val qty = calcQty(equity, entry, config)
                    equity -= qty * entry * config.feeRate
                    position = Position(Side.LONG, entry, qty, next.time)
                } else if (s.shortcr && s.sellAllowed && config.allowShort) {
                    val entry = next.open * (1 - config.slippageRate)
                    val qty = calcQty(equity, entry, config)
                    equity -= qty * entry * config.feeRate
                    position = Position(Side.SHORT, entry, qty, next.time)
                }
            }

            equityCurve += candles[i].time to equity
        }

        return buildReport(trades, equityCurve, config.initialBalanceUsdt)
    }
}

private fun buildReport(trades: List<Trade>, equity: List<Pair<Long, Double>>, start: Double): BacktestReport {
    val end = equity.lastOrNull()?.second ?: start
    val net = end - start

    var peak = start
    var maxDD = 0.0
    for ((_, eq) in equity) {
        peak = maxOf(peak, eq)
        val dd = if (peak > 0) (peak - eq) / peak else 0.0
        maxDD = maxOf(maxDD, dd)
    }

    val wins = trades.count { it.pnl > 0 }
    val winRate = if (trades.isNotEmpty()) wins.toDouble() / trades.size else 0.0

    val grossWin = trades.filter { it.pnl > 0 }.sumOf { it.pnl }
    val grossLoss = trades.filter { it.pnl < 0 }.sumOf { -it.pnl }.coerceAtLeast(1e-6)
    val pf = grossWin / grossLoss

    return BacktestReport(
        trades = trades,
        equityCurve = equity,
        netProfit = net,
        maxDrawdown = maxDD,
        winRate = winRate,
        profitFactor = pf
    )
}
