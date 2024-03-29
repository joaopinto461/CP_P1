package cp.articlerep;

import java.util.HashSet;

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

		byArticleId.lock(a.getId(),1);
		
		if (byArticleId.contains(a.getId())){
			byArticleId.unlock(a.getId(), 1);
			return false;
		}

		byAuthor.lockList(a.getAuthors(), 1);
		byKeyword.lockList(a.getKeywords(), 1);
		
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

		byArticleId.put(a.getId(), a);
		byKeyword.UnlockList(a.getKeywords(), 1);
		byAuthor.UnlockList(a.getAuthors(), 1);
		
		byArticleId.unlock(a.getId(), 1);
		return true;
	}

	public void removeArticle(int id) {

		byArticleId.lock(id, 1);
		Article a = byArticleId.get(id);

		if (a == null){
			byArticleId.unlock(id, 1);
			return;
		}
		byAuthor.lockList(a.getAuthors(), 1);
		byKeyword.lockList(a.getKeywords(), 1);
		

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
		
		byArticleId.remove(id);
		
		byKeyword.UnlockList(a.getKeywords(), 1);
		byAuthor.UnlockList(a.getAuthors(), 1);
		byArticleId.unlock(id, 1);	
	}

	public List<Article> findArticleByAuthor(List<String> authors) {
		
		byAuthor.lockList(authors, 0);
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
		
		byAuthor.UnlockList(authors, 0);

		return res;
	}

	public List<Article> findArticleByKeyword(List<String> keywords) {

		byKeyword.lockList(keywords, 0);
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
		
		byKeyword.UnlockList(keywords, 0);
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
