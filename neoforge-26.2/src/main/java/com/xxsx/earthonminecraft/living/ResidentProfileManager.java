package com.xxsx.earthonminecraft.living;

import com.xxsx.earthonminecraft.EarthOnMinecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Optional;
import java.util.UUID;

public final class ResidentProfileManager {
    private static final int PROFILE_VERSION = 1;
    private static final String PREFIX = "EarthOnMinecraftResident";
    private static final String VERSION_KEY = PREFIX + "Version";
    private static final String NAME_KEY = PREFIX + "NameKey";
    private static final String ROLE_ID_KEY = PREFIX + "RoleId";
    private static final String ROLE_TITLE_KEY = PREFIX + "RoleTitleKey";
    private static final String IDENTITY_KEY = PREFIX + "IdentityKey";
    private static final String SKILL_KEY = PREFIX + "Skill";
    private static final String PROFESSION_KEY = PREFIX + "Profession";
    private static final String SETTLEMENT_ID_KEY = PREFIX + "SettlementId";
    private static final String SETTLEMENT_PROFILE_KEY = PREFIX + "SettlementProfile";
    private static final String SETTLEMENT_NAME_KEY = PREFIX + "SettlementNameKey";
    private static final String OWNS_CUSTOM_NAME_KEY = PREFIX + "OwnsCustomName";

    private ResidentProfileManager() {
    }

    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Villager villager
                && event.getLevel() instanceof ServerLevel) {
            ensure(villager);
        }
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Villager villager && !villager.level().isClientSide()
                && villager.tickCount % 200 == 0) {
            ResidentSnapshot profile = ensure(villager);
            if (villager.level() instanceof ServerLevel level) {
                ResidentWorkBehavior.tick(level, villager, profile);
            }
        }
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getTarget() instanceof Villager villager)) {
            return;
        }
        ResidentSnapshot profile = ensure(villager);
        if (event.getEntity().isShiftKeyDown()) {
            event.getEntity().sendOverlayMessage(profile.summary());
        }
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Villager villager) || !(villager.level() instanceof ServerLevel level)) {
            return;
        }
        read(villager).ifPresent(profile -> SettlementSavedData.get(level)
                .unregisterResident(profile.settlementId(), villager.getUUID()));
    }

    public static ResidentSnapshot ensure(Villager villager) {
        if (!(villager.level() instanceof ServerLevel level)) {
            return read(villager).orElseGet(() -> transientProfile(villager));
        }

        CompoundTag data = villager.getPersistentData();
        UUID uuid = villager.getUUID();
        long residentSeed = SettlementDataManager.mix(uuid.getMostSignificantBits()
                ^ Long.rotateLeft(uuid.getLeastSignificantBits(), 29));

        SettlementSavedData savedData = SettlementSavedData.get(level);
        String storedSettlementId = data.getStringOr(SETTLEMENT_ID_KEY, "");
        SettlementSavedData.SettlementSnapshot settlement = savedData.byId(storedSettlementId)
                .orElseGet(() -> savedData.getOrCreate(level, villager.blockPosition()));

        String nameKey = data.getStringOr(NAME_KEY, "");
        if (nameKey.isBlank()) {
            nameKey = SettlementDataManager.chooseNameKey(residentSeed);
        }

        String professionId = professionId(villager);
        SettlementDataManager.RoleSelection roleSelection;
        if (villager.isBaby()) {
            roleSelection = new SettlementDataManager.RoleSelection(EarthOnMinecraft.id("apprentice"),
                    new SettlementDataManager.ResidentRole("resident.earth_on_minecraft.role.apprentice",
                            "resident.earth_on_minecraft.identity.child", java.util.List.of(), java.util.List.of(), 1,
                            java.util.List.of()));
        } else {
            roleSelection = SettlementDataManager.chooseRole(professionId, settlement.profileId(),
                    residentSeed ^ settlement.profileId().hashCode());
        }
        String roleId = roleSelection.id().toString();
        String roleTitleKey = roleSelection.role().titleKey();
        String identityKey = roleSelection.role().identityKey();
        int skill = 1 + Math.floorMod((int) SettlementDataManager.mix(residentSeed ^ 0x6A09E667F3BCC909L), 5);

        boolean ownsCustomName = data.getBooleanOr(OWNS_CUSTOM_NAME_KEY, false);
        if (ownsCustomName && villager.getCustomName() != null) {
            String oldNameKey = data.getStringOr(NAME_KEY, "");
            String oldRoleTitleKey = data.getStringOr(ROLE_TITLE_KEY, "");
            if (!oldNameKey.isBlank() && !oldRoleTitleKey.isBlank()
                    && !villager.getCustomName().equals(displayName(oldNameKey, oldRoleTitleKey))) {
                ownsCustomName = false;
            }
        }

        ResidentSnapshot profile = new ResidentSnapshot(nameKey, roleId, roleTitleKey, identityKey, skill,
                professionId, settlement.id(), settlement.profileId(), settlement.nameKey());

        if (!villager.hasCustomName() || ownsCustomName) {
            villager.setCustomName(profile.displayName());
            villager.setCustomNameVisible(false);
            ownsCustomName = true;
        }

        data.putInt(VERSION_KEY, PROFILE_VERSION);
        data.putString(NAME_KEY, nameKey);
        data.putString(ROLE_ID_KEY, roleId);
        data.putString(ROLE_TITLE_KEY, roleTitleKey);
        data.putString(IDENTITY_KEY, identityKey);
        data.putInt(SKILL_KEY, skill);
        data.putString(PROFESSION_KEY, professionId);
        data.putString(SETTLEMENT_ID_KEY, settlement.id());
        data.putString(SETTLEMENT_PROFILE_KEY, settlement.profileId());
        data.putString(SETTLEMENT_NAME_KEY, settlement.nameKey());
        data.putBoolean(OWNS_CUSTOM_NAME_KEY, ownsCustomName);

        savedData.registerResident(level, villager.blockPosition(), uuid, roleId, nameKey, skill);
        return profile;
    }

    public static Optional<ResidentSnapshot> read(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        if (data.getIntOr(VERSION_KEY, 0) <= 0) {
            return Optional.empty();
        }
        return Optional.of(new ResidentSnapshot(
                data.getStringOr(NAME_KEY, "resident.earth_on_minecraft.name.unknown"),
                data.getStringOr(ROLE_ID_KEY, EarthOnMinecraft.id("resident").toString()),
                data.getStringOr(ROLE_TITLE_KEY, "resident.earth_on_minecraft.role.resident"),
                data.getStringOr(IDENTITY_KEY, "resident.earth_on_minecraft.identity.resident"),
                Math.max(1, data.getIntOr(SKILL_KEY, 1)),
                data.getStringOr(PROFESSION_KEY, "minecraft:none"),
                data.getStringOr(SETTLEMENT_ID_KEY, ""),
                data.getStringOr(SETTLEMENT_PROFILE_KEY, EarthOnMinecraft.id("agricultural_village").toString()),
                data.getStringOr(SETTLEMENT_NAME_KEY, "settlement.earth_on_minecraft.name.unknown")
        ));
    }

    private static ResidentSnapshot transientProfile(Villager villager) {
        long seed = SettlementDataManager.mix(villager.getUUID().getLeastSignificantBits());
        String nameKey = SettlementDataManager.chooseNameKey(seed);
        SettlementDataManager.RoleSelection role = SettlementDataManager.chooseRole(professionId(villager), "", seed);
        return new ResidentSnapshot(nameKey, role.id().toString(), role.role().titleKey(), role.role().identityKey(),
                1, professionId(villager), "", "", "settlement.earth_on_minecraft.name.unknown");
    }

    private static String professionId(Villager villager) {
        return villager.getVillagerData().profession().unwrapKey()
                .map(key -> key.identifier().toString())
                .orElse("minecraft:none");
    }

    private static Component displayName(String nameKey, String roleTitleKey) {
        return Component.translatable("resident.earth_on_minecraft.display",
                Component.translatable(nameKey), Component.translatable(roleTitleKey));
    }

    public record ResidentSnapshot(String nameKey, String roleId, String roleTitleKey, String identityKey,
                                   int skill, String professionId, String settlementId,
                                   String settlementProfileId, String settlementNameKey) {
        public Component displayName() {
            return ResidentProfileManager.displayName(nameKey, roleTitleKey);
        }

        public Component summary() {
            return Component.translatable("message.earth_on_minecraft.resident.summary",
                    Component.translatable(nameKey), Component.translatable(roleTitleKey),
                    Component.translatable(settlementNameKey), skill);
        }

        public Identifier roleIdentifier() {
            return Identifier.parse(roleId);
        }
    }
}
