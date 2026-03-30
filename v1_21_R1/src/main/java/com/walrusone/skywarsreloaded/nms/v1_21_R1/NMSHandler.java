package com.walrusone.skywarsreloaded.nms.v1_21_R1;

import com.walrusone.skywarsreloaded.game.signs.SWRSign;
import com.walrusone.skywarsreloaded.nms.NMS;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class NMSHandler implements NMS {

    public NMSHandler() {
    }

    @Override
    public int getVersion() {
        return 21;
    }

    @Override
    public SWRSign createSWRSign(String name, Location location) {
        return new SWRSign_21_1(name, location);
    }

    @Override
    public boolean removeFromScoreboardCollection(Scoreboard scoreboard) {
        return false;
    }

    @Override
    public void respawnPlayer(Player player) {
        player.spigot().respawn();
    }

    @Override
    public void sendParticles(World world, String type, float x, float y, float z, float offsetX, float offsetY, float offsetZ, float data, int amount) {
        try {
            Particle particle = Particle.valueOf(type.toUpperCase(Locale.ROOT));
            world.spawnParticle(particle, x, y, z, amount, offsetX, offsetY, offsetZ, data);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Override
    public FireworkEffect getFireworkEffect(Color one, Color two, Color three, Color four, Color five, FireworkEffect.Type type) {
        return FireworkEffect.builder().flicker(false).withColor(one, two, three, four).withFade(five).with(type).trail(true).build();
    }

    @Override
    public void sendTitle(Player player, int fadein, int stay, int fadeout, String title, String subtitle) {
        player.sendTitle(
                ChatColor.translateAlternateColorCodes('&', title),
                ChatColor.translateAlternateColorCodes('&', subtitle),
                fadein, stay, fadeout
        );
    }

    @Override
    public void sendActionBar(Player player, String msg) {
        msg = ChatColor.translateAlternateColorCodes('&', msg);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
    }

    @Override
    public String getItemName(ItemStack item) {
        if (item != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return meta.getDisplayName();
            }
            return item.getType().name();
        }
        return "";
    }

    @Override
    public void playGameSound(Location loc, String paramEnumName, String paramCategory, float paramVolume, float paramPitch, boolean paramIsCustom) {
        if (!paramIsCustom && loc.getWorld() != null) {
            try {
                Sound sound = Sound.valueOf(paramEnumName.toUpperCase(Locale.ROOT));
                loc.getWorld().playSound(loc, sound, paramVolume, paramPitch);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Override
    public ItemStack getMainHandItem(Player player) {
        return player.getInventory().getItemInMainHand();
    }

    @Override
    public ItemStack getOffHandItem(Player player) {
        return player.getInventory().getItemInOffHand();
    }

    @Override
    public ItemStack getItemStack(Material material, List<String> lore, String message) {
        ItemStack addItem = new ItemStack(material, 1);
        ItemMeta addItemMeta = addItem.getItemMeta();
        if (addItemMeta != null) {
            addItemMeta.setDisplayName(message);
            addItemMeta.setLore(lore);
            addItemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            addItem.setItemMeta(addItemMeta);
        }
        return addItem;
    }

    @Override
    public ItemStack getItemStack(ItemStack item, List<String> lore, String message) {
        ItemStack addItem = item.clone();
        ItemMeta addItemMeta = addItem.getItemMeta();
        if (addItemMeta != null) {
            addItemMeta.setDisplayName(message);
            addItemMeta.setLore(lore);
            addItemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            addItem.setItemMeta(addItemMeta);
        }
        return addItem;
    }

    @Override
    public boolean isValueParticle(String string) {
        try {
            Particle.valueOf(string.toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public void updateSkull(Skull skull, UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        skull.setOwningPlayer(player);
    }

    @Override
    public void setMaxHealth(Player player, int health) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(health);
        }
    }

    @Override
    public void spawnDragon(World world, Location loc) {
        EnderDragon dragon = (EnderDragon) world.spawnEntity(loc, EntityType.ENDER_DRAGON);
        dragon.setPhase(EnderDragon.Phase.CHARGE_PLAYER);
        Location locClone = loc.clone();
        locClone.setYaw(ThreadLocalRandom.current().nextFloat() * 360.0F);
        locClone.setPitch(0.0F);
        dragon.teleport(locClone);
    }

    @Override
    public Entity spawnFallingBlock(Location loc, Material mat, boolean damage) {
        if (loc.getWorld() == null) return null;
        FallingBlock block = loc.getWorld().spawnFallingBlock(loc, mat.createBlockData());
        block.setDropItem(false);
        block.setHurtEntities(damage);
        return block;
    }

    @Override
    public void playChestAction(Block block, boolean open) {
        Location location = block.getLocation();
        if (location.getWorld() == null) return;

        if (block.getState() instanceof org.bukkit.block.EnderChest enderChest) {
            if (open) enderChest.open();
            else enderChest.close();
        }
    }

    @Override
    public void setEntityTarget(Entity ent, Player player) {
        if (ent instanceof Creature creature) {
            creature.setTarget(player);
        }
    }

    @Override
    public void updateSkull(SkullMeta meta, Player player) {
        meta.setOwningPlayer(player);
    }

    @Override
    public ChunkGenerator getChunkGenerator() {
        return new ChunkGenerator() {
            @Override
            public void generateSurface(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData chunkData) {
                // Empty world - do nothing
            }

            @Override
            public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
                return new BiomeProvider() {
                    @Override
                    public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
                        return Biome.THE_VOID;
                    }

                    @Override
                    public List<Biome> getBiomes(WorldInfo worldInfo) {
                        return List.of(Biome.THE_VOID);
                    }
                };
            }
        };
    }

    @Override
    public boolean checkMaterial(FallingBlock fb, Material mat) {
        return fb.getBlockData().getMaterial().equals(mat);
    }

    @Override
    public Objective getNewObjective(Scoreboard scoreboard, String criteria, String displayName) {
        return scoreboard.registerNewObjective(displayName, Criteria.DUMMY, displayName);
    }

    @Override
    public void setGameRule(World world, String rule, String value) {
        try {
            @SuppressWarnings("unchecked")
            GameRule<Boolean> boolRule = (GameRule<Boolean>) GameRule.getByName(rule);
            if (boolRule != null) {
                world.setGameRule(boolRule, Boolean.parseBoolean(value));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean headCheck(Block h1) {
        return h1.getType() == Material.PLAYER_HEAD || h1.getType() == Material.PLAYER_WALL_HEAD;
    }

    @Override
    public ItemStack getBlankPlayerHead() {
        return new ItemStack(Material.PLAYER_HEAD, 1);
    }

    @Override
    public ItemStack getMaterial(String item) {
        if (item.equalsIgnoreCase("SKULL_ITEM")) {
            return new ItemStack(Material.SKELETON_SKULL, 1);
        }
        try {
            return new ItemStack(Material.valueOf(item.toUpperCase(Locale.ROOT)), 1);
        } catch (IllegalArgumentException e) {
            return new ItemStack(Material.STONE, 1);
        }
    }

    @Override
    public ItemStack getColorItem(String mat, byte color) {
        Material material;
        if (mat.equalsIgnoreCase("wool")) {
            material = getColoredMaterial("WOOL", color);
        } else if (mat.equalsIgnoreCase("glass")) {
            material = getColoredMaterial("STAINED_GLASS", color);
        } else if (mat.equalsIgnoreCase("banner")) {
            material = getColoredMaterial("BANNER", color);
        } else {
            material = getColoredMaterial("STAINED_GLASS", color);
        }
        return new ItemStack(material, 1);
    }

    private Material getColoredMaterial(String baseName, byte color) {
        String colorName = switch (color) {
            case 0 -> "WHITE";
            case 1 -> "ORANGE";
            case 2 -> "MAGENTA";
            case 3 -> "LIGHT_BLUE";
            case 4 -> "YELLOW";
            case 5 -> "LIME";
            case 6 -> "PINK";
            case 7 -> "GRAY";
            case 8 -> "LIGHT_GRAY";
            case 9 -> "CYAN";
            case 10 -> "PURPLE";
            case 11 -> "BLUE";
            case 12 -> "BROWN";
            case 13 -> "GREEN";
            case 14 -> "RED";
            case 15 -> "BLACK";
            default -> "WHITE";
        };

        String materialName = colorName + "_" + baseName;
        try {
            return Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            return Material.WHITE_WOOL;
        }
    }

    @Override
    public void setBlockWithColor(World world, int x, int y, int z, Material mat, byte cByte) {
        Block block = world.getBlockAt(x, y, z);
        Material coloredMat = getColoredMaterial(mat.name().replace("WHITE_", ""), cByte);
        block.setType(coloredMat);
    }

    @Override
    public void deleteCache() {
        // Not needed for 1.21+
    }

    @Override
    public Block getHitBlock(ProjectileHitEvent event) {
        return event.getHitBlock();
    }

    @Override
    public void sendJSON(Player sender, String json) {
        try {
            sender.spigot().sendMessage(ComponentSerializer.parse(json));
        } catch (Exception e) {
            // Fallback to plain text if JSON parsing fails
            sender.sendMessage(json);
        }
    }

    @Override
    public boolean isHoldingTotem(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
                || player.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING;
    }

    @Override
    public void applyTotemEffect(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1));
    }

    @Override
    public PotionEffectType getPotionEffectTypeByName(String... names) {
        for (String name : names) {
            try {
                PotionEffectType type = Registry.EFFECT.get(NamespacedKey.fromString(name.toLowerCase(Locale.ROOT)));
                if (type != null) {
                    return type;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @Override
    public Enchantment getEnchantmentByName(String... names) {
        for (String name : names) {
            try {
                Enchantment enchantment = Registry.ENCHANTMENT.get(NamespacedKey.fromString(name.toLowerCase(Locale.ROOT)));
                if (enchantment != null) {
                    return enchantment;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
