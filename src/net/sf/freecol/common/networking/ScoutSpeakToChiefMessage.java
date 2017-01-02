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

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when speaking to a chief.
 */
public class ScoutSpeakToChiefMessage extends AttributeMessage {

    public static final String TAG = "scoutSpeakToChief";
    private static final String RESULT_TAG = "result";
    private static final String SETTLEMENT_TAG = "settlement";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code ScoutSpeakToChiefMessage} with the
     * supplied unit, settlement and result.
     *
     * Result is null in a request.
     *
     * @param unit The {@code Unit} that is learning.
     * @param is The {@code IndianSettlement} to talk to.
     * @param result The result of speaking.
     */
    public ScoutSpeakToChiefMessage(Unit unit, IndianSettlement is,
                                    String result) {
        super(TAG, UNIT_TAG, unit.getId(), SETTLEMENT_TAG, is.getId(),
              RESULT_TAG, result);
    }

    /**
     * Create a new {@code ScoutSpeakToChiefMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public ScoutSpeakToChiefMessage(Game game, Element element) {
        super(TAG, UNIT_TAG, getStringAttribute(element, UNIT_TAG),
              SETTLEMENT_TAG, getStringAttribute(element, SETTLEMENT_TAG),
              RESULT_TAG, getStringAttribute(element, RESULT_TAG));
    }


    // Public interface

    public Unit getUnit(Game game) {
        return game.getFreeColGameObject(getAttribute(UNIT_TAG), Unit.class);
    }

    public IndianSettlement getSettlement(Game game) {
        return game.getFreeColGameObject(getAttribute(SETTLEMENT_TAG),
                                         IndianSettlement.class);
    }

    public String getResult() {
        String result = getAttribute(RESULT_TAG);
        return (result == null) ? "" : result;
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final String unitId = getAttribute(UNIT_TAG);
        final String settlementId = getAttribute(SETTLEMENT_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }
        if (!unit.hasAbility(Ability.SPEAK_WITH_CHIEF)) {
            return serverPlayer.clientError("Unit lacks ability to speak to chief: "
                + unitId);
        }

        IndianSettlement is;
        try {
            is = unit.getAdjacentSettlement(settlementId,
                                            IndianSettlement.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        MoveType type = unit.getMoveType(is.getTile());
        if (type != MoveType.ENTER_INDIAN_SETTLEMENT_WITH_SCOUT) {
            return serverPlayer.clientError("Unable to enter "
                + is.getName() + ": " + type.whyIllegal());
        }

        // Valid request, do the scouting.
        return freeColServer.getInGameController()
            .scoutSpeakToChief(serverPlayer, unit, is);
    }
}
