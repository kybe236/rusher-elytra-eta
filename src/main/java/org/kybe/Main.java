package org.kybe;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

/**
 * Elytra Eta
 *
 * @author kybe236
 */
public class Main extends Plugin {
	
	@Override
	public void onLoad() {
		this.getLogger().info("[ELYTRA ETA] LOADED");

		final ETAHud ETAHud = new ETAHud("Elytra ETA");
		RusherHackAPI.getHudManager().registerFeature(ETAHud);
	}
	
	@Override
	public void onUnload() {
		this.getLogger().info("[ELYTRA ETA] UNLOADED");
	}
	
}