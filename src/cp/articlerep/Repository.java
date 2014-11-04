package cp.articlerep;

import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import cp.articlerep.ds.HashTable;
import cp.articlerep.ds.Iterator;
import cp.articlerep.ds.LinkedList;
import cp.articlerep.ds.List;
import cp.articlerep.ds.Map;

/**
 * @author Ricardo Dias
 */
public class Repository {

	private Map<String, List<Article>> byAuthor;
	private Map<String, List<Article>> byKeyword;
	private Map<Integer, Article> byArticleId;

	public Repository(int nkeys) {
		this.byAuthor = new HashTable<String, List<Article>>(nkeys*2);
		this.byKeyword = new HashTable<String, List<Article>>(nkeys*2);
		this.byArticleId = new HashTable<Integer, Article>(nkeys*2);
	}

	public boolean insertArticle(Article a) {

		if (byArticleId.contains(a.getId()))
			return false;

		byArticleId.lock(a.getId());
		
		java.util.List<ReentrantLock> locksAuthors = byAuthor.getLocksList(a.getAuthors());
		java.util.List<ReentrantLock> locksKeywords = byKeyword.getLocksList(a.getAuthors());
		
		for(ReentrantLock ra : locksAuthors)
			ra.lock();
		
		Iterator<String> authors = a.getAuthors().iterator();
		while (authors.hasNext()) {
			String name = authors.next();

			List<Article> ll = byAuthor.get(name);
			if (ll == null) {
				ll = new LinkedList<Article>();
				byAuthor.put(name, ll);
			}
			ll.add(a);
		}

		for(ReentrantLock ra : locksAuthors)
			ra.unlock();
		
		for(ReentrantLock rk : locksKeywords)
			rk.lock();
		
		Iterator<String> keywords = a.getKeywords().iterator();
		while (keywords.hasNext()) {
			String keyword = keywords.next();

			List<Article> ll = byKeyword.get(keyword);
			if (ll == null) {
				ll = new LinkedList<Article>();
				byKeyword.put(keyword, ll);
			} 
			ll.add(a);
		}
		
		for(ReentrantLock rk : locksKeywords)
			rk.unlock();

		byArticleId.put(a.getId(), a);

		byArticleId.unlock(a.getId());
		return true;
	}

	public void removeArticle(int id) {

		Article a = byArticleId.get(id);

		if (a == null)
			return;

		byArticleId.lock(id);
		
		byArticleId.remove(id);
		
		java.util.List<ReentrantLock> locksAuthors = byAuthor.getLocksList(a.getAuthors());
		java.util.List<ReentrantLock> locksKeywords = byKeyword.getLocksList(a.getAuthors());
		
		for(ReentrantLock ra : locksKeywords)
			ra.lock();

		Iterator<String> keywords = a.getKeywords().iterator();
		while (keywords.hasNext()) {
			String keyword = keywords.next();

			List<Article> ll = byKeyword.get(keyword);
			if (ll != null) {
				int pos = 0;
				Iterator<Article> it = ll.iterator();
				while (it.hasNext()) {
					Article toRem = it.next();
					if (toRem == a) {
						break;
					}
					pos++;
				}
				ll.remove(pos);
				it = ll.iterator();
				if (!it.hasNext()) { // checks if the list is empty
					byKeyword.remove(keyword);
				}
			}
		}
		
		for(ReentrantLock ra : locksKeywords)
			ra.unlock();
		
		for(ReentrantLock ra : locksAuthors)
			ra.lock();

		Iterator<String> authors = a.getAuthors().iterator();
		while (authors.hasNext()) {
			String name = authors.next();

			List<Article> ll = byAuthor.get(name);
			if (ll != null) {
				int pos = 0;
				Iterator<Article> it = ll.iterator();
				while (it.hasNext()) {
					Article toRem = it.next();
					if (toRem == a) {
						break;
					}
					pos++;
				}
				ll.remove(pos);
				it = ll.iterator(); 
				if (!it.hasNext()) { // checks if the list is empty
					byAuthor.remove(name);
				}
			}
		}
		
		for(ReentrantLock ra : locksAuthors)
			ra.lock();
		
		byArticleId.unlock(id);	
	}

	public List<Article> findArticleByAuthor(List<String> authors) {

		Lock lock = new ReentrantLock();
		lock.lock();
		List<Article> res = new LinkedList<Article>();

		Iterator<String> it = authors.iterator();
		while (it.hasNext()) {
			String name = it.next();
			List<Article> as = byAuthor.get(name);
			if (as != null) {
				Iterator<Article> ait = as.iterator();
				while (ait.hasNext()) {
					Article a = ait.next();
					res.add(a);
				}
			}
		}
		lock.unlock();
		return res;
	}

	public List<Article> findArticleByKeyword(List<String> keywords) {

		Lock lock = new ReentrantLock();
		lock.lock();

		List<Article> res = new LinkedList<Article>();

		Iterator<String> it = keywords.iterator();
		while (it.hasNext()) {
			String keyword = it.next();
			List<Article> as = byKeyword.get(keyword);
			if (as != null) {
				Iterator<Article> ait = as.iterator();
				while (ait.hasNext()) {
					Article a = ait.next();
					res.add(a);
				}
			}
		}
		lock.unlock();
		return res;
	}


	/**
	 * This method is supposed to be executed with no concurrent thread
	 * accessing the repository.
	 * 
	 */
	public boolean validate() {

		HashSet<Integer> articleIds = new HashSet<Integer>();
		int articleCount = 0;

		Iterator<Article> aIt = byArticleId.values();
		while(aIt.hasNext()) {
			Article a = aIt.next();

			articleIds.add(a.getId());
			articleCount++;

			// check the authors consistency
			Iterator<String> authIt = a.getAuthors().iterator();
			while(authIt.hasNext()) {
				String name = authIt.next();
				if (!searchAuthorArticle(a, name)) {
					return false;
				}
			}

			// check the keywords consistency
			Iterator<String> keyIt = a.getKeywords().iterator();
			while(keyIt.hasNext()) {
				String keyword = keyIt.next();
				if (!searchKeywordArticle(a, keyword)) {
					return false;
				}
			}
		}

		return articleCount == articleIds.size();
	}

	private boolean searchAuthorArticle(Article a, String author) {
		List<Article> ll = byAuthor.get(author);
		if (ll != null) {
			Iterator<Article> it = ll.iterator();
			while (it.hasNext()) {
				if (it.next() == a) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean searchKeywordArticle(Article a, String keyword) {
		List<Article> ll = byKeyword.get(keyword);
		if (ll != null) {
			Iterator<Article> it = ll.iterator();
			while (it.hasNext()) {
				if (it.next() == a) {
					return true;
				}
			}
		}
		return false;
	}

}
