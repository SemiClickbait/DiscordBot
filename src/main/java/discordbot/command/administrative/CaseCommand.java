package discordbot.command.administrative;

import discordbot.command.CommandVisibility;
import discordbot.core.AbstractCommand;
import discordbot.db.controllers.CGuild;
import discordbot.db.controllers.CModerationCase;
import discordbot.db.model.OModerationCase;
import discordbot.guildsettings.moderation.SettingModlogChannel;
import discordbot.handler.GuildSettings;
import discordbot.handler.Template;
import discordbot.main.DiscordBot;
import discordbot.permission.SimpleRank;
import discordbot.util.Misc;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

public class CaseCommand extends AbstractCommand {
	public CaseCommand() {
		super();
	}

	@Override
	public String getDescription() {
		return "Moderate the mod-cases";
	}

	@Override
	public String getCommand() {
		return "case";
	}

	@Override
	public String[] getUsage() {
		return new String[]{
				"case reason <id> <message> //sets/modifies the reason of a case"
		};
	}

	@Override
	public CommandVisibility getVisibility() {
		return CommandVisibility.PUBLIC;
	}

	@Override
	public String[] getAliases() {
		return new String[]{};
	}

	@Override
	public String execute(DiscordBot bot, String[] args, MessageChannel channel, User author) {
		SimpleRank rank = bot.security.getSimpleRank(author, channel);
		Guild guild = ((TextChannel) channel).getGuild();
		if (!rank.isAtLeast(SimpleRank.GUILD_ADMIN)) {
			return Template.get("command_no_permission");
		}
		if (args.length > 0) {
			switch (args[0].toLowerCase()) {
				case "reason":
					if (args.length < 3) {
						return Template.get("command_invalid_use");
					}

					return editReason(guild, channel, args[1], Misc.joinStrings(args, 2));
			}
		}
		return Template.get("command_invalid_use");
	}

	private String editReason(Guild guild, MessageChannel feedbackChannel, String caseId, String reason) {
		OModerationCase ocase = CModerationCase.findById(Misc.parseInt(caseId, -1));
		if (ocase.id == 0 || ocase.guildId != CGuild.getCachedId(guild.getId())) {
			return Template.get("command_case_not_found", ocase.id);
		}
		ocase.reason = reason;
		CModerationCase.update(ocase);
		TextChannel channel = guild.getTextChannelById(GuildSettings.get(guild).getOrDefault(SettingModlogChannel.class));
		if (channel == null) {
			return Template.get("guild_channel_modlog_not_found");
		}
		channel.getMessageById(ocase.messageId).queue(
				message -> {
					message.editMessage(new MessageBuilder().setEmbed(CModerationCase.buildCase(guild, ocase)).build()).queue();
					feedbackChannel.sendMessage(Template.get("command_case_reason_modified")).queue();
				}
				, throwable ->
						feedbackChannel.sendMessage(Template.get("command_case_message_unknown")).queue()


		);
		return "";
	}
}