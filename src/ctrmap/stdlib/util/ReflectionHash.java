package ctrmap.stdlib.util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to dynamically calculate a unique hash of an object's fields.
 */
public class ReflectionHash {

	/**
	 * Enables printing of debug information for the class.
	 */
	private static final boolean REFLHASH_DEBUG = false;
	
	private int hash = 0;
	private int pendingHash = 0;
	private Object object;
	private boolean changedFlag = false;

	/**
	 * Instantiates the hash calculator for an object and calculates the initial hash.
	 * The constructor should be called after the object's initialization.
	 * @param o The object to calculate this and subsequent hashes for.
	 */
	public ReflectionHash(Object o) {
		object = o;
		reset();
	}

	/**
	 * Internal method used to calculate a full field hash of an object.
	 * @param o The object to calculate the hash for.
	 * @param cache A modifiable list used to store already calculated object references to prevent stack overflows.
	 * @return A hash of the input object. If the object has not been modified in any way, this method is guaranteed to always return the same value.
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException 
	 */
	private static int hashObj(Object o, List<Object> cache) throws IllegalArgumentException, IllegalAccessException {
		if (o == null) {
			return 0;
		}
		Class cls = o.getClass();
		boolean isPrimitiveOrEnum = cls.isPrimitive() || cls.isEnum() || o instanceof Number;
		if (!isPrimitiveOrEnum) {
			if (!cache.contains(o)) {
				cache.add(o);
			} else {
				return 0;
			}
		}
		if (isPrimitiveOrEnum) {
			return Objects.hashCode(o);
		} else if (cls.isArray()) {
			int len = Array.getLength(o);
			int hash = 7;
			for (int i = 0; i < len; i++) {
				hash = 37 * hash + hashObj(Array.get(o, i), cache);
			}
			return hash;
		} else {
			int hash = 7;
			for (Field f : getAllFields(cls)) {
				int mods = f.getModifiers();
				if (Modifier.isStatic(mods)) {
					continue;
				}
				if (f.isAnnotationPresent(ReflectionHashIgnore.class)) {
					continue;
				}
				if (f.getType().isAssignableFrom(ReflectionHash.class)) {
					continue;
				}
				
				if (f.getName().equals("modCount")){
					//HACK
					//Java AbstractList store the number of modifications in modCount. In theory, the list can remain unchanged even after modifications.
					//Disabling transient field serialization is not an option since elementData is transient as well for some stupid reason
					
					continue;
				}
				if (REFLHASH_DEBUG){
					System.out.println("Hashing field " + f);
				}
				
				f.setAccessible(true);
				hash = 37 * hash + hashObj(f.get(o), cache);
				
				if (REFLHASH_DEBUG){
					System.out.println(" -> " + hash);
				}
			}
			return hash;
		}
	}

	private static List<Field> getAllFields(Class cls) {
		List<Field> l = ArraysEx.asList(cls.getDeclaredFields());
		Class superc = cls.getSuperclass();
		if (superc != null) {
			l.addAll(getAllFields(superc));
		}
		return l;
	}
	
	/**
	 * Recalculates the hash and resets the modification flag if it was raised by the recalculation.
	 */
	public void reset(){
		recalculate();
		resetChangedFlag();
	}

	/**
	 * Recalculates the assigned object's hash and raises the modification flag if the object has been tampered with.
	 * @return True if the newly calculated hash doesn't match the previous hash.
	 */
	public boolean recalculate() {
		try {
			pendingHash = hashObj(object, new ArrayList<>());

			if (pendingHash != hash) {
				changedFlag = true;
				return true;
			}
		} catch (IllegalArgumentException | IllegalAccessException ex) {
			Logger.getLogger(ReflectionHash.class.getName()).log(Level.SEVERE, null, ex);
		}
		return false;
	}

	/**
	 * Queries the status of the modification flag.
	 * @return True if the assigned object has been modified, false if otherwise.
	 */
	public boolean hasChanged() {
		return changedFlag;
	}
	
	/**
	 * Forces the modification flag to tripped state and sets the object hash to 0 to make it remain so if the hash were to be recalculated.
	 */
	public void forceSetChangedFlag(){
		changedFlag = true;
		hash = 0; //if the data was to be recalculated, the changed flag will remain set
	}

	/**
	 * Convenience method to query the modification flag with automatic hash recalculation if it was not set at the time that the method is called.
	 * @return 
	 */
	public boolean getChangeFlagRecalcIfNeeded() {
		//An update allowed the changed flag to be possibly reset if the changes were reverted, making this method identical to recalculate()
		
		//if (!changedFlag) {
			return recalculate();
		//}
		//return true;
	}

	/**
	 * Resets the state of the modification flag to untripped.
	 */
	public void resetChangedFlag() {
		changedFlag = false;
		hash = pendingHash;
	}
}
