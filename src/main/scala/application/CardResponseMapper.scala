package application

import java.time.ZonedDateTime

import domain.Card
import infrastructure.DateFormat

object CardResponseMapper {
    def apply(card: Card): Dto.CardResponse = {
        val lastVisitedStr = card.stats.lastVisited.map(lv => DateFormat.standard.format(lv.toEpochSecond))

        Dto.CardResponse(card.id.toString, card.front.text, card.back.text, card.back.exampleOfUse,
            Dto.CardStatsResponse(lastVisitedStr, card.stats.wins.get, card.stats.losses.get,
                card.stats.winStreak.get, card.stats.rating(ZonedDateTime.now)))
    }
}
