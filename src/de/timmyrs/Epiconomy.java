package de.timmyrs;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@SuppressWarnings({"deprecation", "WeakerAccess"})
public class Epiconomy extends JavaPlugin implements CommandExecutor
{
	static Epiconomy instance;
	private static File playerDataDir;
	private final EpiconomyEconomy economy = new EpiconomyEconomy();

	static File getConfigFile(UUID playerUuid)
	{
		if(!playerDataDir.exists() && !playerDataDir.mkdir())
		{
			throw new RuntimeException("Failed to create " + playerDataDir.getPath());
		}
		return new File(playerDataDir, playerUuid.toString().replace("-", "") + ".yml");
	}

	public void onEnable()
	{
		instance = this;
		playerDataDir = new File(getDataFolder(), "playerdata");
		getConfig().addDefault("currency.symbol", "₡");
		getConfig().addDefault("currency.nameSingular", "Coin");
		getConfig().addDefault("currency.namePlural", "Coins");
		getConfig().addDefault("currency.hasCents", true);
		getConfig().options().copyDefaults(true);
		saveConfig();
		reloadConfig();
		getServer().getServicesManager().register(Economy.class, new EpiconomyEconomy(), this, ServicePriority.High);
		getCommand("epiconomy").setExecutor(this);
		getCommand("money").setExecutor(this);
		getCommand("sendmoney").setExecutor(this);
		getCommand("givemoney").setExecutor(this);
		getCommand("takemoney").setExecutor(this);
	}

	@Override
	public boolean onCommand(CommandSender s, Command c, String l, String[] a)
	{
		switch(c.getName())
		{
			case "epiconomy":
				if(a.length > 0 && a[0].equalsIgnoreCase("reload") && s.hasPermission("epiconomy.reload"))
				{
					reloadConfig();
					s.sendMessage("§aReloaded the configuration.");
				}
				else
				{
					s.sendMessage("https://github.com/timmyrs/Epiconomy");
				}
				break;
			case "money":
				if(s instanceof Player)
				{
					if(a.length > 0 && s.hasPermission("epiconomy.money.other"))
					{
						final Player p = getServer().getPlayer(a[0]);
						if(p != null && p.isOnline())
						{
							s.sendMessage(p.getName() + " has " + economy.longFormat(economy.getBalance(p)) + ".");
						}
						else
						{
							s.sendMessage("§c'" + a[0] + "' is not online.");
						}
					}
					else
					{
						s.sendMessage("You have " + economy.longFormat(economy.getBalance((Player) s)) + ".");
					}
				}
				else
				{
					s.sendMessage("§cThis command is only for players.");
				}
				break;
			case "sendmoney":
				if(s instanceof Player)
				{
					if(a.length > 1)
					{
						final Player recipient = getServer().getPlayer(a[0]);
						if(recipient != null && recipient.isOnline())
						{
							try
							{
								final double amount = Double.valueOf(a[1]);
								final Player sender = (Player) s;
								final double senderBalance = economy.getBalance(sender);
								if(amount > 0)
								{
									if(senderBalance >= amount)
									{
										economy.withdrawPlayer(sender, amount);
										economy.depositPlayer(recipient, amount);
										s.sendMessage("§aYou've just sent " + economy.format(amount) + " to " + recipient.getName() + ". You now have " + economy.longFormat(senderBalance - amount) + ".");
										recipient.sendMessage("§a" + sender.getName() + " has just sent you " + economy.format(amount) + ". You now have " + economy.longFormat(economy.getBalance(recipient) + amount) + ".");
									}
									else
									{
										s.sendMessage("§cYou can't send " + economy.format(amount) + " as you only have " + economy.longFormat(senderBalance) + ".");
									}
								}
								else
								{
									s.sendMessage("§cNot a chance.");
								}
							}
							catch(NumberFormatException e)
							{
								s.sendMessage("§cSyntax: /sendmoney <recipient> <amount>");
							}
						}
						else
						{
							s.sendMessage("§a'" + a[0] + "' is not online.");
						}
					}
					else
					{
						s.sendMessage("§cSyntax: /sendmoney <recipient> <amount>");
					}
				}
				else
				{
					s.sendMessage("§cThis command is only for players.");
				}
				break;
			case "givemoney":
				if(a.length > 1)
				{
					final Player recipient = getServer().getPlayer(a[0]);
					if(recipient != null && recipient.isOnline())
					{
						try
						{
							final double amount = Double.valueOf(a[1]);
							economy.depositPlayer(recipient, amount);
							s.sendMessage("§aYou've just given " + economy.longFormat(amount) + " to " + recipient.getName() + "." + (s.hasPermission("epiconomy.money.other") ? " They now have " + economy.format(economy.getBalance(recipient)) + "." : ""));
						}
						catch(NumberFormatException e)
						{
							s.sendMessage("§cSyntax: /givemoney <recipient> <amount>");
						}
					}
					else
					{
						s.sendMessage("§a'" + a[0] + "' is not online.");
					}
				}
				else
				{
					s.sendMessage("§cSyntax: /givemoney <recipient> <amount>");
				}
				break;
			case "takemoney":
				if(a.length > 1)
				{
					final Player recipient = getServer().getPlayer(a[0]);
					if(recipient != null && recipient.isOnline())
					{
						try
						{
							final double amount = Double.valueOf(a[1]);
							economy.withdrawPlayer(recipient, amount);
							s.sendMessage("§aYou've just taken " + economy.longFormat(amount) + " from " + recipient.getName() + "." + (s.hasPermission("epiconomy.money.other") ? " They now have " + economy.format(economy.getBalance(recipient)) + "." : ""));
						}
						catch(NumberFormatException e)
						{
							s.sendMessage("§cSyntax: /takemoney <recipient> <amount>");
						}
					}
					else
					{
						s.sendMessage("§a'" + a[0] + "' is not online.");
					}
				}
				else
				{
					s.sendMessage("§cSyntax: /takemoney <recipient> <amount>");
				}
				break;
		}
		return true;
	}
}

@SuppressWarnings({"deprecation", "WeakerAccess"})
class EpiconomyEconomy implements Economy
{
	@Override
	public boolean isEnabled()
	{
		return true;
	}

	@Override
	public String getName()
	{
		return "Epiconomy";
	}

	@Override
	public boolean hasBankSupport()
	{
		return false;
	}

	public boolean hasCents()
	{
		return Epiconomy.instance.getConfig().getBoolean("currency.hasCents");
	}

	@Override
	public int fractionalDigits()
	{
		return hasCents() ? 2 : 0;
	}

	public double round(double amount)
	{
		return hasCents() ? (double) Math.round(amount * 100D) / 100D : Math.round(amount);
	}

	@Override
	public String format(double amount)
	{
		return round(amount) + Epiconomy.instance.getConfig().getString("currency.symbol");
	}

	public String longFormat(double amount)
	{
		return round(amount) + " " + (amount == 1 ? currencyNameSingular() : currencyNamePlural());
	}

	@Override
	public String currencyNamePlural()
	{
		return Epiconomy.instance.getConfig().getString("currency.namePlural");
	}

	@Override
	public String currencyNameSingular()
	{
		return Epiconomy.instance.getConfig().getString("currency.nameSingular");
	}

	@Override
	public boolean hasAccount(String s)
	{
		return true;
	}

	@Override
	public boolean hasAccount(OfflinePlayer offlinePlayer)
	{
		return true;
	}

	@Override
	public boolean hasAccount(String s, String s1)
	{
		return true;
	}

	@Override
	public boolean hasAccount(OfflinePlayer offlinePlayer, String s)
	{
		return true;
	}

	@Override
	public double getBalance(String s)
	{
		return getBalance(Bukkit.getServer().getOfflinePlayer(s));
	}

	@Override
	public double getBalance(OfflinePlayer p)
	{
		if(p == null)
		{
			return 0;
		}
		final YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(Epiconomy.getConfigFile(p.getUniqueId()));
		return playerConfig.getDouble("a", 0);
	}

	@Override
	public double getBalance(String s, String s1)
	{
		return getBalance(Bukkit.getServer().getOfflinePlayer(s));
	}

	@Override
	public double getBalance(OfflinePlayer p, String s)
	{
		return getBalance(p);
	}

	@Override
	public boolean has(String s, double v)
	{
		return has(Bukkit.getServer().getOfflinePlayer(s), v);
	}

	@Override
	public boolean has(OfflinePlayer p, double v)
	{
		return getBalance(p) >= v;
	}

	@Override
	public boolean has(String s, String s1, double v)
	{
		return has(Bukkit.getServer().getOfflinePlayer(s), v);
	}

	@Override
	public boolean has(OfflinePlayer p, String s, double v)
	{
		return has(p, v);
	}

	@Override
	public EconomyResponse withdrawPlayer(String s, double v)
	{
		return withdrawPlayer(Bukkit.getServer().getOfflinePlayer(s), v);
	}

	@Override
	public EconomyResponse withdrawPlayer(OfflinePlayer p, double v)
	{
		if(v < 0)
		{
			return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Amount has to be positive.");
		}
		final File playerConfigFile = Epiconomy.getConfigFile(p.getUniqueId());
		final YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerConfigFile);
		double a = playerConfig.getDouble("a", 0) - v;
		playerConfig.set("a", a);
		try
		{
			playerConfig.save(playerConfigFile);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		return new EconomyResponse(v, a, EconomyResponse.ResponseType.SUCCESS, null);
	}

	@Override
	public EconomyResponse withdrawPlayer(String s, String s1, double v)
	{
		return withdrawPlayer(Bukkit.getServer().getOfflinePlayer(s), v);
	}

	@Override
	public EconomyResponse withdrawPlayer(OfflinePlayer p, String s, double v)
	{
		return withdrawPlayer(p, v);
	}

	@Override
	public EconomyResponse depositPlayer(String s, double v)
	{
		return depositPlayer(Bukkit.getServer().getOfflinePlayer(s), v);
	}

	@Override
	public EconomyResponse depositPlayer(OfflinePlayer p, double v)
	{
		if(v < 0)
		{
			return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Amount has to be positive.");
		}
		final File playerConfigFile = Epiconomy.getConfigFile(p.getUniqueId());
		final YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerConfigFile);
		double a = playerConfig.getDouble("a", 0) + v;
		playerConfig.set("a", a);
		try
		{
			playerConfig.save(playerConfigFile);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		return new EconomyResponse(v, a, EconomyResponse.ResponseType.SUCCESS, null);
	}

	@Override
	public EconomyResponse depositPlayer(String s, String s1, double v)
	{
		return depositPlayer(Bukkit.getServer().getOfflinePlayer(s), v);
	}

	@Override
	public EconomyResponse depositPlayer(OfflinePlayer p, String s, double v)
	{
		return depositPlayer(p, v);
	}

	@Override
	public EconomyResponse createBank(String s, String s1)
	{
		return null;
	}

	@Override
	public EconomyResponse createBank(String s, OfflinePlayer offlinePlayer)
	{
		return null;
	}

	@Override
	public EconomyResponse deleteBank(String s)
	{
		return null;
	}

	@Override
	public EconomyResponse bankBalance(String s)
	{
		return null;
	}

	@Override
	public EconomyResponse bankHas(String s, double v)
	{
		return null;
	}

	@Override
	public EconomyResponse bankWithdraw(String s, double v)
	{
		return null;
	}

	@Override
	public EconomyResponse bankDeposit(String s, double v)
	{
		return null;
	}

	@Override
	public EconomyResponse isBankOwner(String s, String s1)
	{
		return null;
	}

	@Override
	public EconomyResponse isBankOwner(String s, OfflinePlayer offlinePlayer)
	{
		return null;
	}

	@Override
	public EconomyResponse isBankMember(String s, String s1)
	{
		return null;
	}

	@Override
	public EconomyResponse isBankMember(String s, OfflinePlayer offlinePlayer)
	{
		return null;
	}

	@Override
	public List<String> getBanks()
	{
		return null;
	}

	@Override
	public boolean createPlayerAccount(String s)
	{
		return true;
	}

	@Override
	public boolean createPlayerAccount(OfflinePlayer offlinePlayer)
	{
		return true;
	}

	@Override
	public boolean createPlayerAccount(String s, String s1)
	{
		return true;
	}

	@Override
	public boolean createPlayerAccount(OfflinePlayer offlinePlayer, String s)
	{
		return true;
	}
}
