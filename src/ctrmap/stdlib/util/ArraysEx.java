
package ctrmap.stdlib.util;

import java.util.ArrayList;
import java.util.List;

public class ArraysEx {

	public static <T> List<T> asList(T... obj) {
		List<T> r = new ArrayList<>();
		for (T t : obj) {
			r.add(t);
		}
		return r;
	}

	public static <T> void addIfNotNullOrContains(List<T> list, T elem) {
		if (elem != null && !list.contains(elem)) {
			list.add(elem);
		}
	}
	
	public static <T> void addAllIfNotNullOrContains(List<T> list, List<T> toAdd) {
		for (T e : toAdd){
			addIfNotNullOrContains(list, e);
		}
	}
	
	public static String toString(List list){
		return toString(list.toArray(new Object[list.size()]));
	}
	
	public static String toString(Object... list){
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.length; i++){
			if (i != 0){
				sb.append(",");
			}
			sb.append(list[i].toString());
		}
		return sb.toString();
	}
}
