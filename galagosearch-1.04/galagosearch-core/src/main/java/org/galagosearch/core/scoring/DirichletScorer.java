// BSD License (http://www.galagosearch.org/license)
package org.galagosearch.core.scoring;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.galagosearch.core.index.StructuredIndex;
import org.galagosearch.core.retrieval.structured.CountIterator;
import org.galagosearch.core.retrieval.structured.RequiredStatistics;
import org.galagosearch.core.retrieval.structured.ScoringFunctionIterator;
import org.galagosearch.core.retrieval.structured.StructuredRetrieval;
import org.galagosearch.core.tools.Search;
import org.galagosearch.tupleflow.Parameters;

/**
 * 
 * @author trevor
 */
@RequiredStatistics(statistics = { "collectionLength" })
public class DirichletScorer extends ScoringFunctionIterator {
	double background;
	double mu;
	public static long cl;
	public static long documentCount;
	static int counter =0;
	public static HashMap<Integer, Integer> quality = new HashMap<Integer, Integer>();
	public static HashMap<Integer, String> quality2 = new HashMap<Integer, String>();
	public static HashMap<Integer, Integer> quality1 = new HashMap<Integer, Integer>();
	 public static int jo = 0;
	 public static HashMap<Integer, Double> quality3 = new HashMap<Integer, Double>();
	 public static int ko = 0;
	public DirichletScorer(Parameters parameters, CountIterator iterator)
			throws IOException {
		super(iterator);

		mu = parameters.get("mu", 1500);
		// System.out.println("MU:"+mu);
		if (parameters.containsKey("collectionProbability")) {
			background = parameters.get("collectionProbability", 0.0001);
			// System.out.println("Background"+background);

		} else {
			
			long collectionLength = parameters.get("collectionLength", (long) 0);
			//long documentCount = parameters.get("documentCount",);
			//System.out.println("Document Count"+ documentCount);
			//StructuredIndex index=new StructuredIndex("/home/kunal/Downloads/wiki-small.index",1);
			//documentCount=index.manifest.get("documentCount",(long)0);
			//System.out.println("Document Count"+ documentCount);
			cl = collectionLength;
			long count = 0;
			long count1=0;
			//System.out.println("counter "+counter);
			while (!iterator.isDone()) {
				//count1 += iterator.count();
				
				//System.out.println("document" + iterator.document());
				       if(!quality1.containsValue(iterator.document()))
				       {
				       quality1.put(ko,iterator.document());
				       ko++;
				       }
				       
                     count++;
				 //System.out.println(count1);
				iterator.nextDocument();
			}
			
			quality.put(jo, (int) count);
			//System.out.println("Count "+count);
			jo++;
			if (count > 0) {
				background = (double) count / (double) collectionLength;
			} else {
				background = 0.5 / (double) collectionLength;
			}
			iterator.reset();
		}
		
		counter++;
	}

	public double scoreCount(int count, int length)//,String term,int docCount,int doc) 
	{
		// System.out.println("Inside score count"+count);
		// System.out.println("background value "+background);
		// System.out.println("mu value "+mu);
		double numerator = count + mu * background;
		double denominator = length + mu;
		double s=Math.log(numerator/denominator);
		return s;
		/*if(quality2.containsKey(doc))
		{
			if(quality2.containsValue(quality2.get(doc)))
				{
				return 0;
			}
			else
			{   quality2.put(doc,term);
				//System.out.println("Document in Dirichlet "+doc);
				double numerator = (1+(Math.log10(count)/Math.log10(2)))*(Math.log10(cl/docCount)/Math.log10(2));
				// System.out.println("numerator "+numerator);
		        //  System.out.println("term is consider here"+term);
				//double denominator = length + mu;
		          int l=Search.v.get(term);
				double denominator = (1+(Math.log10(l)/Math.log10(2)))*(Math.log10(cl/docCount)/Math.log10(2));;
				// System.out.println("Denominator "+denominator);
				double s = numerator*denominator;
				// System.out.println("result "+s);
				return s;		
			}
		}
		else
		{
			//System.out.println("Document in Dirichlet "+doc);
			double numerator = (1+(Math.log10(count)/Math.log10(2)))*(Math.log10(cl/docCount)/Math.log10(2));
			// System.out.println("numerator "+numerator);
	          System.out.println("term is consider here"+term);
			//double denominator = length + mu;
			
	          int l=Search.v.get(term);
			double denominator = (1+(Math.log10(l)/Math.log10(2)))*(Math.log10(cl/docCount)/Math.log10(2));;
			// System.out.println("Denominator "+denominator);
			double s = numerator*denominator;
			// System.out.println("result "+s);
			return s;	
		}*/
		
		
	}
	public double scoreCount1(int count, int length,String term,int docCount,int doc) 
	{    
		// System.out.println("Inside score count"+count);
		// System.out.println("background value "+background);
		// System.out.println("mu value "+mu);
		//double numerator = count + mu * background;
		//double denominator = length + mu;
		//double s=Math.log(numerator/denominator);
		//return s;
		//System.out.println("Document processing "+ docCount);
		if(quality2.containsKey(doc))
		{
			if(quality2.get(doc).equals(term))
				{
				return 0;
			}
			else
			{   quality2.put(doc,term);
				//System.out.println("Document in Dirichlet "+doc);
				double numerator = (1+(Math.log10(count)/Math.log10(2)))*(Math.log10(cl/docCount)/Math.log10(2));
				// System.out.println("numerator "+numerator);
		        //  System.out.println("term is consider here"+term);
				//double denominator = length + mu;
				 int l=1;
		          if(Search.v.get(term)==null)
		          {
		          
		        	  for(Map.Entry<String, Integer> g:Search.v.entrySet())
		        	  {
		        		  if(g.getKey().startsWith(term))
		        		  {
		        			  l=Search.v.get(g.getKey());
		        		  }
		        	  }
		          
		          }
		          else
		          {
		        	l=Search.v.get(term);  
		          }
				double denominator = (1+(Math.log10(l)/Math.log10(2)))*(Math.log10(cl/docCount)/Math.log10(2));
				// System.out.println("Denominator "+denominator);
				System.out.println(" processing in if part");
				double s = (numerator*denominator)/length;
				if(quality3.containsKey(doc))
				{
					double r=quality3.get(doc);
					s=s+r;
				}
				else
				{
					quality3.put(doc,s);
				}
				// System.out.println("result "+s);
				return s;		
				
			}
		}
		else
		{    
			//System.out.println("Document in Dirichlet "+doc);
			double numerator = (1+(Math.log10(count)/Math.log10(2)))*(Math.log10(cl/docCount)/Math.log10(2));
			// System.out.println("numerator "+numerator);
	         // System.out.println("term is consider here"+term);
			//double denominator = length + mu;
			  int l=1;
	          if(Search.v.get(term)==null)
	          {
	          
	        	  for(Map.Entry<String, Integer> g:Search.v.entrySet())
	        	  {
	        		  if(g.getKey().startsWith(term))
	        		  {
	        			  l=Search.v.get(g.getKey());
	        		  }
	        	  }
	          
	          }
	          else
	          {
	        	l=Search.v.get(term);  
	          }
			double denominator = (1+(Math.log10(l)/Math.log10(2)))*(Math.log10(cl/docCount)/Math.log10(2));
			// System.out.println("Denominator "+denominator);
			//System.out.println(" processing in else part");
			double s = (numerator*denominator)/length;
			if(quality3.containsKey(doc))
			{
				double r=quality3.get(doc);
				s=s+r;
			}
			else
			{
				quality3.put(doc,s);
			}
			// System.out.println("result "+s);
			return s;
			
		}
		
		
	}
}
