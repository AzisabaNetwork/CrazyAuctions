package me.badbones69.crazyauctions.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 *
 * @author BadBones69
 *
 * This event is fired when a player buys something from the selling auction house.
 *
 */
public class AuctionBuyEvent extends Event {
	
	private Player player;
	private String seller;
	private long price;
	private ItemStack item;
	private static final HandlerList handlers = new HandlerList();
	
	/**
	 *
	 * @param player
	 * @param item
	 * @param price
	 */
	public AuctionBuyEvent(Player player, ItemStack item, long price, String sellerName) {
		this.player = player;
		this.seller = sellerName;
		this.item = item;
		this.price = price;
	}
	
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public ItemStack getItem() {
		return item;
	}
	
	public long getPrice() {
		return price;
	}
	
	public String getSellerName() {
		return seller;
	}
	
}