public class Test {

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
}