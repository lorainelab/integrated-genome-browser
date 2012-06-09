
package com.affymetrix.genometryImpl.operator;

import com.affymetrix.genometryImpl.operator.Operator.Order;
import java.util.Comparator;

/**
 *
 * @author hiralv
 */
public class OperatorComparator implements Comparator<Operator> {

	public int compare(Operator o1, Operator o2) {
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
