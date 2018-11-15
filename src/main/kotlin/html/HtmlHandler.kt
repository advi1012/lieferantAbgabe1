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

import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

/**
 * Eine Handler-Function wird von der Router-Function
 * [de.hska.lieferant.Router.router] aufgerufen, nimmt einen Request entgegen
 * und erstellt den HTML-Response durch den Aufruf der Funktion `render`.
 * Die Daten werden an die (HTML-) _View_ durch ein (Concurrent-) _Model_
 * weitergegeben.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
interface HtmlHandler {
    /**
     * Startseite anzeigen
     * @param request Der eingehende Request
     * @return Ein Mono-Objekt mit dem Statuscode 200 und der eigentlichen Startseite.
     */
    fun home(request: ServerRequest): Mono<ServerResponse>

    /**
     * Alle Lieferanten anzeigen
     * @param request Der eingehende Request
     * @return Ein Mono-Objekt mit dem Statuscode 200 und der Resultatseite.
     */
    fun find(request: ServerRequest): Mono<ServerResponse>

    /**
     * Einen Lieferanten zu einer gegebenen ID (als Query-Parameter) anzeigen
     * @param request Der eingehende Request
     * @return Ein Mono-Objekt mit dem Statuscode 200 und der HTML-Seite mit den Lieferantendaten.
     */
    fun details(request: ServerRequest): Mono<ServerResponse>
}
