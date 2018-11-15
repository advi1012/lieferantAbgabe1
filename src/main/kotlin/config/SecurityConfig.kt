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

import org.springframework.boot.actuate.autoconfigure.security.reactive.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod.DELETE
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpMethod.PATCH
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpMethod.PUT
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails
        .MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User

// https://github.com/spring-projects/spring-security/blob/5.0.2.RELEASE/samples/...
//       ...javaconfig/hellowebflux/src/main/java/sample/SecurityConfig.java

/**
 * Security-Konfiguration.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface SecurityConfig {
    /**
     * Bean-Definition, um den Zugriffsschutz an der REST-Schnittstelle zu
     * konfigurieren.
     *
     * @param http Injiziertes Objekt von `ServerHttpSecurity` als
     *      Ausgangspunkt für die Konfiguration.
     * @return Objekt von `SecurityWebFilterChain`
     */
    @Suppress("HasPlatformType")
    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity) = http.authorizeExchange()
            .pathMatchers(POST, lieferantPath).permitAll()
            .pathMatchers(GET, lieferantPath, lieferantIdPath).hasRole(adminRolle)
            .pathMatchers(PUT, lieferantPath).hasRole(adminRolle)
            .pathMatchers(PATCH, lieferantIdPath).hasRole(adminRolle)
            .pathMatchers(DELETE, lieferantIdPath).hasRole(adminRolle)

            .matchers(EndpointRequest.to("health")).permitAll()
            .matchers(EndpointRequest.toAnyEndpoint()).hasRole(endpointAdminRolle)

            .anyExchange().authenticated()

            .and()
            .httpBasic()
            .and()
            .formLogin().disable()
            .csrf().disable()
            // TODO Disable FrameOptions: Clickjacking
            .build()

    /**
     * Bean, um Test-User anzulegen. Dazu gehören jeweils ein Benutzername, ein
     * Passwort und diverse Rollen. Das wird in Beispiel 2 verbessert werden.
     *
     * @return Ein Objekt, mit dem diese Test-User verwaltet werden, z.B. für
     * die künftige Suche.
     */
    @Bean
    @Suppress("DEPRECATION")
    fun userDetailsRepository(): MapReactiveUserDetailsService {
        val admin = User.withDefaultPasswordEncoder()
                .username("admin")
                .password("p")
                .roles(adminRolle, lieferantRolle, endpointAdminRolle)
                .build()
        val alpha = User.withDefaultPasswordEncoder()
                .username("alpha")
                .password("p")
                .roles(lieferantRolle)
                .build()
        return MapReactiveUserDetailsService(admin, alpha)
    }

    @Suppress("MayBeConstant")
    companion object {
        private val adminRolle = "ADMIN"

        private val lieferantRolle = "KUNDE"

        private val endpointAdminRolle = "ENDPOINT_ADMIN"

        private val lieferantPath = "/"

        private val lieferantIdPath = "/*"
    }
}
