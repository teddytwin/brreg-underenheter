package no.nav.underenhet

import com.google.common.collect.MapDifference
import com.google.common.collect.Maps
import io.ktor.application.Application
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.prometheus.client.hotspot.DefaultExports
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import no.nav.underenhet.api.nais
import no.nav.underenhet.common.ApplicationState
import no.nav.underenhet.common.DEFAULT_RETRY_ATTEMPTS
import no.nav.underenhet.common.retry
import no.nav.underenhet.services.brreg.BrregService
import no.nav.underenhet.services.kafka.KafkaService
import no.nav.underenhet.services.kafka.loadKafkaConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer

private val logger = KotlinLogging.logger { }
private val applicationContext = Executors.newFixedThreadPool(8).asCoroutineDispatcher() + MDCContext()

fun main() {
    runBlocking {
        val applicationState = ApplicationState()
        val config = Configuration()
        DefaultExports.initialize()

        val app = embeddedServer(Netty, config.application.port, module = { mainModule(config, applicationState) }).start(wait = false)

        try {
            val job = launch {
                while (applicationState.running) {
                    delay(100)
                }
            }

            Runtime.getRuntime().addShutdownHook(Thread {
                logger.info { "Shutdown hook called, shutting down gracefully" }
                applicationState.fail()
                app.stop(1, 1, TimeUnit.SECONDS)
            })

            applicationState.initialized = true
            logger.info { "Application ready" }

            job.join()
        } finally {
            exitProcess(1)
        }
    }
}

fun Application.mainModule(config: Configuration, applicationState: ApplicationState = ApplicationState(initialized = true, running = true)) {

    val kafkaProducer: KafkaProducer<ByteArray, ByteArray> = KafkaProducer(config.loadKafkaConfig())
    val kafkaConsumer: KafkaConsumer<ByteArray, ByteArray> = KafkaConsumer<ByteArray, ByteArray>(config.loadKafkaConfig())
        .also { consumer -> consumer.subscribe(listOf(config.kafka.underenhetTopic)) }
    val kafkaService = KafkaService()
    val brregService = BrregService(config)

    routing {
        nais(readinessCheck = { applicationState.initialized }, livenessCheck = { applicationState.running })
    }

    launchBackgroundTask(applicationState = applicationState, callName = "underenheter", maxDelay = getSecondsToNextDailyPollTime(config.brreg.dailyPollTime)) {

            val underenheter = brregService.getUnderenheter()

            val underEnheterFromBrreg = brregService.underEnheterFromBrreg(underenheter)
            val underenheterOnTopic =
                kafkaService.getUnderenheterOnTopic(kafkaConsumer)

            val diff: MapDifference<String, Int> = Maps.difference(underEnheterFromBrreg, underenheterOnTopic)
            val oppdaterteUnderEnheter = diff.entriesDiffering().keys as Set<String>
            val nyeUnderEnheter: Set<String> = diff.entriesOnlyOnLeft().keys
            val slettedeUnderEnheter: Set<String> = diff.entriesOnlyOnRight().keys // TODO:: Should be deleted??
            kafkaService.processUnderEnheter(underenheter, nyeUnderEnheter.plus(oppdaterteUnderEnheter), config.kafka.underenhetTopic, kafkaProducer)
    }
}

private fun CoroutineScope.launchBackgroundTask(
    applicationState: ApplicationState,
    callName: String,
    attempts: Int = DEFAULT_RETRY_ATTEMPTS,
    maxDelay: Long = 60_000L,
    block: suspend () -> Any
) {
    launch(applicationContext) {
        try {
            retry(callName = callName, attempts = attempts, maxDelay = maxDelay) {
                block()
            }
        } catch (e: Throwable) {
            logger.warn(e) { "Background task '$callName' was cancelled, failing self tests" }
            applicationState.running = false
        }
    }
}

private fun getSecondsToNextDailyPollTime(dailyPollTime: String) = Duration.between(
    LocalDateTime.now(),
    LocalDateTime.parse(LocalDate.now().plusDays(1).toString() + dailyPollTime))
    .toSeconds()
