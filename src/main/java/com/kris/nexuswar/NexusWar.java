package com.kris.nexuswar;

import com.kris.nexuswar.commands.War;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import com.palmergames.bukkit.towny.event.NewNationEvent;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;

import org.bukkit.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class NexusWar extends JavaPlugin implements Listener {
    private static NexusWar instance;

    private static Map<Map<String, String>, Long> warSchedule;
    private static Map<String, String> warFighting;

    Map<String, Location> nexusPos = new HashMap<>();
    Map<String, Integer> nexusHeal = new HashMap<>();

    CustomConfig data;
    Random random = new Random();

    public NexusWar() {
        instance = this;
        warSchedule = new HashMap<>();
        warFighting = new HashMap<>();
    }

    public static NexusWar getInstance() {
        return instance;
    }

    public static Map getSchedule() {
        return warSchedule;
    }

    public static Map getFighting() {
        return warFighting;
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        TownyCommandAddonAPI.addSubCommand(TownyCommandAddonAPI.CommandType.NATION, "war", new War());

        getLogger().info("データをロード中...");

        data = new CustomConfig(this, "data.yml");
        data.saveDefaultConfig();

        // Load War Data
        for(String strNation : data.getConfig().getConfigurationSection("sch").getKeys(false)) {
            Nation defenderNation = TownyAPI.getInstance().getNation(data.getConfig().getString("sch." + strNation + ".defender"));
            if(TownyAPI.getInstance().getNation(strNation) == null || defenderNation == null) {
                continue;
            }

            Long warStartTime = data.getConfig().getLong("sch." + strNation + ".start");

            Map<String, String> fightingQueue = new HashMap<>();
            fightingQueue.put(strNation, defenderNation.getName());

            warSchedule.put(fightingQueue, warStartTime);
        }
        for(String strNation : data.getConfig().getConfigurationSection("war").getKeys(false)) {
            Nation defenderNation = TownyAPI.getInstance().getNation(data.getConfig().getString("war." + strNation));
            if(TownyAPI.getInstance().getNation(strNation) == null || defenderNation == null) {
                continue;
            }

            warFighting.put(strNation, defenderNation.getName());
        }

        // Load Nexus Data
        for(String strNation : data.getConfig().getConfigurationSection("nexus.pos").getKeys(false)) {
            World world = Bukkit.getWorld(data.getConfig().getString("nexus.pos." + strNation + ".world"));
            if(TownyAPI.getInstance().getNation(strNation) == null || world == null) {
                continue;
            }

            Location nexusLocate = new Location(world, data.getConfig().getDouble("nexus.pos." + strNation + ".x"), data.getConfig().getDouble("nexus.pos." + strNation + ".y"), data.getConfig().getDouble("nexus.pos." + strNation + ".z"));

            nexusPos.put(strNation, nexusLocate);
        }
        for(String strNation : data.getConfig().getConfigurationSection("nexus.heal").getKeys(false)) {
            if(TownyAPI.getInstance().getNation(strNation) == null) {
                continue;
            }

            Integer nexusHealth = data.getConfig().getInt("nexus.heal." + strNation);

            nexusHeal.put(strNation, nexusHealth);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis() / 1000L;
                for(Map<String, String> warFightData : warSchedule.keySet()) {
                    Long warStartTime = warSchedule.get(warFightData);
                    if(warStartTime < currentTime) {
                        String senderNation = warFightData.keySet().toArray()[0].toString();
                        String defenderNation = warFightData.get(warFightData.keySet().toArray()[0].toString());
                        System.out.println(senderNation + "," + defenderNation);
                        warFighting.put(senderNation, defenderNation);

                        warSchedule.remove(warFightData);
                    }
                }
            }
        }.runTaskTimerAsynchronously(this, 200L, 100L);

        getLogger().info("ロード完了");
    }

    @Override
    public void onDisable() {
        getLogger().info("データをセーブ中...");

        // Save War Data
        for(Map<String, String> fightScheduleNation : warSchedule.keySet()) {
            Long warStartTime = warSchedule.get(fightScheduleNation);

            data.getConfig().set("sch." + fightScheduleNation.keySet().toArray()[0].toString() + ".start", warStartTime);
            data.getConfig().set("sch." + fightScheduleNation.keySet().toArray()[0].toString() + ".defender", fightScheduleNation.values().toArray()[0].toString());
        }
        for(String senderNation : warFighting.keySet()) {
            String defenderNation = warFighting.get(senderNation);
            data.getConfig().set("war." + senderNation, defenderNation);
        }

        // Save Nexus Data
        for(String nation : nexusPos.keySet()) {
            Location nexusLocate = nexusPos.get(nation);

            data.getConfig().set("nexus.pos." + nation + ".world", nexusLocate.getWorld().getName());
            data.getConfig().set("nexus.pos." + nation + ".x", nexusLocate.getBlockX());
            data.getConfig().set("nexus.pos." + nation + ".y", nexusLocate.getBlockY());
            data.getConfig().set("nexus.pos." + nation + ".z", nexusLocate.getBlockZ());
        }
        for(String nation : nexusHeal.keySet()) {
            Integer nexusHealth = nexusHeal.get(nation);

            data.getConfig().set("nexus.heal." + nation, nexusHealth);
        }

        // isEmpty = Init Values
        if(warFighting.isEmpty()) {
            data.getConfig().set("sch", new HashMap<>());
        }
        if(warFighting.isEmpty()) {
            data.getConfig().set("war", new HashMap<>());
        }
        if(nexusPos.isEmpty()) {
            data.getConfig().set("nexus.pos", new HashMap<>());
        }
        if(nexusHeal.isEmpty()) {
            data.getConfig().set("nexus.heal", new HashMap<>());
        }

        data.saveConfig();

        getLogger().info("セーブ完了");
    }

    @EventHandler
    public void onNewNation(NewNationEvent event) {
        try {
            Location nexusLocate = event.getNation().getSpawn().getBlock().getLocation();
            nexusLocate.setY(nexusLocate.getY() + 5);

            nexusLocate.getWorld().getBlockAt(nexusLocate).setType(Material.END_STONE);

            nexusPos.put(event.getNation().getName(), nexusLocate);
            nexusHeal.put(event.getNation().getName(), 100);
        } catch(TownyException exception) {}
    }

    @EventHandler
    public void onDeleteNation(DeleteNationEvent event) {
        getLogger().warning(event.getNationName() + "のデータを削除中...");

        // Delete War Schedule
        for(Map<String, String> warSchData : warSchedule.keySet()) {
            String senderNation = warSchData.keySet().toArray()[0].toString();
            String defenderNation = warSchData.get(warSchData.keySet().toArray()[0].toString());
            if(senderNation.equalsIgnoreCase(event.getNationName()) || defenderNation.equalsIgnoreCase(event.getNationName())) {
                warSchedule.remove(warSchData);
                break;
            }
        }
        for(String senderNation : warFighting.keySet()) {
            String defenderNation = warFighting.get(senderNation);
            if(senderNation.equalsIgnoreCase(event.getNationName()) || defenderNation.equalsIgnoreCase(event.getNationName())) {
                warFighting.remove(senderNation);
                break;
            }
        }

        // Delete Nexus Data
        nexusPos.remove(event.getNationName());
        nexusHeal.remove(event.getNationName());

        getLogger().warning("データを削除しました");
    }

    @EventHandler
    public void onDestroy(TownyDestroyEvent event) {
        if(event.isInWilderness()) {
            return;
        }

        if(nexusPos.values().contains(event.getLocation())) {
            Map<Location, String> revNexusPos = new HashMap<>();
            for(String nation : nexusPos.keySet()) {
                Location nexusLocate = nexusPos.get(nation);
                revNexusPos.put(nexusLocate, nation);
            }

            String strNation = revNexusPos.get(event.getLocation());
            Nation nation = TownyAPI.getInstance().getNation(strNation);

            if(warFighting.keySet().contains(strNation) || warFighting.values().contains(strNation)) {
                Resident playerResident = TownyAPI.getInstance().getResident(event.getPlayer());
                if(nation.hasResident(playerResident)) {
                    event.getPlayer().sendMessage(ChatColor.RED + "自分のNexusを破壊することは出来ません" + ChatColor.RESET);
                    event.setCancelled(true);
                } else {
                    nexusHeal.replace(strNation, nexusHeal.get(strNation) - 1);
                    event.getPlayer().playSound(event.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.5f, (random.nextInt(5) / 10f));
                    event.getPlayer().sendMessage(ChatColor.GREEN + "Nexusを削りました" + ChatColor.RESET);

                    if(nexusHeal.get(strNation) == Integer.valueOf(0)) {
                        event.getBlock().getDrops().clear();

                        if(warFighting.keySet().contains(strNation)) {
                            // Senderの負け
                            String strWinNation = warFighting.get(strNation);
                            Nation winNation = TownyAPI.getInstance().getNation(strWinNation);

                            warFighting.remove(strNation);

                            nexusPos.remove(strNation);
                            nexusHeal.remove(strNation);
                            nexusHeal.remove(strNation);

                            nexusHeal.replace(strWinNation, 100);
                            winNation.playerBroadCastMessageToNation(event.getPlayer(), ChatColor.GREEN + "この国は勝利しました" + ChatColor.RESET);
                            nation.playerBroadCastMessageToNation(event.getPlayer(), ChatColor.RED + "この国は負けました" + ChatColor.RESET);

                            nation.clear();
                            TownyUniverse.getInstance().getDataSource().removeNation(nation);
                        } else {
                            // Defenderの負け
                            for(String senderNation : warFighting.keySet()) {
                                getLogger().info(String.valueOf(warFighting.get(senderNation).equalsIgnoreCase(strNation)));
                                if(warFighting.get(senderNation).equalsIgnoreCase(strNation)) {
                                    String strWinNation = senderNation;
                                    Nation winNation = TownyAPI.getInstance().getNation(strWinNation);

                                    nexusPos.remove(strNation);
                                    nexusHeal.remove(strNation);
                                    nexusHeal.remove(strNation);

                                    nexusHeal.replace(strWinNation, 100);
                                    winNation.playerBroadCastMessageToNation(event.getPlayer(), ChatColor.GREEN + "この国は勝利しました" + ChatColor.RESET);
                                    nation.playerBroadCastMessageToNation(event.getPlayer(), ChatColor.RED + "この国は負けました" + ChatColor.RESET);

                                    warFighting.remove(senderNation);
                                    break;
                                }
                            }

                            nation.clear();
                            TownyUniverse.getInstance().getDataSource().removeNation(nation);
                        }
                    } else {
                        event.setCancelled(true);
                    }
                }
            } else {
                event.getPlayer().sendMessage(ChatColor.RED + "戦争時以外Nexusを破壊することは出来ません");
                event.setCancelled(true);
            }
        }
    }
}
