/**
 *  Copyright (C) 2002-2017   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.networking;

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when looting cargo.
 */
public class LootCargoMessage extends DOMMessage {

    public static final String TAG = "lootCargo";
    private static final String LOSER_TAG = "loser";
    private static final String WINNER_TAG = "winner";

    /** The object identifier of the unit that is looting. */
    private final String winnerId;

    /** The object identifier of the unit that is looted. */
    private final String loserId;

    /** The goods to be looted. */
    private final List<Goods> goods;


    /**
     * Create a new {@code LootCargoMessage}.
     *
     * @param winner The {@code Unit} that is looting.
     * @param loserId The identifier of the {@code Unit} that is looted.
     * @param goods The {@code AbstractGoods} to loot.
     */
    public LootCargoMessage(Unit winner, String loserId, List<Goods> goods) {
        super(TAG);

        this.winnerId = winner.getId();
        this.loserId = loserId;
        this.goods = (goods == null) ? null : new ArrayList<>(goods);
    }

    /**
     * Create a new {@code LootCargoMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public LootCargoMessage(Game game, Element element) {
        super(TAG);

        this.winnerId = getStringAttribute(element, WINNER_TAG);
        this.loserId = getStringAttribute(element, LOSER_TAG);
        this.goods = getChildren(game, element, Goods.class);
    }


    // Public interface

    /**
     * Public accessor to help the client in game controller.
     *
     * @param game The {@code Game} to look for the unit in.
     * @return The winner unit.
     */
    public Unit getUnit(Game game) {
        return game.getFreeColGameObject(winnerId, Unit.class);
    }

    /**
     * Public accessor to help the client in game controller.
     *
     * @return The defender Object Identifier.
     */
    public String getDefenderId() {
        return loserId;
    }

    /**
     * Public accessor to help the client in game controller.
     *
     * @return The goods to loot.
     */
    public List<Goods> getGoods() {
        return goods;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = freeColServer.getGame();

        Unit winner;
        try {
            winner = getUnit(game);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }
        // Do not check loserId, as it might have sunk.  It is enough
        // that the attacker knows it.  Similarly the server is better
        // placed to check the goods validity.

        // Try to loot.
        return freeColServer.getInGameController()
            .lootCargo(serverPlayer, winner, this.loserId, goods);
    }

    /**
     * Convert this LootCargoMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG,
            WINNER_TAG, this.winnerId,
            LOSER_TAG, this.loserId)
            .add(this.goods).toXMLElement();
    }
}
