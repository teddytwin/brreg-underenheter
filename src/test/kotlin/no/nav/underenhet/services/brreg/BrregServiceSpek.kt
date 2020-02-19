package no.nav.underenhet.services.brreg

import com.google.common.collect.MapDifference
import com.google.common.collect.Maps
import java.nio.charset.StandardCharsets
import no.nav.common.KafkaEnvironment
import no.nav.underenhet.Configuration
import no.nav.underenhet.services.kafka.KafkaService
import no.nav.underenhet.services.kafka.loadKafkaConfig
import org.amshove.kluent.shouldEqual
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.json.JSONArray
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

const val BRREG_INITIAL_LOAD = "/underenheter_inital.json"
const val BRREG_UPDATED = "/underenheter_updated.json"
const val TOPIC = "testtopic"

object BrregServiceSpek : Spek({

    val kEnv = KafkaEnvironment(
        topicNames = listOf(TOPIC),
        withSecurity = false,
        autoStart = true
    )
    val config = Configuration(
        kafka = Configuration.Kafka(
            bootstrapServers = kEnv.brokersURL,
            underenhetTopic = TOPIC
        )
    )
    val brregService = BrregService(config)
    val kafkaService = KafkaService()

    val kafkaProducer: KafkaProducer<ByteArray, ByteArray> = KafkaProducer(config.loadKafkaConfig())
    val kafkaConsumer: KafkaConsumer<ByteArray, ByteArray> = KafkaConsumer<ByteArray, ByteArray>(config.loadKafkaConfig())
        .also { consumer -> consumer.subscribe(listOf(config.kafka.underenhetTopic)) }

    beforeGroup {
        kEnv.start()
    }

    describe("Verfify Brreg service") {
        context("Do initaial load of two records from brreg to kafkatopic, then update one and insert one new from brreg") {
            it("Initial load on topic") {
                val underEnheter = getUnderenheterFromResouce(BRREG_INITIAL_LOAD)
                val underEnheterFromBrreg = brregService.underEnheterFromBrreg(underEnheter)
                val underenheterOnTopic =
                    kafkaService.getUnderenheterOnTopic(kafkaConsumer)

                val diff: MapDifference<String, Int> = Maps.difference(underEnheterFromBrreg, underenheterOnTopic)
                val oppdaterteEnheter = diff.entriesDiffering().keys as Set<String>
                val nyeUnderEnheter: Set<String> = diff.entriesOnlyOnLeft().keys

                kafkaService.processUnderEnheter(underEnheter, nyeUnderEnheter.plus(oppdaterteEnheter), config.kafka.underenhetTopic, kafkaProducer)

                underEnheterFromBrreg.size shouldEqual 2
                underenheterOnTopic.size shouldEqual 0
                nyeUnderEnheter.size shouldEqual 2
                oppdaterteEnheter.size shouldEqual 0
            }
            it("Update topic with changes from brreg") {
                val underEnheter = getUnderenheterFromResouce(BRREG_UPDATED)
                val underEnheterFromBrreg = brregService.underEnheterFromBrreg(underEnheter)
                Thread.sleep(8000)
                val underenheterOnTopic =
                    kafkaService.getUnderenheterOnTopic(kafkaConsumer)

                val diff: MapDifference<String, Int> = Maps.difference(underEnheterFromBrreg, underenheterOnTopic)
                val oppdaterteEnheter = diff.entriesDiffering().keys as Set<String>
                val nyeUnderEnheter: Set<String> = diff.entriesOnlyOnLeft().keys

                kafkaService.processUnderEnheter(underEnheter, nyeUnderEnheter.plus(oppdaterteEnheter), config.kafka.underenhetTopic, kafkaProducer)

                underEnheterFromBrreg.size shouldEqual 4
                underenheterOnTopic.size shouldEqual 2
                nyeUnderEnheter.size shouldEqual 2
                oppdaterteEnheter.size shouldEqual 1
            }
            it("Update topic with no changes from brreg") {
                val underEnheter = getUnderenheterFromResouce(BRREG_UPDATED)
                val underEnheterFromBrreg = brregService.underEnheterFromBrreg(underEnheter)
                Thread.sleep(8000)
                val underenheterOnTopic =
                    kafkaService.getUnderenheterOnTopic(kafkaConsumer)

                val diff: MapDifference<String, Int> = Maps.difference(underEnheterFromBrreg, underenheterOnTopic)
                val oppdaterteEnheter = diff.entriesDiffering().keys as Set<String>
                val nyeUnderEnheter: Set<String> = diff.entriesOnlyOnLeft().keys
                kafkaService.processUnderEnheter(underEnheter, nyeUnderEnheter.plus(oppdaterteEnheter), config.kafka.underenhetTopic, kafkaProducer)

                underEnheterFromBrreg.size shouldEqual 4
                underenheterOnTopic.size shouldEqual 4
                nyeUnderEnheter.size shouldEqual 0
                oppdaterteEnheter.size shouldEqual 0
            }
        }
    }

    afterGroup {
        kEnv.tearDown()
    }
})

internal fun getUnderenheterFromResouce(resourcefile: String) =
    JSONArray(BrregService::class.java.getResourceAsStream(resourcefile).bufferedReader(
        StandardCharsets.UTF_8
    ).use { it.readText() })
