package domain

import java.time.{Period, ZonedDateTime}
import java.util.UUID

import infrastructure.SystemMessage
import infrastructure.SystemMessages.CannotBeEmpty

// NB! Do not instantiate directly
abstract case class Front(text: String)

object Front {
    def apply(get: String): Either[SystemMessage, Front] =
        get.trim match {
            case "" => Left(CannotBeEmpty("front"))
            case t => Right(new Front(t) {})
    }
}


// NB! Do not instantiate directly
abstract case class Back(text: String, exampleOfUse: String)

object Back {
    def apply(text: String, exampleOfUse: String): Either[SystemMessage, Back] =
        text.trim match {
            case "" => Left(CannotBeEmpty("back"))
            case t => Right(new Back(t, exampleOfUse) {})
    }
}

case class Wins(get: Int)

case class Losses(get: Int)

case class WinStreak(get: Int)

abstract case class CardStatistics(lastVisited: Option[ZonedDateTime], wins: Wins, losses: Losses, winStreak: WinStreak) {
    def rating(now: ZonedDateTime): Int = {
        val (visitedBonus, daysSinceLastVisited) = lastVisited match {
            case None => (0, 0)
            case Some(last) => (1, Period.between(last.toLocalDate, now.toLocalDate).getDays)
        }

        visitedBonus + daysSinceLastVisited - Math.pow(Math.max(0, winStreak.get - 3), 2).toInt
    }
}


// NB! Do not instantiate directly
abstract case class Card(id: UUID, front: Front, back: Back, stats: CardStatistics) {
    def win(now: ZonedDateTime): Card = {
        val newStats = new CardStatistics(Some(now), Wins(stats.wins.get + 1), stats.losses, WinStreak(stats.winStreak.get + 1)) {}
        new Card(id, front, back, newStats) {}
    }

    def lose(now: ZonedDateTime): Card = {
        val newStats = new CardStatistics(Some(now), stats.wins, Losses(stats.losses.get + 1), WinStreak(0)) {}
        new Card(id, front, back, newStats) {}
    }

    def rating(now: ZonedDateTime): Int = stats.rating(now)

}

object Card {
    def apply(front: Front, back: Back): Card = {
        val initStats = new CardStatistics(None, Wins(0), Losses(0), WinStreak(0)) {}
        new Card(UUID.randomUUID(), front, back, initStats) {}
    }
}

object PracticeDeck {
    def apply(noOfCards: Int, cards: Seq[Card], now: ZonedDateTime): Seq[Card] = {
        cards.sortBy(c => c.rating(now)).reverse.take(noOfCards)
    }
}