package no.nav.underenhet.services.brreg

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import java.nio.charset.StandardCharsets.UTF_8
import java.util.zip.GZIPInputStream
import no.nav.underenhet.Configuration
import no.nav.underenhet.common.DEFAULT_RETRY_ATTEMPTS
import no.nav.underenhet.common.retry
import org.json.JSONArray

class BrregService(private val configuration: Configuration) {

    suspend fun getUnderenheter(attempts: Int = DEFAULT_RETRY_ATTEMPTS): JSONArray =
        retry(callName = "Brreg underenheter", attempts = attempts) {
            getUnderenheterFromBrreg()
        }

    fun underEnheterFromBrreg(underEnheter: JSONArray): Map<String, Int> {
        var map = mutableMapOf<String, Int>()
        for (i in 0 until underEnheter.length()) {
            val item = underEnheter.getJSONObject(i)
            val key = item["organisasjonsnummer"].toString()
            val hashValue = item.toString().hashCode()
            map[key] = hashValue
        }
        return map
    }

    private suspend fun getUnderenheterFromBrreg(): JSONArray {
        val client = HttpClient()
        val jsonArray = ungzip(client.get<ByteArray>(configuration.brreg.urlUnderenhet))
        client.close()
        return jsonArray
    }

    private fun ungzip(content: ByteArray): JSONArray =
        JSONArray(GZIPInputStream(content.inputStream()).bufferedReader(UTF_8).use { it.readText() })
}
