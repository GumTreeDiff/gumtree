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
			if (items.length > 0) {
				return items[0];
			} else {
				return null;
			}
		}

		/**
		 * Gets the first inventory item matching with any of the provided ids.
		 *
		 * @param filter the filter to use
		 * @return the first inventory item matching with any of the provided ids;
		 *         otherwise <code>null</code>
		 */
		public static Item getItem(final Filter<Item> filter) {
			Item[] items = getItems(filter);
			if (items.length > 0) {
				return items[0];
			} else {
				return null;
			}
		}
}