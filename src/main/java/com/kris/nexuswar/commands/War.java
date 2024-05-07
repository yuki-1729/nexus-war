package com.kris.nexuswar.commands;

import com.kris.nexuswar.NexusWar;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class War implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;

        if(args.length == 0) {
            sender.sendMessage(ChatColor.RED + "宣戦布告する国家の名前を指定する必要があります" + ChatColor.RESET);
            return false;
        }

        Nation myNation = TownyAPI.getInstance().getNation(player);
        if(myNation == null) {
            sender.sendMessage(ChatColor.RED + "あなたは国家の王である必要があります" + ChatColor.RESET);
            return false;
        }

        Nation targetNation = TownyAPI.getInstance().getNation(args[0]);
        if(targetNation == null) {
            sender.sendMessage(ChatColor.RED + "指定された名前は見つかりませんでした" + ChatColor.RESET);
            return false;
        }

        if(NexusWar.getFighting().keySet().contains(myNation) || NexusWar.getFighting().values().contains(targetNation)) {
            sender.sendMessage(ChatColor.RED + "既にあなたの国(または相手国)は戦争中です" + ChatColor.RESET);
            return false;
        }

        Town inTown = TownyAPI.getInstance().getTown(player.getLocation());
        if(inTown == null) {
            sender.sendMessage(ChatColor.RED + "宣戦布告は町の中でのみ出来ます" + ChatColor.RESET);
            return false;
        } else if(!inTown.getName().equalsIgnoreCase(myNation.getCapital().getName())) {
            sender.sendMessage(ChatColor.RED + "宣戦布告は自分の町の中でのみ出来ます" + ChatColor.RESET);
            return false;
        }

        boolean allowed = false;

        List<Location> blocksLoc = getNearbyBlocksLocation(player);
        for(Location blockLoc : blocksLoc) {
            Town town = TownyAPI.getInstance().getTown(blockLoc);
            if(town == null) {
                continue;
            }

            try {
                Nation townNation = town.getNation();
                if(townNation == null) {
                    continue;
                }

                if(townNation.getName().equalsIgnoreCase(args[0])) {
                    allowed = true;
                    break;
                }
            } catch (NotRegisteredException e) {}
        }

        if(!allowed) {
            sender.sendMessage(ChatColor.RED + "周囲5マス以内に指定の国に所属する町が見つかりませんでした" + ChatColor.RESET);
            return false;
        }

        Map<String, String> fightingQueue = new HashMap<>();
        fightingQueue.put(myNation.getName(), targetNation.getName());

        long currentTime = System.currentTimeMillis() / 1000L;
        long warStartTime = currentTime + 86400L; //

        NexusWar.getSchedule().put(fightingQueue, warStartTime);

        sender.sendMessage(ChatColor.GREEN + "宣戦布告しました(今から24時間の準備期間です)" + ChatColor.RESET);

        myNation.playerBroadCastMessageToNation((Player) sender, targetNation.getName() + "に宣戦布告しました");
        targetNation.playerBroadCastMessageToNation((Player) sender, myNation.getName() + "から宣戦布告されました");

        return true;
    }

    public List<Location> getNearbyBlocksLocation(Player player) {
        Location playerLoc = player.getLocation().getBlock().getLocation();

        List<Location> blocksLoc = new ArrayList<Location>();

        for(int x = playerLoc.getBlockX() - 5; x <= playerLoc.getBlockX() + 5; x++) {
            for(int y = playerLoc.getBlockY() - 5; y <= playerLoc.getBlockY() + 5; y++) {
                for(int z = playerLoc.getBlockZ() - 5; z <= playerLoc.getBlockZ() + 5; z++) {
                    blocksLoc.add(playerLoc.getWorld().getBlockAt(x, y, z).getLocation());
                }
            }
        }

        return blocksLoc;
    }
}
