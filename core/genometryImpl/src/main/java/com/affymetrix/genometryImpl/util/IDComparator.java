
package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.general.ID;
import com.affymetrix.genometryImpl.general.ID.Order;
import java.util.Comparator;

/**
 *
 * @author hiralv
 */
public class IDComparator implements Comparator<ID> {

	public int compare(ID o1, ID o2) {
		if (o1 instanceof Order && o2 instanceof Order) {
			if (((Order) o1).getOrder() == ((Order) o2).getOrder()) {
				return 0;
			} else if (((Order) o1).getOrder() > ((Order) o2).getOrder()) {
				return 1;
			}

			return -1;
		}

		if (o1 instanceof Order && !(o2 instanceof Order)) {
			return -1;
		}

		if (!(o1 instanceof Order) && o2 instanceof Order) {
			return 1;
		}

		int c = o1.getDisplay().compareTo(o2.getDisplay());
		if (c == 0) {
			c = o1.getName().compareTo(o2.getName());
		}
		return c;
	}
}
