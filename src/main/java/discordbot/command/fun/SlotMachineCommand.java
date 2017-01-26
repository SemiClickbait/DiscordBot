/*
 * Copyright 2017 github.com/kaaz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package discordbot.command.fun;

import discordbot.command.CooldownScope;
import discordbot.command.ICommandCooldown;
import discordbot.core.AbstractCommand;
import discordbot.games.SlotMachine;
import discordbot.games.slotmachine.Slot;
import discordbot.main.Config;
import discordbot.main.DiscordBot;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

import java.util.concurrent.Future;

/**
 * Created on 23-8-2016
 */
public class SlotMachineCommand extends AbstractCommand implements ICommandCooldown {

	private final long SPIN_INTERVAL = 2000L;

	public SlotMachineCommand() {
		super();
	}

	@Override
	public long getCooldownDuration() {
//		return 30L;
		return 1L;
	}

	@Override
	public CooldownScope getScope() {
		return CooldownScope.USER;
	}

	@Override
	public String getDescription() {
		return "Feeling lucky? try the slotmachine! You might just win a hand full of air!";
	}

	@Override
	public String getCommand() {
		return "slot";
	}

	@Override
	public String[] getUsage() {
		return new String[]{
				"slot              //spin the slotmachine",
				"slot [cookies]    //play for real cookies where [cookies] is the amount of cookies you bet",
				"slot info         //info about payout"
		};
	}

	@Override
	public String[] getAliases() {
		return new String[]{};
	}

	@Override
	public String execute(DiscordBot bot, String[] args, MessageChannel channel, User author) {
		if (args.length == 0 || args.length >= 1 && !args[0].equals("info")) {
			final SlotMachine slotMachine = new SlotMachine();
			bot.out.sendAsyncMessage(channel, slotMachine.toString(), message -> {
				final Future<?>[] f = {null};
				f[0] = bot.scheduleRepeat(() -> {
					try {
						if (slotMachine.gameInProgress()) {
							slotMachine.spin();
						}
						String gameResult;
						if (!slotMachine.gameInProgress()) {
							int winMulti = slotMachine.getWinMultiplier();
							if (winMulti < 0) {
								gameResult = "You rolled " + slotMachine.getWinSlotAmount() + " **" + slotMachine.getWinSlot().getName() + "** and won **" + winMulti + "**";
							} else {
								gameResult = "Aw you lose, better luck next time!";
							}
							message.editMessage(slotMachine.toString() + Config.EOL + gameResult).queue();
							f[0].cancel(false);
						} else {
							message.editMessage(slotMachine.toString()).queue();
						}
					} catch (Exception e) {
						bot.getContainer().reportError(e, "slotmachine", author.getId(), "channel", ((TextChannel) channel).getAsMention(), bot);
						f[0].cancel(false);
					}
				}, 1000L, SPIN_INTERVAL);
			});
		} else {
			String ret = "The slotmachine!" + Config.EOL;
			ret += "payout is as follows: " + Config.EOL;
			for (Slot s : Slot.values()) {
				ret += String.format("%1$s %1$s %1$s = %2$s" + Config.EOL, s.getEmote(), s.getTriplePayout());
			}
			ret += "type **slot play** to give it a shot!";
			return ret;
		}
		return "";
	}
}