package co.ready.candidateassessment.data.api

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * Custom Moshi adapter to handle the case where the API returns:
 * - "price": { ... } (object with price data)
 * - "price": false (when price data is not available)
 */
internal class PriceInfoAdapter : JsonAdapter<EthplorerApi.PriceInfo?>() {

    @FromJson
    override fun fromJson(reader: JsonReader): EthplorerApi.PriceInfo? = when (reader.peek()) {
        JsonReader.Token.BEGIN_OBJECT -> {
            reader.beginObject()
            var rate: Double? = null
            var currency: String? = null
            var diff: Double? = null
            var marketCapUsd: Double? = null
            var volume24h: Double? = null

            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "rate" ->
                        rate =
                            if (reader.peek() != JsonReader.Token.NULL) reader.nextDouble() else reader.nextNull()

                    "currency" ->
                        currency =
                            if (reader.peek() != JsonReader.Token.NULL) reader.nextString() else reader.nextNull()

                    "diff" ->
                        diff =
                            if (reader.peek() != JsonReader.Token.NULL) reader.nextDouble() else reader.nextNull()

                    "marketCapUsd" ->
                        marketCapUsd =
                            if (reader.peek() != JsonReader.Token.NULL) reader.nextDouble() else reader.nextNull()

                    "volume24h" ->
                        volume24h =
                            if (reader.peek() != JsonReader.Token.NULL) reader.nextDouble() else reader.nextNull()

                    else -> reader.skipValue() // Skip unknown fields
                }
            }
            reader.endObject()

            EthplorerApi.PriceInfo(
                rate = rate,
                currency = currency,
                diff = diff,
                marketCapUsd = marketCapUsd,
                volume24h = volume24h
            )
        }

        JsonReader.Token.BOOLEAN -> {
            // When price is false, skip it and return null
            reader.nextBoolean()
            null
        }

        JsonReader.Token.NULL -> {
            reader.nextNull<Unit>()
            null
        }

        else -> {
            reader.skipValue()
            null
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: EthplorerApi.PriceInfo?) {
        if (value == null) {
            writer.value(false)
        } else {
            writer.beginObject()
            writer.name("rate").value(value.rate)
            writer.name("currency").value(value.currency)
            writer.name("diff").value(value.diff)
            writer.name("marketCapUsd").value(value.marketCapUsd)
            writer.name("volume24h").value(value.volume24h)
            writer.endObject()
        }
    }
}
