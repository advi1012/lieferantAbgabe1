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

import de.hska.lieferant.service.LieferantService
import org.springframework.http.MediaType.TEXT_EVENT_STREAM
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import de.hska.lieferant.rest.util.itemLinks

/**
 * Eine Handler-Function wird von der Router-Function
 * [de.hska.lieferant.Router.router] aufgerufen, nimmt einen Request entgegen
 * und erstellt den Response.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @constructor Einen LieferantStreamHandler mit einem injizierten [LieferantService]
 *      erzeugen.
 */
@Component
class LieferantStreamHandler(private val service: LieferantService) {
    /**
     * Alle Lieferanten als Event-Stream zurückliefern.
     * @param request Das eingehende Request-Objekt.
     * @return Response mit dem MIME-Typ `text/event-stream`.
     */
    fun findAll(request: ServerRequest): Mono<ServerResponse> {
        val lieferanten = service.findAll().map {
            val listUri = request.uri()
            it.links = listUri.itemLinks(it.id!!)
            it
        }

        return ok().contentType(TEXT_EVENT_STREAM).body(lieferanten)
    }
}
