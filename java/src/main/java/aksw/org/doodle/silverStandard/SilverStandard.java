package aksw.org.doodle.silverStandard;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.impl.TupleQueryResultBuilder;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sparql.SPARQLRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * find a knowledge base, number of owl:sameAs links between the different KB
 * 
 * @author ricardousbeck
 * 
 */
public class SilverStandard {
    private static Logger log = LoggerFactory.getLogger(SilverStandard.class);

    public static void main(String args[]) throws RepositoryException, IOException {
        int end = 0;
        for (String endpoint : getEndpoints()) {
            // get endpoints from _endpoints.txt
            System.out.println(endpoint);
            // ask for all owl:sameAs and save them
            saveSameAs(getSameAs("http://dbpedia.org/sparql/"), endpoint, end);
            // TODO group them by the endpoint
            end++;
        }
    }

    /**
     * saves every triple in the form of ?s ?o leaving out owl:sameAs as predicate due to storage reasons
     * 
     * @param sameAs
     * @param endpoint
     * @param end
     * @throws IOException
     */
    private static void saveSameAs(ArrayList<ArrayList<String>> sameAs, String endpoint, int end) throws IOException {
        System.out.println("\t" + sameAs.size());
        BufferedWriter bw = new BufferedWriter(new FileWriter(new File("resources/endpoint/" + end)));
        bw.write(endpoint);
        bw.newLine();
        for (ArrayList<String> row : sameAs) {
            for (String entry : row) {
                bw.write(entry);
                bw.write("\t");
            }
            bw.newLine();
        }
        bw.close();
    }

    private static ArrayList<String> getEndpoints() {
        ArrayList<String> list = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("resources/_endpoints.txt"));
            while (br.ready()) {
                list.add(br.readLine().split("\t")[0]);
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static ArrayList<ArrayList<String>> getSameAs(String endpoint) throws RepositoryException {
        SPARQLRepository rep = new SPARQLRepository(endpoint);
        rep.initialize();
        RepositoryConnection con = rep.getConnection();
        // TODO build in offset
        ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
        int oldCount = 0;
        int newCount = 0;
        do {
            oldCount = newCount;
            String query = "PREFIX owl:<http://www.w3.org/2002/07/owl#> " +
                    "SELECT ?s ?o " +
                    "WHERE { ?s owl:sameAs ?o} ORDER BY ?s ";// LIMIT " + (40000 + oldCount) + " OFFSET " + oldCount;
            // System.out.println(query);
            HashSet<ArrayList<String>> ask = ask(query, con);
            newCount = ask.size() + oldCount;
            System.out.println("Offset: " + newCount + "-> " + ask.size());
            result.addAll(ask);
            System.gc();
        } while (oldCount < newCount);
        con.close();
        return result;
    }

    public static HashSet<ArrayList<String>> ask(String query, RepositoryConnection con) {
        QueryLanguage queryLanguage = QueryLanguage.SPARQL;
        HashSet<ArrayList<String>> result = null;
        try {
            result = new HashSet<ArrayList<String>>();
            TupleQuery tupleQuery = con.prepareTupleQuery(queryLanguage, query);
            TupleQueryResultBuilder tQRW = new TupleQueryResultBuilder();
            tupleQuery.evaluate(tQRW);
            TupleQueryResult tQR = tQRW.getQueryResult();
            while (tQR.hasNext()) {
                ArrayList<String> tmp = new ArrayList<String>();
                BindingSet st = tQR.next();
                Iterator<Binding> stIterator = st.iterator();
                while (stIterator.hasNext()) {
                    // watch out! the binding has to ensure the order
                    Binding b = stIterator.next();
                    tmp.add(b.getValue().stringValue());
                }
                result.add(tmp);
            }
        } catch (QueryEvaluationException e) {
            log.error(query);
        } catch (TupleQueryResultHandlerException e) {
            log.error(e.getLocalizedMessage());
        } catch (RepositoryException e) {
            log.error(e.getLocalizedMessage());
        } catch (MalformedQueryException e) {
            log.error(e.getLocalizedMessage());
        }
        return result;
    }
}