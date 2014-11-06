// BSD License (http://www.galagosearch.org/license)
package org.galagosearch.core.retrieval.structured;

import java.io.IOException;

import org.galagosearch.core.index.PositionIndexReader;
import org.galagosearch.core.index.PositionIndexReader.Iterator;
import org.galagosearch.tupleflow.Parameters;

/**
 *
 * @author trevor
 */
public abstract class ScoreCombinationIterator implements ScoreIterator {
    ScoreIterator[] iterators;
    boolean done;

    public ScoreCombinationIterator(Parameters parameters, ScoreIterator[] childIterators) {
        this.iterators = childIterators;
    }

    public double score(int document, int length) {
        float total = 0;

        for (ScoreIterator iterator : iterators) {
        	double b=iterator.score(document, length);
        	
        	
        	//System.out.println(b);
            //total += iterator.score(document, length);
        	total += b;
            //System.out.println("total is "+total);
        }
        return total;
    }

    public double score1(int document, int length) {
        float total = 0;

        for (ScoreIterator iterator : iterators) {
        	double b=iterator.score1(document, length);
        	
        	
        	//System.out.println(b);
            //total += iterator.score(document, length);
        	total += b;
            //System.out.println("total is "+total);
        }
        return total;
    }

    public void movePast(int document) throws IOException {
        for (ScoreIterator iterator : iterators) {
            iterator.movePast(document);
        }
    }

    public void moveTo(int document) throws IOException {
        for (ScoreIterator iterator : iterators) {
            iterator.moveTo(document);
        }
    }

    public void reset() throws IOException {
        for (ScoreIterator iterator : iterators) {
            iterator.reset();
        }
    }
}
