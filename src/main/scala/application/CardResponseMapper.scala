package application

import domain.Card

object CardResponseMapper {
    def apply(card: Card): Dto.CardResponse =
        Dto.CardResponse(card.id.toString, card.front.text, card.back.text, card.back.exampleOfUse)
}
