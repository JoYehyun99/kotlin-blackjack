package blackjack.model

import blackjack.controller.BlackjackController.Companion.INITIAL_DISTRIBUTE_COUNT

sealed class Participant(val name: ParticipantName, state: State) {
    var state = state
        private set

    fun receiveCard(card: Card) {
        state = state.draw(card)
    }

    fun finishRound() {
        state = state.stay()
    }

    fun getCards(): List<Card> = state.hand().cards

    fun getSumOfCards(): Int = state.hand().sumUpCardValues()

    fun getWinningResult(opponent: Participant): WinningResult = state.calculateWinningResult(opponent)
}

class Player(name: ParticipantName, state: State) : Participant(name, state) {
    fun playRound(
        cardDeck: CardDeck,
        isPlayerActionContinued: (name: ParticipantName) -> Boolean,
        updatePlayerStatus: (player: Player) -> Unit,
    ) {
        while (state is Running) {
            if (isPlayerActionContinued(name)) {
                receiveCard(cardDeck.pick())
            } else {
                finishRound()
            }
            updatePlayerStatus(this)
        }
    }
}

class Dealer(name: ParticipantName = ParticipantName(DEALER_NAME), state: State) : Participant(name, state) {
    fun playRound(
        cardDeck: CardDeck,
        updateDealerStatus: (dealer: Dealer) -> Unit,
    ) {
        while (state is Running) {
            if (isUnderHitThreshold()) {
                receiveCard(cardDeck.pick())
            } else {
                finishRound()
            }
        }
        if (countAdditionalDrawnCards() > 0) {
            updateDealerStatus(this)
        }
    }

    fun countAdditionalDrawnCards(): Int = getCards().size - INITIAL_DISTRIBUTE_COUNT

    fun isUnderHitThreshold(threshold: Int = THRESHOLD_HIT): Boolean = state.hand().sumUpCardValues() <= threshold

    companion object {
        const val THRESHOLD_HIT = 16
        const val DEALER_NAME = "딜러"
    }
}
