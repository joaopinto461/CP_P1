package cp.articlerep.ds;


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
	
	public void lock(K key, int flag);
	public void unlock(K key, int flag);
	
	public void lockList(List<K> elems, int flag);
	public void UnlockList(List<K> elems, int flag);
//	public java.util.List<ReentrantLock> getLocksList(List<K> list);
}
