/*
 * Copyright (C) 2016 - present Juergen Zimmermann, Hochschule Karlsruhe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.hska.lieferant.config

import org.springframework.boot.Banner
import org.springframework.boot.SpringBootVersion
import org.springframework.core.SpringVersion
import org.springframework.security.core.SpringSecurityCoreVersion
import java.net.InetAddress

/**
 * Singleton-Klasse, um sinnvolle Konfigurationswerte für den Microservice vorzugeben.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
object Settings {
    /**
     * Konstante für das Spring-Profile "dev".
     */
    const val DEV = "dev"

    @Suppress("MayBeConstant")
    private val version = "1.0"

    /**
     * Banner für den Start des Microservice in der Konsole.
     */
    val BANNER = Banner { _, _, out ->
        out.println("""

            |(C) Juergen Zimmermann,  Hochschule Karlsruhe
            |Version          $version
            |Spring Boot      ${SpringBootVersion.getVersion()}
            |Spring Security  ${SpringSecurityCoreVersion.getVersion()}
            |Spring Framework ${SpringVersion.getVersion()}
            |Kotlin           ${KotlinVersion.CURRENT}
            |OpenJDK          ${System.getProperty("java.runtime.version")}
            |Betriebssystem   ${System.getProperty("os.name")}
            |Rechnername      ${InetAddress.getLocalHost().hostName}
            |""".trimMargin("|"))
    }

    private val parentPkgName by lazy {
        val pkgName = Settings::class.java.`package`.name
        pkgName.substringBeforeLast('.')
    }

    private val appName = parentPkgName.substringAfterLast('.')

    /**
     * Properties, die berücksichtigt werden, wenn der Microservice in der
     * Konsole gestartet wird.
     */
    val PROPS = mapOf(
            "spring.application.name" to appName,
            "spring.devtools.livereload.enabled" to false,
            "spring.devtools.restart.trigger-file=" to "/restart.txt",
            // "spring.http.log-request-details" to true,
            "spring.profiles.default" to "prod",
            // Functional bean definition Kotlin DSL
            // "context.initializer.classes" to "$parentPkgName.BeansInitializer"

            "spring.jackson.serialization.indent_output" to true,
            "spring.jackson.default-property-inclusion" to "non_null",

            // -Dreactor.trace.operatorStacktrace=true
            "spring.reactor.stacktrace-mode.enabled" to true,

            "spring.security.user.password" to "p",

            // "server.compression.enabled" to true,
            // "server.compression.mime-types" to "application/json",

            "server.ssl.enabled" to true,
            "server.ssl.key-alias" to "zimmermann",
            "server.ssl.key-password" to "zimmermann",
            "server.ssl.key-store" to "classpath:keystore.p12",
            "server.ssl.key-store-password" to "zimmermann",
            "server.ssl.key-store-type" to "PKCS12",
            "server.ssl.protocol" to "TLSv1.3",
            "server.ssl.trust-store" to "classpath:truststore.p12",
            "server.ssl.trust-store-password" to "zimmermann",
            "server.ssl.trust-store-type" to "PKCS12",

            // TODO https://youtrack.jetbrains.com/issue/IDEA-188869 Kein HTTP/2 wegen "REST Client" Texteditor
            // "server.http2.enabled" to true,

            "management.endpoints.web.exposure.include" to "*",
            "management.endpoint.health.enabled" to true,
            // "management.endpoint.health.show-details" to true,
            "management.endpoint.mappings.enabled" to true,
            "management.endpoint.shutdown.enabled" to true,

            "spring.cloud.service-registry.auto-registration.enabled" to false,
            "eureka.client.enabled" to false,

            "spring.thymeleaf.cache" to false,
            "spring.thymeleaf.enabled" to true)

    // "server.error.whitelabel.enabled" to false,
}
