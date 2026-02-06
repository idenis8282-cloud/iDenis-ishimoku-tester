package com.example.ichimokubacktester.data

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import com.example.ichimokubacktester.domain.Candle

class BinanceFuturesApi {
    private val client = OkHttpClient()

    fun fetchKlines5m(symbol: String, startMs: Long, endMs: Long): List<Candle> {
        val all = mutableListOf<Candle>()
        var start = startMs

        while (true) {
            val url = "https://fapi.binance.com/fapi/v1/klines" +
                    "?symbol=$symbol&interval=5m&startTime=$start&endTime=$endMs&limit=1500"

            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                val body = resp.body?.string() ?: error("Empty body")

                val arr = JSONArray(body)
                if (arr.length() == 0) return all

                for (i in 0 until arr.length()) {
                    val k = arr.getJSONArray(i)
                    all += Candle(
                        time = k.getLong(0),
                        open = k.getString(1).toDouble(),
                        high = k.getString(2).toDouble(),
                        low = k.getString(3).toDouble(),
                        close = k.getString(4).toDouble(),
                        volume = k.getString(5).toDouble()
                    )
                }
            }

            val lastOpenTime = all.last().time
            val nextStart = lastOpenTime + 5L * 60L * 1000L
            if (nextStart >= endMs) break
            if (nextStart <= start) break
            start = nextStart
        }

        return all
    }
}
