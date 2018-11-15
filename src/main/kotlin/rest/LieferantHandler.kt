/*
 * Copyright (C) 2017 - present Juergen Zimmermann, Hochschule Karlsruhe
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
package de.hska.lieferant.rest

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import de.hska.lieferant.config.logger
import de.hska.lieferant.Router.Companion.idPathVar
import de.hska.lieferant.entity.Lieferant
import de.hska.lieferant.rest.util.LieferantConstraintViolation
import de.hska.lieferant.rest.util.LieferantPatcher
import de.hska.lieferant.rest.util.PatchOperation
import de.hska.lieferant.rest.util.InvalidInteresseException
import de.hska.lieferant.rest.util.itemLinks
import de.hska.lieferant.rest.util.singleLinks

import de.hska.lieferant.service.LieferantService
import java.net.URI
import javax.validation.ConstraintViolationException
import org.springframework.core.codec.DecodingException
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.bodyToFlux
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse
    .badRequest
import org.springframework.web.reactive.function.server.ServerResponse.created
import org.springframework.web.reactive.function.server.ServerResponse.noContent
import org.springframework.web.reactive.function.server.ServerResponse.notFound
import org.springframework.web.reactive.function.server.ServerResponse.ok
import org.springframework.web.reactive.function.server.ServerResponse.status
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Mono
import reactor.core.publisher.onErrorResume
import reactor.core.publisher.toMono

/**
 * Eine Handler-Function wird von der Router-Function
 * [de.hska.lieferant.Router.router] aufgerufen, nimmt einen Request entgegen
 * und erstellt den Response.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @constructor Einen LieferantHandler mit einem injizierten [LieferantService]
 *      erzeugen.
 */
@Component
@Suppress("TooManyFunctions")
class LieferantHandler(private val service: LieferantService) {
    /**
     * Suche anhand der lieferant-ID
     * @param request Der eingehende Request
     * @return Ein Mono-Objekt mit dem Statuscode 200 und dem gefundenen
     *      Lieferanten einschließlich HATEOAS-Links, oder aber Statuscode 204.
     */
    fun findById(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable(idPathVar)
        val lieferant = service.findById(id).map {
            it._links = request.uri().singleLinks()
            it
        }

        return lieferant.flatMap { ok().body(it.toMono()) }
            .switchIfEmpty(notFound().build())
    }

    /**
     * Suche mit diversen Suchkriterien als Query-Parameter. Es wird
     * `Mono<List<lieferant>>` statt `Flux<lieferant>` zurückgeliefert, damit
     * auch der Statuscode 204 möglich ist.
     * @param request Der eingehende Request mit den Query-Parametern.
     * @return Ein Mono-Objekt mit dem Statuscode 200 und einer Liste mit den
     *      gefundenen Lieferanten einschließlich HATEOAS-Links, oder aber
     *      Statuscode 204.
     */
    fun find(request: ServerRequest): Mono<ServerResponse> {
        val queryParams = request.queryParams()

        // https://stackoverflow.com/questions/45903813/...
        //     ...webflux-functional-how-to-detect-an-empty-flux-and-return-404
        val lieferanten = service.find(queryParams).map {
            if (it.id != null) {
                it.links = request.uri().itemLinks(it.id)
            }
            it
        }.collectList()

        logger.debug("Gefundene Lieferanten: {}", lieferanten)
        return lieferanten.flatMap {
            if (it.isEmpty()) notFound().build() else ok().body(it.toMono())
        }
    }

    /**
     * Einen neuen lieferant-Datensatz anlegen.
     * @param request Der eingehende Request mit dem lieferant-Datensatz im Body.
     * @return Response mit Statuscode 201 einschließlich Location-Header oder
     *      Statuscode 400 falls Constraints verletzt sind oder der
     *      JSON-Datensatz syntaktisch nicht korrekt ist.
     */
    fun create(request: ServerRequest): Mono<ServerResponse> =
        request.bodyToMono<Lieferant>()
            .flatMap { service.create(it) }
            .flatMap {
                logger.trace("lieferant abgespeichert: {}", it)
                val location = URI("${request.uri()}${it.id}")
                created(location).build()
            }
            .onErrorResume(ConstraintViolationException::class) {
                if (it.constraintViolations.isEmpty()) {
                    badRequest().build()
                } else {
                    val violations =
                        it.constraintViolations.map { violation ->
                            // Funktion "create" mit Parameter "lieferant"
                            LieferantConstraintViolation(property = violation.propertyPath.toString()
                                .replace("create.lieferant.", ""),
                                message = it.message)
                        }
                    logger.trace("violations [create]: {}", violations)
                    badRequest().body(violations.toMono())
                }
            }
            .onErrorResume(DecodingException::class) {
                handleDecodingException(it)
            }

    /**
     * Einen vorhandenen lieferant-Datensatz überschreiben.
     * @param request Der eingehende Request mit dem neuen lieferant-Datensatz im
     *      Body.
     * @return Response mit Statuscode 204 oder Statuscode 400, falls
     *      Constraints verletzt sind oder der JSON-Datensatz syntaktisch nicht
     *      korrekt ist.
     */
    fun update(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable(idPathVar)
        return request.bodyToMono<Lieferant>()
            .flatMap { service.update(it, id) }
            .flatMap { noContent().build() }
            .switchIfEmpty(notFound().build())
            .onErrorResume(ConstraintViolationException::class) {
                if (it.constraintViolations.isEmpty()) {
                    badRequest().build()
                } else {
                    val violations = it.constraintViolations.map { violation ->
                        // Funktion "update" mit Parameter "lieferant"
                        LieferantConstraintViolation(
                            property = violation.propertyPath.toString().replace("update.lieferant.", ""),
                            message = it.message)
                    }
                    logger.trace("violations [update]: {}", violations)
                    badRequest().body(violations.toMono())
                }
            }
            .onErrorResume(DecodingException::class) {
                handleDecodingException(it)
            }
    }

    /**
     * Einen vorhandenen lieferant-Datensatz durch PATCH aktualisieren.
     * @param request Der eingehende Request mit dem PATCH-Datensatz im Body.
     * @return Response mit Statuscode 204 oder Statuscode 400, falls
     *      Constraints verletzt sind oder der JSON-Datensatz syntaktisch nicht
     *      korrekt ist.
     */
    fun patch(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable(idPathVar)

        return request.bodyToFlux<PatchOperation>()
            // Die einzelnen Patch-Operationen als Liste in einem Mono
            .collectList()
            .flatMap { patchOps ->
                service.findById(id)
                    .flatMap {
                        val kundePatched = LieferantPatcher.patch(it, patchOps)
                        logger.trace("lieferant mit Patch-Ops: {}", kundePatched)
                        service.update(kundePatched, id)
                    }
                    .flatMap { noContent().build() }
                    .switchIfEmpty(notFound().build())
            }
            .onErrorResume(ConstraintViolationException::class) {
                if (it.constraintViolations.isEmpty()) {
                    badRequest().build()
                } else {
                    val violations = it.constraintViolations.map { violation ->
                        LieferantConstraintViolation(
                            property = violation.propertyPath.toString().replace("update.lieferant.", ""),
                            message = it.message)
                    }
                    logger.trace("violations [patch]: {}", violations)
                    badRequest().body(violations.toMono())
                }
            }
            .onErrorResume(InvalidInteresseException::class) {
                val msg = it.message
                if (msg == null) {
                    badRequest().build()
                } else {
                    badRequest().body(msg.toMono())
                }
            }
            .onErrorResume(DecodingException::class) {
                handleDecodingException(it)
            }
    }

    /**
     * Einen vorhandenen Lieferanten anhand seiner ID löschen.
     * @param request Der eingehende Request mit der ID als Pfad-Parameter.
     * @return Response mit Statuscode 204.
     */
    fun deleteById(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable(idPathVar)
        return service.deleteById(id).flatMap { noContent().build() }
    }

    /**
     * Einen vorhandenen Lieferanten anhand seiner Emailadresse löschen.
     * @param request Der eingehende Request mit der Emailadresse als
     *      Query-Parameter.
     * @return Response mit Statuscode 204.
     */
    fun deleteByEmail(request: ServerRequest): Mono<ServerResponse> {
        val email = request.queryParam("email")
        return if (email.isPresent) {
            return service.deleteByEmail(email.get())
                .flatMap { noContent().build() }
        } else {
            notFound().build()
        }
    }

    private fun handleDecodingException(e: DecodingException): Mono<ServerResponse> {
        val exception = e.cause
        return when (exception) {
            is JsonParseException -> {
                logger.debug(exception.message)
                badRequest().syncBody(exception.message ?: "")
            }
            is InvalidFormatException -> {
                logger.debug("${exception.message}")
                badRequest().syncBody(exception.message ?: "")
            }
            else -> status(INTERNAL_SERVER_ERROR).build()
        }
    }

    private companion object {
        val logger = logger()
    }
}
