/*
 * Copyright (C) 2018 - present Juergen Zimmermann, Hochschule Karlsruhe
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
package de.hska.lieferant.html

import de.hska.lieferant.service.LieferantService
import org.springframework.http.MediaType.TEXT_HTML
import org.springframework.stereotype.Component
import org.springframework.ui.ConcurrentModel
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable
import reactor.core.publisher.Mono

/**
 * Eine Handler-Function wird von der Router-Function
 * [de.hska.lieferant.Router.router] aufgerufen, nimmt einen Request entgegen
 * und erstellt den HTML-Response durch den Aufruf der Funktion `render`.
 * Die Daten werden an die (HTML-) _View_ durch ein (Concurrent-) _Model_
 * weitergegeben.
 *
 * Alternativen zu ThymeLeaf sind z.B. Mustache oder FreeMarker.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 *
 * @constructor Einen ThymeleafHandler mit einem injizierten [LieferantService]
 *      erzeugen.
 */
@Suppress("unused")
@Component
class ThymeleafHandler(private val service: LieferantService) : HtmlHandler {
    override fun home(request: ServerRequest) =
        ServerResponse.ok().contentType(TEXT_HTML).render("index")

    override fun find(request: ServerRequest): Mono<ServerResponse> {
        val lieferantn = ConcurrentModel()
                .addAttribute("lieferantn", ReactiveDataDriverContextVariable(
                        service.findAll(), 1
                ))

        return ServerResponse.ok().contentType(TEXT_HTML).render("suche", lieferantn)
    }

    override fun details(request: ServerRequest): Mono<ServerResponse> {
        val lieferant = ConcurrentModel()
        request.queryParam("id").ifPresent {
            lieferant.addAttribute("lieferant", service.findById(it))
        }

        return ServerResponse.ok().contentType(TEXT_HTML).render("details", lieferant)
    }
}
