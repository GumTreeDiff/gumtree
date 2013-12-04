package org.rsbuddy.tabs;

import com.rsbuddy.script.methods.Game;
import com.rsbuddy.script.methods.Menu;
import com.rsbuddy.script.methods.Mouse;
import com.rsbuddy.script.methods.Widgets;
import com.rsbuddy.script.task.Task;
import com.rsbuddy.script.util.Random;
import com.rsbuddy.script.wrappers.*;
import com.rsbuddy.script.wrappers.Component;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Inventory tab related operations.
 */
public class Inventory {

	public static final int WIDGET = 679;
	public static final int WIDGET_PRICE_CHECK = 204;
	public static final int WIDGET_EQUIPMENT_BONUSES = 670;
	public static final int WIDGET_BANK = 763;
	public static final int WIDGET_EXCHANGE = 644;
	public static final int WIDGET_SHOP = 621;
	public static final int WIDGET_DUNGEONEERING_SHOP = 957;
	public static final int WIDGET_BEAST_OF_BURDEN_STORAGE = 665;

	public static final int[] ALT_WIDGETS = {WIDGET_PRICE_CHECK,
			WIDGET_EQUIPMENT_BONUSES, WIDGET_BANK, WIDGET_EXCHANGE,
			WIDGET_SHOP, WIDGET_DUNGEONEERING_SHOP,
			WIDGET_BEAST_OF_BURDEN_STORAGE};

	/**
	 * Clicks on the selected inventory item.
	 *
	 * @param leftClick <tt>true</tt> to left-click otherwise; <tt>false</tt> to
	 *                  right-click
	 * @return <tt>true</tt> if the inventory item was clicked on; otherwise
	 *         <tt>false</tt>
	 */
	public static boolean clickSelectedItem(boolean leftClick) {
		Item item = getSelectedItem();
		return item != null && item.click(leftClick);
	}

	/**
	 * Left-clicks on the selected inventory item.
	 *
	 * @return <tt>true</tt> if the selected inventory item was clicked on;
	 *         otherwise </tt>false</tt>
	 * @see #clickSelectedItem(boolean)
	 */
	public static boolean clickSelectedItem() {
		return clickSelectedItem(true);
	}

	/**
	 * Checks whether the inventory contains the provided item id.
	 *
	 * @param itemId the item id to look for
	 * @return <tt>true</tt> if the inventory contains the provided item id;
	 *         otherwise <tt>false</tt>
	 * @see #containsOneOf(int...)
	 * @see #containsAll(int...)
	 */
	public static boolean contains(int itemId) {
		return getItem(itemId) != null;
	}

	/**
	 * Checks whether the inventory contains all of the provided item ids.
	 *
	 * @param itemIds the item ids to look for
	 * @return <tt>true</tt> if the inventory contains all of the provided item
	 *         ids; otherwise <tt>false</tt>
	 * @see #containsOneOf(int...)
	 */
	public static boolean containsAll(int... itemIds) {
		for (int itemId : itemIds) {
			if (getItem(itemId) == null)
				return false;
		}
		return true;
	}

	/**
	 * Checks whether the inventory contains one of the provided item ids.
	 *
	 * @param itemIds the item ids to check for
	 * @return <tt>true</tt> if the inventory contains one of the provided
	 *         items; otherwise <tt>false</tt>
	 * @see #containsAll(int...)
	 */
	public static boolean containsOneOf(int... itemIds) {
		return getItems(itemIds).length > 0;
	}

	/**
	 * Drags an item to the specified slot. Slot must be in the range of 0 and
	 * 27.
	 *
	 * @param itemId the item id
	 * @param slot   the slot
	 * @return <tt>true</tt> if dragged; otherwise <tt>false</tt>
	 */
	public static boolean drag(int itemId, int slot) {
		return drag(getItem(itemId), slot);
	}

	/**
	 * Drags an item to the specified inventory slot, which must be in the range
	 * of 0 and 27.
	 *
	 * @param item	the inventory item
	 * @param invSlot the inventory slot
	 * @return <tt>true</tt> if dragged; otherwise <tt>false</tt>
	 */
	public static boolean drag(Item item, int invSlot) {
		if (item != null) {
			if (invSlot >= 0 && invSlot <= 27) {
				Component slot = getComponent().getComponents()[invSlot];
				if (slot != null) {
					Rectangle slotRectangle = slot.getBoundingRect();
					Rectangle itemRectangle = item.getComponent()
							.getContentRect();
					if (slotRectangle.contains(itemRectangle)) {
						return true;
					}
					Mouse.move((int) itemRectangle.getCenterX(),
							(int) itemRectangle.getCenterY(), 5, 5);
					Mouse.drag((int) slotRectangle.getCenterX(),
							(int) slotRectangle.getCenterY());
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Drags an item to the specified slot. Slot must be in the range of 1 and
	 * 28.
	 *
	 * @param itemId the item id
	 * @param slot   the slot
	 * @return <tt>true</tt> if dragged; otherwise <tt>false</tt>
	 */
	@Deprecated
	public static boolean dragItem(int itemId, int slot) {
		return dragItem(getItem(itemId), slot);
	}

	/**
	 * Drags an item to the specified inventory slot, which must be in the range
	 * of 1 and 28.
	 *
	 * @param item	the inventory item
	 * @param invSlot the inventory slot
	 * @return <tt>true</tt> if dragged; otherwise <tt>false</tt>
	 */
	@Deprecated
	public static boolean dragItem(Item item, int invSlot) {
		if (item != null) {
			if (invSlot >= 1 && invSlot <= 28) {
				Component slot = getComponent().getComponents()[invSlot - 1];
				if (slot != null) {
					Rectangle slotRectangle = slot.getBoundingRect();
					Rectangle itemRectangle = item.getComponent()
							.getContentRect();
					if (slotRectangle.contains(itemRectangle)) {
						return true;
					}
					Mouse.move((int) itemRectangle.getCenterX(),
							(int) itemRectangle.getCenterY(), 5, 5);
					Mouse.drag((int) slotRectangle.getCenterX(),
							(int) slotRectangle.getCenterY());
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Drops all inventory items excepting those matching with any of the
	 * provided ids.
	 *
	 * @param leftToRight <tt>true</tt> to span row by row (horizontal precedence);
	 *                    <tt>false</tt> to span column by column (vertical precedence).
	 * @param itemIds	 the item ids to exclude
	 */
	public static void dropAllExcept(boolean leftToRight, int... itemIds) {
		if (getCountExcept(itemIds) != 0) {
			if (!leftToRight) {
				for (int c = 0; c < 4; c++) {
					for (int r = 0; r < 7; r++) {
						boolean found = false;
						for (int i = 0; i < itemIds.length && !found; ++i) {
							found = itemIds[i] == getAllItems()[c + r * 4]
									.getId();
						}
						if (!found) {
							dropItem(c, r);
						}
					}
				}
			} else {
				for (int r = 0; r < 7; r++) {
					for (int c = 0; c < 4; c++) {
						boolean found = false;
						for (int i = 0; i < itemIds.length && !found; ++i) {
							found = itemIds[i] == getAllItems()[c + r * 4]
									.getId();
						}
						if (!found) {
							dropItem(c, r);
						}
					}
				}
			}
			Task.sleep(Random.nextInt(500, 800));
		}
	}

	/**
	 * Drops all inventory items vertically (going down the inventory) excepting
	 * those matching with any of the provided ids.
	 *
	 * @param itemIds the item ids to exclude
	 * @see #dropAllExcept(boolean, int...)
	 */
	public static void dropAllExcept(int... itemIds) {
		dropAllExcept(false, itemIds);
	}

	/**
	 * Drops the inventory item of the specified column and row.
	 *
	 * @param col the column the inventory item is in
	 * @param row the row the inventory item is in
	 */
	public static void dropItem(int col, int row) {
		if (col < 0 || col > 3 || row < 0 || row > 6)
			return;
		if (getAllItems()[col + row * 4].getId() == -1)
			return;
		Point p;
		p = Mouse.getLocation();
		if (p.x < 563 + col * 42 || p.x >= 563 + col * 42 + 32
				|| p.y < 213 + row * 36 || p.y >= 213 + row * 36 + 32) {
			Mouse.move(
					getComponent().getComponents()[row * 4 + col].getCenter(),
					10, 10);
		}
		Mouse.click(false);
		Task.sleep(Random.nextInt(10, 25));
		Menu.click("Drop");
		Task.sleep(Random.nextInt(25, 50));
	}

	/**
	 * Gets all the inventory items. If the tab is not currently open, it will
	 * not open it and will return the last known array of items.
	 *
	 * @return an array instance of <code>Item</code>
	 */
	public static Item[] getCachedItems() {
		Component invIface = Widgets.getComponent(WIDGET, 0);
		if (invIface != null) {
			Component[] components = invIface.getComponents();
			if (components.length > 0) {
				List<Item> items = new LinkedList<Item>();
				for (int i = 0; i < 28; ++i) {
					if (components[i].getItemId() != -1) {
						items.add(new Item(components[i]));
					}
				}
				return items.toArray(new Item[items.size()]);
			}
		}
		return new Item[0];
	}

	/**
	 * Gets the inventory component.
	 *
	 * @return the inventory component
	 */
	public static Component getComponent() {
		for (int widget : ALT_WIDGETS) {
			Component inventory = Widgets.getComponent(widget, 0);
			if (inventory != null && inventory.getAbsLocation().x > 50) {
				return inventory;
			}
		}

		// Tab has to be open for us to get its contents
		openTab();

		return Widgets.getComponent(WIDGET, 0);
	}

	/**
	 * Gets the count of all inventory items ignoring stack sizes.
	 *
	 * @return the count
	 * @see #getCount(boolean)
	 */
	public static int getCount() {
		return getCount(false);
	}

	/**
	 * Gets the count of all inventory items.
	 *
	 * @param includeStacks <tt>false</tt> if stacked items should be counted as single
	 *                      items; otherwise <tt>true</tt>
	 * @return the count
	 */
	public static int getCount(boolean includeStacks) {
		int count = 0;
		Item[] items = getItems();
		for (Item item : items) {
			if (item == null)
				continue;
			int itemId = item.getId();
			if (itemId != -1) {
				count += includeStacks ? item.getStackSize() : 1;
			}
		}
		return count;
	}

	/**
	 * Gets the count of all the inventory items matching with any of the
	 * provided ids ignoring stack sizes.
	 *
	 * @param itemIds the item ids to include
	 * @return the count
	 * @see #getCount(boolean, int...)
	 */
	public static int getCount(int... itemIds) {
		return getCount(false, itemIds);
	}

	/**
	 * Gets the count of all the inventory items matching with any of the
	 * provided ids.
	 *
	 * @param includeStacks <tt>true</tt> to count the stack sizes of each item; otherwise
	 *                      <tt>false</tt>
	 * @param itemIds	   the item ids to include
	 * @return the count
	 */
	public static int getCount(boolean includeStacks, int... itemIds) {
		int count = 0;
		Item[] items = getItems(itemIds);
		for (Item item : items) {
			if (item == null)
				continue;
			int itemId = item.getId();
			if (itemId != -1) {
				count += includeStacks ? item.getStackSize() : 1;
			}
		}
		return count;
	}

	/**
	 * Gets the count of all the inventory items excluding the provided ids
	 * ignoring stack sizes.
	 *
	 * @param ids the ids to exclude
	 * @return the count
	 * @see #getCountExcept(boolean, int...)
	 */
	public static int getCountExcept(int... ids) {
		return getCountExcept(false, ids);
	}

	/**
	 * Gets the count of all the inventory items excluding any of the provided
	 * ids.
	 *
	 * @param includeStacks <tt>true</tt> to count the stack sizes of each item; otherwise
	 *                      <tt>false</tt>
	 * @param ids		   the ids to exclude
	 * @return the count
	 */
	public static int getCountExcept(boolean includeStacks, int... ids) {
		int count = 0;
		Item[] items = getItems();
		outer:
		for (Item item : items) {
			if (item == null)
				continue;
			int itemId = item.getId();
			for (int id : ids) {
				if (itemId == id)
					continue outer;
			}
			count += includeStacks ? item.getStackSize() : 1;
		}
		return count;
	}

	/**
	 * Gets the first inventory item matching with any of the provided ids.
	 *
	 * @param ids the ids to look for
	 * @return the first inventory item matching with any of the provided ids;
	 *         otherwise <code>null</code>
	 */
	public static Item getItem(int... ids) {
		Item[] items = getItems(ids);
		for (Item item : items) {
			if (item != null) {
				return item;
			}
		}
		return null;
	}

	/**
	 * Gets the inventory item at the specified index.
	 *
	 * @param index the index of the inventory item
	 * @return the <code>Item</code>; otherwise <code>null</code> if invalid
	 */
	public static Item getItemAt(int index) {
		Component comp = getComponent().getComponent(index);
		return 0 <= index && index < 28 && comp != null && comp.getItemId() != -1 ? new Item(comp) : null;
	}

	/**
	 * Gets all the valid inventory items.
	 *
	 * @return an array instance of <code>Item</code> of the current valid
	 *         inventory items
	 */
	public static Item[] getItems() {
		Component invIface = getComponent();
		if (invIface != null) {
			Component[] comps = invIface.getComponents();
			if (comps.length > 27) {
				List<Item> items = new LinkedList<Item>();
				for (int i = 0; i < 28; ++i) {
					if (comps[i].getItemId() != -1) {
						items.add(new Item(comps[i]));
					}
				}
				return items.toArray(new Item[items.size()]);
			}
		}
		return new Item[0];
	}

	/**
	 * Gets all the inventory items (including empty ones).
	 *
	 * @return an array instance of <code>Item</code> of the current inventory
	 *         items
	 */
	public static Item[] getAllItems() {
		Item[] items = new Item[28];
		Component invIface = getComponent();
		if (invIface != null) {
			Component[] comps = invIface.getComponents();
			if (comps.length > 27) {
				for (int i = 0; i < 28; ++i) {
					items[i] = new Item(comps[i]);
				}
			}
		}
		return items;
	}

	/**
	 * Gets all the inventory items matching with any of the provided ids.
	 *
	 * @param ids the item ids
	 * @return an array instance of <code>Item</code>
	 */
	public static Item[] getItems(int... ids) {
		List<Item> items = new LinkedList<Item>();
		for (Item item : getItems()) {
			if (item == null)
				continue;
			int itemId = item.getId();
			for (int id : ids) {
				if (itemId == id) {
					items.add(item);
					break;
				}
			}
		}
		return items.toArray(new Item[items.size()]);
	}

	/**
	 * Gets the first id of an inventory item with the given name.
	 *
	 * @param name the name of the inventory item to look for
	 * @return the id of the inventory item; otherwise -1
	 */
	public static int getItemId(String name) {
		Item[] items = getItems();
		for (Item item : items) {
			if (item == null) {
				continue;
			}
			String itemName = item.getComponent().getItemName().toLowerCase();
			if (itemName.contains(name.toLowerCase())) {
				return item.getId();
			}
		}
		return -1;
	}

	/**
	 * Gets the selected inventory item.
	 *
	 * @return the selected inventory item; otherwise <code>null</code> if none
	 *         is selected
	 */
	public static Item getSelectedItem() {
		int index = getSelectedItemIndex();
		return index == -1 ? null : getItemAt(index);
	}

	/**
	 * Gets the selected inventory item's index.
	 *
	 * @return the index of the current selected inventory item; otherwise -1 if
	 *         none is selected
	 */
	public static int getSelectedItemIndex() {
		Item[] items = getItems();
		for (Item item : items) {
			if (item == null) {
				continue;
			}
			Component comp = item.getComponent();
			if (comp.getBorderThickness() == 2) {
				return comp.getIndex();
			}
		}
		return -1;
	}

	/**
	 * Returns the index of the first occurrence of an item in the inventory
	 * matching with the provided id.
	 *
	 * @param id the item id
	 * @return the index; otherwise <tt>-1</tt>.
	 */
	public static int indexOf(int id) {
		Item[] items = getItems();
		for (int i = 0; i < items.length; i++) {
			if (id == items[i].getId()) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the index of the first occurrence of an item in the inventory
	 * matching with the provided name. Case-insensitive.
	 *
	 * @param name the name of the item
	 * @return the index; otherwise <tt>-1</tt>.
	 */
	public static int indexOf(String name) {
		if (name != null && !name.isEmpty()) {
			Item[] items = getItems();
			for (int i = 0; i < items.length; i++) {
				if (items[i].getName().equalsIgnoreCase(name))
					return i;
			}
		}
		return -1;
	}

	/**
	 * Checks whether the inventory is full.
	 *
	 * @return <tt>true</tt> if the inventory contains 28 items; otherwise
	 *         <tt>false</tt>
	 */
	public static boolean isFull() {
		return getCount() == 28;
	}

	/**
	 * Checks whether an inventory item is selected.
	 *
	 * @return <tt>true</tt> if an inventory item is selected; otherwise
	 *         <tt>false</tt>
	 */
	public static boolean isItemSelected() {
		return getSelectedItemIndex() != -1;
	}

	/**
	 * Opens the inventory tab if not already opened.
	 */
	public static void openTab() {
		int tabInventory = Game.TAB_INVENTORY;
		if (Game.getCurrentTab() != tabInventory) {
			Game.openTab(tabInventory);
		}
	}

	/**
	 * Uses two inventory items together.
	 *
	 * @param item   the inventory item to use on another inventory item
	 * @param target the other inventory item to be used on
	 * @return <tt>true</tt> if the "Use" action had been used on both inventory
	 *         items; otherwise <tt>false</tt>
	 */
	public static boolean useItem(Item item, Item target) {
		return item != null && target != null && useItem(item, (Object) target);
	}

	/**
	 * Uses an item on a game object.
	 *
	 * @param item   the item to use
	 * @param target the game object to be used on by the item
	 * @return <tt>true</tt> if the "Use" action had been used on both the
	 *         inventory item and the game object; otherwise <tt>false</tt>
	 */
	public static boolean useItem(Item item, GameObject target) {
		if (item != null && target != null) {
			for (int i = 0, r = Random.nextInt(5, 8); i < r; i++) {
				if (!isItemSelected()) {
					if (item.interact("Use")) {
						for (int j = 0; j < 10 && !isItemSelected(); j++) {
							Task.sleep(100, 200);
						}
					} else {
						return false;
					}
				}
				// just make sure in case something bad happened
				if (isItemSelected()) {
					final String itemName = item.getName();
					final ObjectDefinition targetDef = target.getDef();
					final Model targetModel = target.getModel();
					if (targetDef != null && itemName != null && targetModel != null) {
						final String targetName = targetDef.getName();
						Mouse.move(targetModel.getNextPoint());
						final String action = "Use " + itemName.replace("<col=ff9040>", "") + " -> " + targetName.replace("<col=ff9040>", "");
						for (int j = 0, s = Random.nextInt(5, 8); j < s; j++) {
							if (Menu.contains(action) && Menu.click(action)) {
								return true;
							} else {
								Mouse.move(targetModel.getNextPoint());
							}
						}
					}
					// kay, since that failed, let's try just use
					if (target.interact("Use")) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Uses an inventory item on either another inventory item or a game object.
	 *
	 * @param item   the inventory item to use
	 * @param target the inventory item or the game object to be used on by the
	 *               inventory item
	 * @return <tt>true</tt> if the "Use" action had been used on both the
	 *         inventory item and the game object/other inventory item;
	 *         otherwise <tt>false</tt>
	 */
	private static boolean useItem(Item item, Object target) {
		if (isItemSelected()) {
			Item selectedItem = getSelectedItem();
			int selectedItemId = selectedItem.getId();
			if (item.getId() != selectedItemId) {
				if (!selectedItem.interact("Cancel")) {
					return false;
				}
			} else if (target instanceof Item) {
				Item t = (Item) target;
				if (selectedItemId != t.getId()
						&& selectedItemId != item.getId()) {
					if (!selectedItem.interact("Cancel")) {
						return false;
					}
				}
			}
		}
		for (int i = 0, r = Random.nextInt(5, 8); i < r; i++) {
			if (isItemSelected()) {
				boolean success = false;
				for (int j = 0, k = Random.nextInt(5, 8); j < k; j++) {
					try {
						Item t = (Item) target;
						if (t.interact("Use")) {
							success = true;
							break;
						}
						Task.sleep(150, 300);
					} catch (final ClassCastException e) {
						return false;
					}
				}
				return success;
			}
			item.interact("Use");
			Task.sleep(150, 300);
		}
		return false;
	}

}
