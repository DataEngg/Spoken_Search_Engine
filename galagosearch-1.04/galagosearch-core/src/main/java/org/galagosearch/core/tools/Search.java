// BSD License (http://www.galagosearch.org/license)

package org.galagosearch.core.tools;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import org.galagosearch.core.retrieval.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.galagosearch.core.parse.Document;
import org.galagosearch.core.retrieval.query.Node;
import org.galagosearch.core.retrieval.query.SimpleQuery;
import org.galagosearch.core.retrieval.query.StructuredQuery;
import org.galagosearch.core.scoring.DirichletScorer;
import org.galagosearch.core.store.DocumentStore;
import org.galagosearch.core.store.SnippetGenerator;
import org.galagosearch.tupleflow.Parameters;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * 
 * @author trevor
 */
public class Search {
	SnippetGenerator generator;
	DocumentStore store;
	Retrieval retrieval;
	static String que = null;
	static String que1 = null;
	static TreeMap<Double, String> fp = new TreeMap<Double, String>();
	static TreeMap<Double, String> fp1 = new TreeMap<Double, String>();
	public static HashMap<String, Integer> v = new HashMap<String, Integer>();

	public Search(Retrieval retrieval, DocumentStore store) {
		this.store = store;
		this.retrieval = retrieval;
		generator = new SnippetGenerator();
	}

	public void close() throws IOException {
		store.close();
		retrieval.close();
	}

	public static class SearchResult {
		public Node query;
		public Node transformedQuery;
		public List<SearchResultItem> items;
	}

	public static class SearchResultItem {
		public int rank;
		public String identifier;
		public String displayTitle;
		public String url;
		public Map<String, String> metadata;
		public String summary;
	}

	public String getSummary(Document document, Set<String> query)
			throws IOException {
		if (document.metadata.containsKey("description")) {
			String description = document.metadata.get("description");

			if (description.length() > 10) {
				return generator.highlight(description, query);
			}
		}

		return generator.getSnippet(document.text, query);
	}

	public static Node parseQuery(String query, Parameters parameters) {
		String queryType = parameters.get("queryType", "complex");

		if (queryType.equals("simple")) {
			return SimpleQuery.parseTree(query);
		}

		return StructuredQuery.parse(query);
	}

	public Document getDocument(String identifier) throws IOException {
		return store.get(identifier);
	}

	public SearchResult runQuery(String query, int startAt, int count,
			boolean summarize) throws Exception {

		que = NounDetection(query);// Noun Detection

		StringTokenizer st = new StringTokenizer(que, " ");//O(2^n) sub query generation
		int token_count = st.countTokens();
		if (token_count > 1) {
			String[] temp = new String[token_count];
			for (int l = 0; l < token_count; l++)
				temp[l] = st.nextToken();
			// TreeMap<Integer,String>permute_map=new TreeMap<Integer,String>();
			Combination s1 = new Combination();
			s1.combine(temp);
			// permute(temp, 0);

			for (int l = 1; l <= s1.permute_map.size(); l++) {//passing each sub query in query quality predictor
				int kl = 0;
				int yu = 1;
				String que1 = s1.permute_map.get(l);
				// System.out.println("passed query " + que1);
				for (String token : que1.split(" ")) {
					kl++;
					// System.out.println("Strings in querys "+token);
					if (v.containsKey(token)) {
						int y = (v.get(token) + 1);
						v.remove(token);
						v.put(token, y);
					} else {

						v.put(token, yu);
					}
				}
				// System.out.println("passed to sub-query"+que1);

				runSubQuery(que1, startAt, count, summarize, kl);
				v.clear();
				DirichletScorer.quality.clear();
				DirichletScorer.jo = 0;
				DirichletScorer.quality1.clear();
				DirichletScorer.ko = 0;
				DirichletScorer.quality2.clear();
				DirichletScorer.quality3.clear();
				// System.out.println("kunal");
			}
			/*
			 * for(Map.Entry<Double,String> po:fp1.entrySet()) {
			 * System.out.println("key in QS  "+po.getKey());
			 * System.out.println("value in QS "+po.getValue()); }
			 */
			if (SearchWebHandler.value.equals("1")) {//checking which query quality predictor(AVIDF) you have taken
				double r = fp.lastKey();
				// System.out.println("SCS value " + r);
				Search.que1 = fp.get(r);
				System.out.println("String Passes to Search Box  "
						+ Search.que1);

				fp.clear();
			} else {//Quality Scope
				double r = fp1.lastKey();
				// System.out.println("SCS value " + r);
				Search.que1 = fp1.get(r);
				System.out.println("String Passes to Search Box  "
						+ Search.que1);

				fp1.clear();
			}
			/*
			 * int yu1=1; for (String token : Search.que1.split(" ")) {
			 * 
			 * // System.out.println("Strings in querys "+token); if
			 * (v.containsKey(token)) { int y = (v.get(token) + 1);
			 * v.remove(token); v.put(token, y); } else {
			 * 
			 * v.put(token, yu1); } }
			 */
		} else {//if query is of one length
			Search.que1 = que;// Combination.permute_map=null;
			System.out.println("String passes to search box"+Search.que1);
		}
		Node tree = parseQuery(Search.que1, new Parameters());
		Node transformed = retrieval.transformQuery(tree);
		ScoredDocument[] results = retrieval.runQuery(transformed, startAt
				+ count);
		SearchResult result = new SearchResult();
		Set<String> queryTerms = StructuredQuery.findQueryTerms(tree);
		result.query = tree;
		result.transformedQuery = transformed;
		result.items = new ArrayList();

		for (int i = startAt; i < Math.min(startAt + count, results.length); i++) {
			String identifier = retrieval.getDocumentName(results[i].document);
			// System.out.println(identifier);
			// System.out.println(results[i].score);
			Document document = getDocument(identifier);
			SearchResultItem item = new SearchResultItem();
            
			item.rank = i + 1;
			item.identifier = identifier;
			item.displayTitle = identifier;

			if (document.metadata.containsKey("title")) {
				item.displayTitle = document.metadata.get("title");
			}

			if (item.displayTitle != null) {
				item.displayTitle = generator.highlight(item.displayTitle,
						queryTerms);
			}

			if (document.metadata.containsKey("url")) {
				item.url = document.metadata.get("url");
			}

			if (summarize) {
				item.summary = getSummary(document, queryTerms);
			}

			item.metadata = document.metadata;
			result.items.add(item);
		}

		return result;
	}

	public static String NounDetection(String query) {
		String que = "";
		InputStream modelIn = null;
		try {
			/*
			 * MaxentTagger tagger = new MaxentTagger(
			 * "taggers/bidirectional-distsim-wsj-0-18.tagger"); String tagged =
			 * tagger.tagString(query);
			 */
			// System.out.println("Reduced Verbose Query "+tagged);
			modelIn = new FileInputStream(
					"/home/kunal/Downloads/en-pos-perceptron.bin");
			POSModel model = new POSModel(modelIn);
			POSTaggerME tagger = new POSTaggerME(model);
			String tags = (String) tagger.tag(query);
			StringTokenizer s = new StringTokenizer(tags, " ");
			while (s.hasMoreElements()) {
				String k = s.nextToken();
				//System.out.println("Token " + k);
				int o = k.indexOf('/');
				String f = k.substring(o + 1, k.length());

				if (f.equals("NN") || f.equals("NNP") || (f.equals("JJ"))) {
					String j = k.substring(0, o);

					que = que.concat(j);
					que = que.concat(" ");
				}

			}

			System.out.println("Reduce Verbose Query " + que);
		} catch (Exception e) {
			System.out.println(e);
		}
		return que;
	}

	public void runSubQuery(String query, int startAt, int count,
			boolean summarize, int kl) throws Exception {

		if (SearchWebHandler.value.equals("1")) { //checking for AVIDF
			Node tree = parseQuery(query, new Parameters());
			Node transformed = retrieval.transformQuery(tree);
			ScoredDocument[] results = retrieval.runQuery(transformed, startAt
					+ count);
			/*
			 * for(Map.Entry<String,Integer> o:v.entrySet()) {
			 * System.out.println("key in V map  "+o.getKey());
			 * System.out.println("value in V map "+o.getValue()); }
			 * 
			 * System.out.println("Query is present "+query);
			 * 
			 * /*int kl=0; int yu=1; for (String token : Search.que1.split(" "))
			 * {
			 * 
			 * // System.out.println("Strings in querys "+token); if
			 * (v.containsKey(token)) { int y = (v.get(token) + 1);
			 * v.remove(token); v.put(token, y); } else {
			 * 
			 * v.put(token, yu1); } }
			 */
			double scs = 0;
			int op = 0;

			/*
			 * System.out.println("Kl value"+kl); for(Map.Entry<String,Integer>
			 * l1:v.entrySet()) { System.out.println("Key "+l1.getKey());
			 * System.out.println("Value "+l1.getValue()); }
			 * for(Map.Entry<Integer,Integer>
			 * o:DirichletScorer.quality.entrySet()) {
			 * System.out.println("key in dirichlet "+o.getKey());
			 * System.out.println("val
			 * ue in dirichlet "+o.getValue()); }
			 * 
			 * for(Map.Entry<Integer,Integer>
			 * o:DirichletScorer.quality1.entrySet()) {
			 * System.out.println("key in dirichlet  "+o.getKey());
			 * System.out.println("value in dirichlet "+o.getValue()); }
			 */
			double e;
			double kunal = DirichletScorer.documentCount + 0.5;
			for (String token : query.split(" ")) {
				// int count1 = v.get(token);
				// System.out.println("String "+token);
				// System.out.println("count "+count1);
				int p = DirichletScorer.quality.get(op);
				if (p == 0) {
					op++;
					continue;
				}
				// System.out.println("p value"+p);
				// long d = DirichletScorer.cl;
				double d = kunal / (double) p;
				double r = Math.log10(d) / Math.log10(2);
				double lol = Math.log10(DirichletScorer.documentCount + 1)
						/ Math.log10(2);
				// System.out.println("d value"+d);

				// double m = (double) p / (double) d;
				// System.out.println("r value"+r);
				// double n = (double) count1 / (double) kl;
				// System.out.println("lol value "+lol);
				// double e = Math.log10(n / m) / Math.log10(2);
				e = r / lol;
				if (e == Double.NEGATIVE_INFINITY
						|| e == Double.POSITIVE_INFINITY) {
					op++;
					continue;
				}
				// System.out.println("e value "+e);
				scs += e;
				op++;
			}
			scs = scs / kl;
			// System.out.println("S value " + scs);
			fp.put(scs, query);

		} else {
			Node tree = parseQuery(query, new Parameters());
			Node transformed = retrieval.transformQuery(tree);
			ScoredDocument[] results = retrieval.runQuery(transformed, startAt
					+ count);
			// System.out.println("Quality Predictor "+SearchWebHandler.value);
			int p = DirichletScorer.quality1.size();

			// System.out.println("p value"+p);
			long d = DirichletScorer.cl;

			double r = (Math.log10((double) p / (double) d) / Math.log10(2))
					* (-1);

			// System.out.println("d value"+d);

			// double m = (double) p / (double) d;
			// System.out.println("r value"+r);
			// double n = (double) count1 / (double) kl;
			// System.out.println("lol value "+lol);
			// double e = Math.log10(n / m) / Math.log10(2);

			// System.out.println("e value "+e);

			// System.out.println("S value " + scs);
			fp1.put(r, query);
		}

	}

}
