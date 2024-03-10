package blackjack.model

class BlackjackGame(private val deck: CardDeck, val participants: Participants) {
    fun playRound(
        isPlayerActionContinued: (name: ParticipantName) -> Boolean,
        updateParticipantStatus: (Participant) -> Unit,
    ) {
        playRoundForPlayers(isPlayerActionContinued, updateParticipantStatus)
        playRoundForDealer(updateParticipantStatus)
    }

    private fun playRoundForPlayers(
        isPlayerActionContinued: (name: ParticipantName) -> Boolean,
        updateParticipantStatus: (player: Player) -> Unit,
    ) {
        participants.players.forEach { player ->
            while (player.state is Running) {
                val continuePlaying = isPlayerActionContinued(player.name)

                if (continuePlaying) {
                    player.receiveCard(deck.pick())
                } else {
                    player.finishRound()
                }
                updateParticipantStatus(player)
            }
        }
    }

    private fun playRoundForDealer(updateParticipantStatus: (dealer: Dealer) -> Unit) {
        val dealer = participants.dealer
        while (dealer.state is Running) {
            val continuePlaying = dealer.isUnderHitThreshold()
            if (continuePlaying) {
                dealer.receiveCard(deck.pick())
            } else {
                dealer.finishRound()
            }
            updateParticipantStatus(dealer)
        }
    }

    fun calculateResult(): Map<Participant, WinningState> {
        val (dealer, players) = participants
        val outcomes = getPlayersWinningState(dealer, players)

        val dealerWinCount = outcomes.values.count { it.wins == 0 && it.losses == 1 }
        val dealerLoseCount = outcomes.values.count { it.wins == 1 && it.losses == 0 }
        return outcomes.plus(mapOf(dealer to WinningState(dealerWinCount, dealerLoseCount)))
    }

    private fun getPlayersWinningState(
        dealer: Dealer,
        players: List<Player>,
    ): Map<Participant, WinningState> {
        return players.associate { player ->
            player to
                when (dealer.state) {
                    is Blackjack -> whenDealerIsBlackjack(player.state)
                    is Bust -> whenDealerIsBust(player.state)
                    is Stay, is Hit -> whenDealerIsStay(player, dealer)
                }
        }
    }

    private fun whenDealerIsBlackjack(playerState: State): WinningState {
        return when (playerState) {
            is Blackjack -> WinningState(0, 0)
            else -> WinningState(0, 1)
        }
    }

    private fun whenDealerIsBust(playerState: State): WinningState {
        return when (playerState) {
            is Bust -> WinningState(0, 1)
            else -> WinningState(1, 0)
        }
    }

    private fun whenDealerIsStay(
        player: Player,
        dealer: Dealer,
    ): WinningState {
        val dealerScore = dealer.state.hand().sumUpCardValues()
        val playerScore = player.state.hand().sumUpCardValues()

        return when {
            player.state is Bust || playerScore < dealerScore -> WinningState(0, 1)
            playerScore > dealerScore -> WinningState(1, 0)
            else -> WinningState(0, 0)
        }
    }
}
