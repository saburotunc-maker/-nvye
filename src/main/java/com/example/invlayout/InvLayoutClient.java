package com.example.invlayout;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class InvLayoutClient implements ClientModInitializer {
	private static final String CAT = "Inventory Layout";
	private static KeyBinding saveKey;
	private static KeyBinding applyKey;
	private static final Item[] layout = new Item[36];
	private static Path file;

	@Override
	public void onInitializeClient() {
		file = FabricLoader.getInstance().getConfigDir().resolve("invlayout.txt");
		load();

		saveKey = KeyBindingHelper.registerKeyBinding(
			new KeyBinding("Duzeni Kaydet", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, CAT));
		applyKey = KeyBindingHelper.registerKeyBinding(
			new KeyBinding("Duzeni Uygula", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, CAT));

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			if (mc.player == null) return;
			while (saveKey.wasPressed()) save(mc);
			while (applyKey.wasPressed()) apply(mc);
		});
	}

	private static void msg(MinecraftClient mc, String s) {
		mc.player.sendMessage(Text.literal(s), true);
	}

	private static void save(MinecraftClient mc) {
		PlayerEntity p = mc.player;
		List<String> lines = new ArrayList<>();
		for (int i = 0; i < 36; i++) {
			ItemStack st = p.getInventory().getStack(i);
			layout[i] = st.isEmpty() ? null : st.getItem();
			if (layout[i] != null) lines.add(i + "=" + Registries.ITEM.getId(layout[i]));
		}
		try {
			Files.write(file, lines);
			msg(mc, "§aEnvanter duzeni kaydedildi!");
		} catch (IOException e) {
			msg(mc, "§cKaydedilemedi: " + e.getMessage());
		}
	}

	private static void load() {
		try {
			if (!Files.exists(file)) return;
			for (String l : Files.readAllLines(file)) {
				String[] a = l.split("=", 2);
				if (a.length != 2) continue;
				int slot = Integer.parseInt(a[0].trim());
				Identifier id = Identifier.tryParse(a[1].trim());
				if (id != null && slot >= 0 && slot < 36 && Registries.ITEM.containsId(id))
					layout[slot] = Registries.ITEM.get(id);
			}
		} catch (Exception ignored) {}
	}

	private static int sid(int i) {
		return i < 9 ? 36 + i : i;
	}

	private static void swap(MinecraftClient mc, int a, int b) {
		int sync = mc.player.playerScreenHandler.syncId;
		var im = mc.interactionManager;
		im.clickSlot(sync, sid(a), 0, SlotActionType.PICKUP, mc.player);
		im.clickSlot(sync, sid(b), 0, SlotActionType.PICKUP, mc.player);
		im.clickSlot(sync, sid(a), 0, SlotActionType.PICKUP, mc.player);
	}

	private static void apply(MinecraftClient mc) {
		if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;
		PlayerEntity p = mc.player;
		if (!p.playerScreenHandler.getCursorStack().isEmpty()) {
			msg(mc, "§cOnce elindeki esyayi birak!");
			return;
		}
		int moved = 0;
		for (int i = 0; i < 36; i++) {
			Item want = layout[i];
			if (want == null) continue;
			if (p.getInventory().getStack(i).isOf(want)) continue;
			for (int j = 0; j < 36; j++) {
				if (j == i) continue;
				if (!p.getInventory().getStack(j).isOf(want)) continue;
				if (layout[j] == want) continue;
				swap(mc, j, i);
				moved++;
				break;
			}
		}
		msg(mc, moved > 0 ? "§aEnvanter duzenlendi (" + moved + " tasima)" : "§eZaten duzenli.");
	}
}
