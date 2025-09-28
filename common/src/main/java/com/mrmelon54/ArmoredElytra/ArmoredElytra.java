package com.mrmelon54.ArmoredElytra;

import com.mrmelon54.ArmoredElytra.models.ArmoredElytraModelProvider;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.platform.Platform;
import dev.architectury.registry.client.rendering.ColorHandlerRegistry;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import net.fabricmc.fabric.api.client.item.v1.TooltipComponentCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArmoredElytra {
    public static final String MOD_ID = "armored_elytra";
    public static final int DEFAULT_LEATHER_COLOR = 10511680;
    public static final Map<UUID, ChestplateWithElytraItem> armoredElytraMappings = new HashMap<>();

    public static void init() {
        if (Platform.getOptionalMod("advancednetherite").isPresent()) {
            // TODO: add advanced netherite support later
            System.out.println("[Armored Elytra] Detected Advanced Netherite so adding those chestplates");
            //InternalArrays.CHESTPLATES.addAll(List.of(ModItems.NETHERITE_IRON_CHESTPLATE, ModItems.NETHERITE_GOLD_CHESTPLATE, ModItems.NETHERITE_EMERALD_CHESTPLATE, ModItems.NETHERITE_DIAMOND_CHESTPLATE));
        }

        // Listen for the end of every tick
        ClientTickEvent.CLIENT_POST.register(ArmoredElytra::tick);

        // Create a new model provider
        ArmoredElytraModelProvider armElyModelProvider = new ArmoredElytraModelProvider();

        // Set up the model provider and color provider for the elytra item
        ItemPropertiesRegistry.register(Items.ELYTRA, new ResourceLocation("armored_elytra_type"), armElyModelProvider);
        ColorHandlerRegistry.registerItemColors((itemStack, i) -> {
            if (itemStack == null) return -1;
            ChestplateWithElytraItem item = ChestplateWithElytraItem.fromItemStack(itemStack);
            if (item == null) return -1;
            return i > 0 ? -1 : item.getLeatherChestplateColor();
        }, Items.ELYTRA);

        // Set up the model provider for all chestplate items
        for (Item chestplateType : InternalArrays.CHESTPLATES)
            ItemPropertiesRegistry.register(chestplateType, new ResourceLocation("armored_elytra_type"), armElyModelProvider);

        // Clear armored elytra mappings when quitting a level
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> armoredElytraMappings.clear());

        TooltipComponentCallback.EVENT.register(data -> {
            ItemStack stack = data.itemStack();
            ChestplateWithElytraItem item = ChestplateWithElytraItem.fromItemStack(stack);
            if (item == null || !item.isArmoredElytra()) {
                return null;
            }

            ItemStack chestplateItemStack = item.getChestplate();
            if (chestplateItemStack == null) {
                return null;
            }

            Minecraft mc = Minecraft.getInstance();
            List<Component> tooltipLines = chestplateItemStack.getTooltipLines(mc.player, mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL);

            List<ClientTooltipComponent> components = tooltipLines.stream().map(ClientTooltipComponent::create).toList();
            return ClientTooltipComponent.create(chestplateItemStack.getTooltipImage().orElse(null), components, List.of(), DefaultTooltipPositioner.INSTANCE);
        });
    }

    private static void tick(Minecraft minecraft) {
        if (minecraft.level == null) return;

        // rip fps
        // kinda surprised this doesn't kill the fps
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity == null) continue;
            if (entity instanceof LivingEntity livingEntity) updateWearingArmoredElytra(livingEntity);
        }
    }

    private static void updateWearingArmoredElytra(LivingEntity livingEntity) {
        ItemStack chestSlot = livingEntity.getItemBySlot(EquipmentSlot.CHEST);
        ChestplateWithElytraItem item = ChestplateWithElytraItem.fromItemStack(chestSlot);
        if (item != null) armoredElytraMappings.put(livingEntity.getUUID(), item);
        else armoredElytraMappings.remove(livingEntity.getUUID());
    }
}