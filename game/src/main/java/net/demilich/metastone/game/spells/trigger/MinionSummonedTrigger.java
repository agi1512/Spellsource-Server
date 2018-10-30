package net.demilich.metastone.game.spells.trigger;

import net.demilich.metastone.game.entities.Entity;
import net.demilich.metastone.game.entities.minions.Race;
import net.demilich.metastone.game.events.GameEvent;
import net.demilich.metastone.game.events.GameEventType;
import net.demilich.metastone.game.events.SummonEvent;
import net.demilich.metastone.game.spells.desc.trigger.EventTriggerArg;
import net.demilich.metastone.game.spells.desc.trigger.EventTriggerDesc;
import net.demilich.metastone.game.cards.Attribute;

public class MinionSummonedTrigger extends EventTrigger {

	public MinionSummonedTrigger(EventTriggerDesc desc) {
		super(desc);
	}

	@Override
	protected boolean fire(GameEvent event, Entity host) {
		SummonEvent summonEvent = (SummonEvent) event;
		Race race = (Race) getDesc().get(EventTriggerArg.RACE);
		if (race != null && !summonEvent.getMinion().getRace().hasRace(race)) {
			return false;
		}

		Attribute requiredAttribute = (Attribute) getDesc().get(EventTriggerArg.REQUIRED_ATTRIBUTE);
		// Special case DEATHRATTLES
		if (requiredAttribute == Attribute.DEATHRATTLES
				&& !summonEvent.getMinion().getSourceCard().hasAttribute(requiredAttribute)) {
			return false;
		} else if (requiredAttribute != null && !summonEvent.getMinion().hasAttribute(requiredAttribute)) {
			return false;
		}

		return true;
	}

	@Override
	public GameEventType interestedIn() {
		return GameEventType.SUMMON;
	}

}


