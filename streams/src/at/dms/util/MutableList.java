package at.dms.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Intended to be used in conjunction with ConstList, if one user
 * wants a constant view of a list while another wants to be able to
 * mutate it.
 */
public class MutableList extends ConstList implements List, Cloneable, Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = -6249241660177073762L;

	/** Inserts the specified element at the specified position in
     * this list (optional operation). */
    @Override
	public void add(int index, Object element) {
        list.add(index, element);
    }
        
    /** Appends the specified element to the end of this list
     * (optional operation). */
    @Override
	public boolean add(Object o) {
        return list.add(o);
    }

    /** Appends all of the elements in the specified collection to
     * the end of this list, in the order that they are returned
     * by the specified collection's iterator (optional
     * operation). */
    @Override
	public boolean addAll(Collection c) {
        return list.addAll(c);
    }

    /** Inserts all of the elements in the specified collection
     * into this list at the specified position (optional
     * operation). */
    @Override
	public boolean addAll(int index, Collection c) {
        return list.addAll(index, c);
    }

    /** Removes all of the elements from this list (optional
     * operation). */
    @Override
	public void clear() {
        list.clear();
    }

    /** Removes the element at the specified position in this list
     * (optional operation). */
    @Override
	public Object remove(int index)  {
        return list.remove(index);
    }

    /** Removes the first occurrence in this list of the specified
     * element (optional operation). */
    @Override
	public boolean remove(Object o) {
        return list.remove(o);
    }

    /** Removes from this list all the elements that are contained
     * in the specified collection (optional operation). */
    @Override
	public boolean removeAll(Collection c) {
        return list.removeAll(c);
    }

    /** Retains only the elements in this list that are contained
     * in the specified collection (optional operation). */
    @Override
	public boolean retainAll(Collection c) {
        return list.retainAll(c);
    }

    /** Returns a view of the portion of this list between the
     * specified fromIndex, inclusive, and toIndex, exclusive. */
    @Override
	public List<Object> subList(int fromIndex, int toIndex) {
        return list.subList(fromIndex, toIndex);
    }

    @Override
	public Object clone() {
        MutableList result = new MutableList();
        result.list = (LinkedList<Object>)list.clone();
        return result;
    }
}
