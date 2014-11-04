package cp.articlerep.ds;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Ricardo Dias
 */
public interface Map<K extends Comparable<K>, V> {
	public V put(K key, V value);
	public boolean contains(K key);
	public V remove(K key);
	public V get(K key);
	
	public Iterator<V> values();
	public Iterator<K> keys();
	
	public void lock(K key);
	public void unlock(K key);
	public java.util.List<ReentrantLock> getLocksList(List<K> list);
}
