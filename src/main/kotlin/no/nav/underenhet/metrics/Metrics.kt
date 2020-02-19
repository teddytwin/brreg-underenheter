package no.nav.underenhet.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Summary

private const val NAMESPACE = "brreg_underenheter"

object Metrics {
    val networkCallSummary: Summary = Summary.build()
        .namespace(NAMESPACE)
        .name("network_call_summary")
        .help("Summary for networked call times")
        .labelNames("call_name")
        .register()
    val networkCallFailuresCounter: Counter = Counter.build()
        .namespace(NAMESPACE)
        .name("network_call_failures")
        .help("Number of network call failures")
        .labelNames("call_name")
        .register()
}
