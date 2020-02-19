package no.nav.underenhet

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import java.io.File
import java.io.FileNotFoundException

private val localProperties = ConfigurationMap(
    "application.port" to "8080",
    "brreg.dailypolltime" to "T05:30:00",
    "kafka.app.id" to "brreg-underenheter-v1-0",
    "kafka.bootstrap.servers" to "localhost:9092",
    "kafka.topic.underenhet" to "underenhettopic",
    "kafka.security" to "false",
    "serviceuser.username" to ("/var/run/secrets/nais.io/serviceuser/username".readFile() ?: "username"),
    "serviceuser.password" to ("/var/run/secrets/nais.io/serviceuser/password".readFile() ?: "password"),
    "brreg.url.underenhet" to "https://data.brreg.no/enhetsregisteret/api/underenheter/lastned"
)

private val config = systemProperties() overriding
    EnvironmentVariables() overriding
        localProperties

data class Configuration(
    val application: Application = Application(),
    val kafka: Kafka = Kafka(),
    val brreg: Brreg = Brreg()
) {
    data class Application(
        val port: Int = config[Key("application.port", intType)],
        val username: String = config[Key("serviceuser.username", stringType)],
        val password: String = config[Key("serviceuser.password", stringType)]
    )

    data class Kafka(
        val appId: String = config[Key("kafka.app.id", stringType)],
        val bootstrapServers: String = config[Key("kafka.bootstrap.servers", stringType)],
        val security: String = config[Key("kafka.security", stringType)].toUpperCase(),
        val underenhetTopic: String = config[Key("kafka.topic.underenhet", stringType)]
    )

    data class Brreg(
        val urlUnderenhet: String = config[Key("brreg.url.underenhet", stringType)],
        val dailyPollTime: String = config[Key("brreg.dailypolltime", stringType)]
    )
}

internal fun String.readFile(): String? =
    try {
        File(this).readText(Charsets.UTF_8)
    } catch (err: FileNotFoundException) {
        null
    }
