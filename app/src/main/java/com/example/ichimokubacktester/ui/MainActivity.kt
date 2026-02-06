package com.example.ichimokubacktester.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.ichimokubacktester.data.BinanceFuturesApi
import com.example.ichimokubacktester.domain.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    App()
                }
            }
        }
    }
}

@Composable
private fun App() {
    var symbol by remember { mutableStateOf("BTCUSDT") }
    var status by remember { mutableStateOf("Ready") }
    var report by remember { mutableStateOf<BacktestReport?>(null) }

    val api = remember { BinanceFuturesApi() }
    val scope = rememberCoroutineScope()

    val params = remember {
        IchimokuParams(
            tenkanLen = 9,
            kijunLen = 26,
            senkouBLen = 52,
            useHTF15m = true,
            useHTF1h = false,
            useHTF4h = false
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ichimoku Backtester (Binance Futures)")

        OutlinedTextField(
            value = symbol,
            onValueChange = { symbol = it.uppercase() },
            label = { Text("Symbol (e.g. BTCUSDT)") },
            singleLine = true
        )

        Button(onClick = {
            status = "Starting..."
            report = null
            scope.launch(Dispatchers.IO) {
                try {
                    status = "Loading klines..."
                    val end = System.currentTimeMillis()
                    val start = end - 7L * 24L * 60L * 60L * 1000L
                    val candles = api.fetchKlines5m(symbol, start, end)

                    status = "Signals..."
                    val signals = IchimokuIDenisSignalEngine(params).run(candles)

                    status = "Backtest..."
                    val cfg = BacktestConfig(symbol = symbol, timeframeMinutes = 5, startMs = start, endMs = end)
                    val rep = BacktestEngine(cfg).run(candles, signals)

                    report = rep
                    status = "Done: trades=${rep.trades.size}"
                } catch (e: Exception) {
                    status = "Error: ${e.message}"
                }
            }
        }) {
            Text("Backtest (7 days)")
        }

        Text(status)

        report?.let { r ->
            Text("NetProfit: %,.2f USDT".format(r.netProfit))
            Text("MaxDD: %,.2f %%".format(r.maxDrawdown * 100))
            Text("WinRate: %,.2f %%".format(r.winRate * 100))
            Text("PF: %,.2f".format(r.profitFactor))
            Text("Trades: ${r.trades.size}")
        }
    }
}
