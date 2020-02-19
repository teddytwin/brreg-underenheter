package no.nav.underenhet.services.kafka

import java.time.Duration
import mu.KotlinLogging
import no.nav.underenhet.proto.UnderenhetProto.UnderenhetKey
import no.nav.underenhet.proto.UnderenhetProto.UnderenhetValue
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.json.JSONArray

private val logger = KotlinLogging.logger { }

class KafkaService {

    fun processUnderEnheter(undernheter: JSONArray, entriesDiffering: Set<String>, topic: String, kafkaProducer: KafkaProducer<ByteArray, ByteArray>) {
        for (i in 0 until undernheter.length()) {
            val item = undernheter.getJSONObject(i)

            val key = item["organisasjonsnummer"].toString()
            if (entriesDiffering.contains(key)) {
                val underenhetKey = UnderenhetKey.newBuilder().apply {
                    this.orgnr = key
                }.build().toByteArray()
                val underenhetValue = UnderenhetValue.newBuilder().apply {
                    this.hashvalue = item.toString().hashCode()
                    this.json = item.toString()
                }.build().toByteArray()
                kafkaProducer.send(
                    ProducerRecord(topic, underenhetKey, underenhetValue)
                ).get().also {
                    logger.info("Message was routed to topic, ${it.topic()} ")
                }
            }
        }
    }

    fun getUnderenheterOnTopic(kafkaConsumer: KafkaConsumer<ByteArray, ByteArray>): Map<String, Int> {
        kafkaConsumer.seekToBeginning(emptyList())
        val records: ConsumerRecords<ByteArray, ByteArray> = kafkaConsumer.poll(Duration.ofMillis(1000L))

        return records.associateBy(
            { UnderenhetKey.parseFrom(it.key()).orgnr },
            { UnderenhetValue.parseFrom(it.value()).hashvalue }
        )
    }
}
