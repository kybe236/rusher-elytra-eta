package org.kybe;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.rusherhack.client.api.feature.hud.TextHudElement;
import org.rusherhack.client.api.utils.ChatUtils;
import org.rusherhack.core.setting.NumberSetting;

import java.awt.*;
import java.time.Duration;

public class ETAHud extends TextHudElement {

	private int glDurability = 0;
	private volatile boolean running = false;
	private Thread updateThread;
	Component prefix = Component.literal("[ELYTRA ETA] ").withStyle(ChatFormatting.RED);

	final NumberSetting<Integer> delay = new NumberSetting<Integer>("Delay ms", "Check delay", 500, 0, 10000)
			.incremental(1)
			.onChange(setting -> {
				if (this.running) {
					// Print to chat that the delay has been changed
					mc.gui.getChat().addMessage(Component.empty().append(prefix).append(Component.literal("Delay changed to " + setting.longValue() + "ms").withStyle(ChatFormatting.WHITE)));
					stopUpdateThread();
					startUpdateThread();
				}
			});

	public ETAHud(String name) {
		super(name);
		this.registerSettings(delay);
	}

	@Override
	public String getText() {
		if (mc.level == null) {
			mc.gui.getChat().addMessage(Component.empty().append(prefix).append(Component.literal("MC.LEVEL NULL").withStyle(ChatFormatting.WHITE)));
			return "ETA: ERROR";
		}

		if (!this.isToggled()) {
			return "ETA: ERROR";
		}

		if (!running) {
			mc.gui.getChat().addMessage(Component.empty().append(prefix).append(Component.literal("STARTED").withStyle(ChatFormatting.WHITE)));
			startUpdateThread();
		}

		// Format the durability information as minutes and seconds
		return glDurability + "s (" + (glDurability / 60) + "m " + (glDurability % 60) + "s)";
	}

	@Override
	public void onDisable() {
		stopUpdateThread();
		mc.gui.getChat().addMessage(Component.empty().append(prefix).append(Component.literal("STOPPED").withStyle(ChatFormatting.WHITE)));
		super.onDisable();
	}

	private void startUpdateThread() {
		running = true;
		updateThread = new Thread(() -> {
			try {
				while (running && this.isToggled()) {
					glDurability = calculateTotalDurability();
					Thread.sleep(delay.getValue());
				}
			} catch (InterruptedException e) {
				this.getLogger().error("ETA calculation thread interrupted", e);
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				this.getLogger().error("Unexpected error in ETA calculation thread", e);
			} finally {
				running = false;
			}
		});
		updateThread.start();
	}

	private void stopUpdateThread() {
		running = false;
		if (updateThread != null && updateThread.isAlive()) {
			updateThread.interrupt();
		}
	}

	private int calculateTotalDurability() {
		int totalDurability = 0;

		// Check all inventory slots including armor slots for Elytra
		for (int i = 0; i < 36; i++) {
			ItemStack item = mc.player.getInventory().getItem(i);
			if (item.getItem() == Items.ELYTRA) {
				totalDurability += calculateElytraDurability(item);
			}
		}

		// Check the chest armor slot specifically for Elytra
		ItemStack chestArmor = mc.player.getInventory().getArmor(2);
		if (chestArmor.getItem() == Items.ELYTRA) {
			totalDurability += calculateElytraDurability(chestArmor);
		}

		return totalDurability;
	}

	private int calculateElytraDurability(ItemStack item) {
		if (item.isEmpty()) {
			return 0;
		}

		// Fetch the Unbreaking enchantment level on the Elytra. Thanks to rocoplays for getting the code via chatgpt.
		// I swear chatgpt hates me :(.
		Registry<Enchantment> enchantmentRegistry = mc.level.registryAccess().registry(Registries.ENCHANTMENT).orElseThrow();
		Holder<Enchantment> unbreakingEnchantment = enchantmentRegistry.getHolderOrThrow(Enchantments.UNBREAKING);

		int unbreakingLevel = item.getEnchantments().getLevel(unbreakingEnchantment);

		return ((item.getMaxDamage() - item.getDamageValue()) * (unbreakingLevel + 1)) - 1;
	}
}
