// BSD License (http://www.galagosearch.org/license)
package org.galagosearch.core.retrieval.structured;

import java.io.IOException;

import org.galagosearch.core.index.PositionIndexReader;
import org.galagosearch.core.index.PositionIndexReader.Iterator;

/**
 *
 * @author trevor
 */
public abstract class ScoringFunctionIterator implements ScoreIterator {
    public boolean done;
    public CountIterator iterator;

    public ScoringFunctionIterator(CountIterator iterator) {
        this.iterator = iterator;
    }

    public abstract double scoreCount(int count, int length);
    public abstract double scoreCount1(int count, int length,String term,int docCount,int doc);
    public double score(int document, int length) {
        int count = 0;
       // String jk="";
        //PositionIndexReader.Iterator it=(Iterator) iterator;
        
        /*try {
			jk=it.getCurrentTerm();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
       
       // System.out.println("Document in Scoring function iterator"+document);
        //System.out.println("position index reader document "+iterator.document());
        if (iterator.document() == document) {
            count = iterator.count();
          
            
        }
        return scoreCount(count, length);//jk,it.documentCount,//it.document());
    }
    public double score1(int document, int length) {
        int count = 0;
        String jk="";
        //System.out.println("Class function "+);
        PositionIndexReader.Iterator it=null;
         if(!iterator.getClass().toString().equals("class org.galagosearch.core.retrieval.structured.NullExtentIterator"))
         {
        it=(Iterator) iterator;
         }
         else
         {
        	 return 0;
         }
        
        try {
			jk=it.getCurrentTerm();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       
       // System.out.println("Document in Scoring function iterator"+document);
        //System.out.println("position index reader document "+iterator.document());
        //if (iterator.document() == document) {
            count = iterator.count();
          
            //System.out.println("Document processing "+iterator.document());
        //}
        return scoreCount1(count, length,jk,it.documentCount,it.document());
    }

    public void moveTo(int document) throws IOException {
        if (!iterator.isDone()) {
            iterator.skipToDocument(document);
            
        }
    }

    public void movePast(int document) throws IOException {
        if (!iterator.isDone() && iterator.document() <= document) {
            iterator.skipToDocument(document + 1);
        }
    }

    public int nextCandidate() {
        if (isDone()) {
            return Integer.MAX_VALUE;
        }
        return iterator.document();
    }

    public boolean isDone() {
        return iterator.isDone();
    }

    public boolean hasMatch(int document) {
        return !isDone() && iterator.document() == document;
    }

    public void reset() throws IOException {
        iterator.reset();
    }
}
