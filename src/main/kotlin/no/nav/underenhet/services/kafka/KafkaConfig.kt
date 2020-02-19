package no.nav.underenhet.services.kafka

import java.util.Properties
import no.nav.underenhet.Configuration
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.ByteArraySerializer

internal fun Configuration.loadKafkaConfig(): Properties = Properties().also { property ->
    property[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = kafka.bootstrapServers
    property[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
    property[ConsumerConfig.GROUP_ID_CONFIG] = kafka.appId
    property[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = ByteArrayDeserializer::class.java
    property[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = ByteArrayDeserializer::class.java

    property[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "1000"
    property[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "false"

    property[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = ByteArraySerializer::class.java
    property[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = ByteArraySerializer::class.java

    if (kafka.security == "TRUE") {
        property[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SASL_SSL"
        property[SaslConfigs.SASL_JAAS_CONFIG] = "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username=\"${application.username}\" password=\"${application.password}\";"
        property[SaslConfigs.SASL_MECHANISM] = "PLAIN"
    }
}
