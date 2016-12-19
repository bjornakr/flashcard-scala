package unit

import java.time.ZonedDateTime
import java.util.UUID

import domain._
import infrastructure.SystemMessages
import org.scalatest.WordSpec

class CardSpec extends WordSpec {
    val validFrontText = "front"
    val validBackText = "back"
    val exampleOfUseText = "example"

    val validFront = new Front(validFrontText) {}
    val validBack = new Back(validBackText, exampleOfUseText) {}

    val initStats = new CardStatistics(None, Wins(0), Losses(0), WinStreak(0)) {}
    val untouchedCard = new Card(uuid, validFront, validBack, initStats) {}

    val uuid = UUID.randomUUID()
    val now = ZonedDateTime.now

    def validateEither[E, A](ea: Either[E, A])(f: A => Unit): Unit = {
        ea.right.map(f)
        ea.left.map(_ => assert(false))
    }

    "front" when {
        "text is empty" should {
            "give error" in {
                val result = Front("   ")
                assert(result == Left(SystemMessages.CannotBeEmpty("front")))
            }
        }

        "text has content" should {
            "give valid front" in {
                val result = Front(validFrontText)
                validateEither(result)(a => assert(a.text == validFrontText))
            }
        }
    }

    "back" when {
        "text is empty" should {
            "give error" in {
                val result = Back("   ", exampleOfUseText)
                assert(result == Left(SystemMessages.CannotBeEmpty("back")))
            }
        }

        "text has content" when {
            "exampleOfUse is empty" should {
                "give valid back" in {
                    val result = Back(validBackText, "")
                    validateEither(result)(a => {
                        assert(a.text == validBackText)
                        assert(a.exampleOfUse == "")
                    })
                }
            }

            "exampleOfUse has content" should {
                "give valid back" in {
                    val result = Back(validBackText, exampleOfUseText)
                    validateEither(result)(a => {
                        assert(a.text == validBackText)
                        assert(a.exampleOfUse == exampleOfUseText)
                    })
                }
            }
        }
    }


    "Card" should {
        "return valid Card" in {
            val card = Card(new Front(validFrontText) {}, new Back(validBackText, exampleOfUseText) {})
            assert(card.front.text == validFrontText)
            assert(card.back.text == validBackText)
            assert(card.back.exampleOfUse == exampleOfUseText)
            assert(card.stats.wins.get == 0)
            assert(card.stats.losses.get == 0)
        }
    }


    "Card.win" should {
        val stats = new CardStatistics(None, Wins(0), Losses(0), WinStreak(0)) {}
        val card = new Card(uuid, validFront, validBack, stats) {}
        val now = ZonedDateTime.now
        val wonCard = card.win(now)

        "increase number of wins and winstreak" in {
            assert(wonCard.stats.wins == Wins(1))
            assert(wonCard.stats.winStreak == WinStreak(1))
        }

        "update lastVisited" in {
            assert(wonCard.stats.lastVisited == Some(now))
        }
    }

    "Card.lose" should {
        val stats = new CardStatistics(None, Wins(0), Losses(0), WinStreak(3)) {}
        val card = new Card(uuid, validFront, validBack, stats) {}
        val now = ZonedDateTime.now
        val lostCard = card.lose(now)

        "increase number of losses" in {
            assert(lostCard.stats.losses == Losses(1))
        }

        "end winStreak" in {
            assert(lostCard.stats.winStreak == WinStreak(0))
        }

        "update lastVisited" in {
            assert(lostCard.stats.lastVisited == Some(now))
        }
    }

    "CardStatistics.rating" when {

        "initial stats" should {
            "be 0" in {
                val stats = new CardStatistics(None, Wins(0), Losses(0), WinStreak(0)) {}
                assert(stats.rating(now) == 0)
            }
        }

        "has been visited" should {
            "be 1" in {
                val stats = new CardStatistics(Some(now), Wins(0), Losses(0), WinStreak(0)) {}
                assert(stats.rating(now) == 1)
            }
        }
    }

    "PracticeDeck" when {
        "no cards in source deck" should {
            "be empty" in {
                assert(PracticeDeck(0, List(), now).isEmpty)
                assert(PracticeDeck(100, List(), now).isEmpty)
            }
        }
        "one card in source deck" when {
            "number of cards is 0" should {
                "be empty" in {
                    assert(PracticeDeck(0, List(untouchedCard), now).isEmpty)
                }
            }
            "number of cards is 1" should {
                "give deck of same card" in {
                    val result = PracticeDeck(1, List(untouchedCard), now)
                    assert(result == List(untouchedCard))
                }
            }
        }
        "multiple cards in source deck" should {
            "give deck of highest rated cards" in {
                val statsWithRating1 = new CardStatistics(Some(now), Wins(0), Losses(0), WinStreak(0)) {}
                val statsWithRating2 = new CardStatistics(Some(now.minusDays(1)), Wins(0), Losses(0), WinStreak(0)) {}
//                val statsWithRating3 = new CardStatistics(Some(now.minusDays(2)), Wins(0), Losses(0), WinStreak(0)) {}
                val rating0 = untouchedCard
                val rating1 = new Card(uuid, validFront, validBack, statsWithRating1) {}
                val rating2 = new Card(uuid, validFront, validBack, statsWithRating2) {}

                val sourceDeck = List(rating0, rating1, rating2)
                val practiceDeckWithOneCard = PracticeDeck(1, sourceDeck, now)
                val practiceDeckWithTwoCards = PracticeDeck(2, sourceDeck, now)
                assert(practiceDeckWithOneCard == List(rating2))
                assert(practiceDeckWithTwoCards == List(rating2, rating1))
            }
        }
    }
}
