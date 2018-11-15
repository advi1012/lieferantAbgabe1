@file:Suppress("TooManyFunctions")

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
package de.hska.lieferant.service

import de.hska.lieferant.config.logger
import de.hska.lieferant.entity.Adresse
import de.hska.lieferant.entity.FamilienstandType.VERHEIRATET
import de.hska.lieferant.entity.GeschlechtType.WEIBLICH
import de.hska.lieferant.entity.InteresseType.LESEN
import de.hska.lieferant.entity.InteresseType.REISEN
import de.hska.lieferant.entity.Lieferant
import de.hska.lieferant.entity.Umsatz
import java.math.BigDecimal.ONE
import java.net.URL
import java.time.LocalDate
import java.util.Currency.getInstance
import java.util.Locale.GERMANY
import java.util.UUID.randomUUID
import javax.validation.Valid
import org.springframework.stereotype.Component
import org.springframework.util.MultiValueMap
import org.springframework.validation.annotation.Validated
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.publisher.toMono
import kotlin.random.Random

/**
 * Anwendungslogik für Lieferanten.
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@Component
@Validated
class LieferantService {
    /**
     * Einen Lieferanten anhand seiner ID suchen
     * @param id Die Id des gesuchten Lieferanten
     * @return Der gefundene lieferant oder ein leeres Mono-Objekt
     */
    fun findById(id: String) = if (id[0].toLowerCase() == 'f') {
        logger.trace("Kein lieferant gefunden")
        Mono.empty()
    } else {
        val lieferant = createLieferant(id)
        logger.trace("Gefunden: {}", lieferant)
        lieferant.toMono()
    }

    private fun findByEmail(email: String): Mono<Lieferant> {
        var id = randomUUID().toString()
        if (id[0] == 'f') {
            // damit findById nicht empty() liefert (s.u.)
            id = id.replaceFirst("f", "1")
        }

        return findById(id).flatMap {
            it.copy(email = email).toMono()
        }
    }

    /**
     * Lieferanten anhand von Suchkriterien suchen
     * @param queryParams Die Suchkriterien
     * @return Die gefundenen Lieferanten oder ein leeres Flux-Objekt
     */
    @Suppress("ReturnCount")
    fun find(queryParams: MultiValueMap<String, String>): Flux<Lieferant> {
        if (queryParams.isEmpty()) {
            return findAll()
        }

        for ((key, value) in queryParams) {
            if (value.size != 1) {
                return Flux.empty()
            }

            val paramValue = value[0]
            when (key) {
                "email" -> return findByEmail(paramValue).flux()
                "nachname" -> return findByNachname(paramValue)
            }
        }

        return Flux.empty()
    }

    /**
     * Alle Lieferanten als Flux ermitteln, wie sie später auch von der DB kommen.
     * @return Alle Lieferanten
     */
    fun findAll(): Flux<Lieferant> {
        val lieferanten = ArrayList<Lieferant>(maxLieferanten)
        repeat(maxLieferanten) {
            var id = randomUUID().toString()
            if (id[0] == 'f') {
                id = id.replaceFirst("f", "1")
            }
            val lieferant = createLieferant(id)
            lieferanten.add(lieferant)
        }
        return lieferanten.toFlux()
    }

    @Suppress("ReturnCount")
    private fun findByNachname(nachname: String): Flux<Lieferant> {
        if (nachname == "") {
            return findAll()
        }

        if (nachname[0] == 'Z') {
            return Flux.empty()
        }

        val anzahl = nachname.length
        val lieferanten = ArrayList<Lieferant>(anzahl)
        repeat(anzahl) {
            var id = randomUUID().toString()
            if (id[0] == 'f') {
                id = id.replaceFirst("f", "1")
            }
            val lieferant = createLieferant(id, nachname)
            lieferanten.add(lieferant)
        }
        return lieferanten.toFlux()
    }

    /**
     * Einen neuen Lieferanten anlegen.
     * @param lieferant Das Objekt des neu anzulegenden Lieferanten.
     * @return Der neu angelegte lieferant mit generierter ID
     */
    fun create(@Valid lieferant: Lieferant): Mono<Lieferant> {
        val neuerLieferant = lieferant.copy(id = randomUUID().toString())
        logger.trace("Neuer lieferant: {}", neuerLieferant)
        return neuerLieferant.toMono()
    }

    /**
     * Einen vorhandenen Lieferanten aktualisieren.
     * @param lieferant Das Objekt mit den neuen Daten (ohne ID)
     * @param id ID des zu aktualisierenden Lieferanten
     * @return Der aktualisierte lieferant oder ein leeres Mono-Objekt, falls
     * es keinen Lieferanten mit der angegebenen ID gibt
     */
    fun update(@Valid lieferant: Lieferant, id: String) =
        findById(id)
            .flatMap {
                val lieferantMitId = lieferant.copy(id = id)
                logger.trace("Aktualisierter lieferant: {}", lieferantMitId)
                lieferantMitId.toMono()
            }

    /**
     * Einen vorhandenen Lieferanten löschen.
     * @param lieferantId Die ID des zu löschenden Lieferanten.
     */
    fun deleteById(lieferantId: String) = findById(lieferantId)

    /**
     * Einen vorhandenen Lieferanten löschen.
     * @param email Die Email des zu löschenden Lieferanten.
     */
    fun deleteByEmail(email: String) = findByEmail(email)

    private fun createLieferant(id: String) = createLieferant(id, nachnamen.random())

    private fun createLieferant(id: String, nachname: String): Lieferant {
        @Suppress("MagicNumber")
        val minusYears = Random.nextLong(1, 60)
        val geburtsdatum = LocalDate.now().minusYears(minusYears)
        val homepage = URL("https://www.hska.de")
        val umsatz = Umsatz(betrag = ONE, waehrung = getInstance(GERMANY))
        val adresse = Adresse(plz = "12345", ort = "Testort")

        return Lieferant(
            id = id,
            nachname = nachname,
            email = "$nachname@hska.de",
            newsletter = true,
            geburtsdatum = geburtsdatum,
            umsatz = umsatz,
            homepage = homepage,
            geschlecht = WEIBLICH,
            familienstand = VERHEIRATET,
            interessen = listOf(LESEN, REISEN),
            adresse = adresse)
    }

    private companion object {
        const val maxLieferanten = 8
        val nachnamen = listOf("Moritz", "Peter", "Hans", "Winfried", "Max")
        val logger = logger()
    }
}
