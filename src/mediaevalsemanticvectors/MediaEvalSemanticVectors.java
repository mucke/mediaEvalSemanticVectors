/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mediaevalsemanticvectors;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.en.EnglishMinimalStemmer;

import pitt.search.semanticvectors.*;
import static pitt.search.semanticvectors.Search.runSearch;
import static pitt.search.semanticvectors.Search.usageMessage;
import pitt.search.semanticvectors.utils.PsiUtils;
import pitt.search.semanticvectors.utils.VerbatimLogger;
import pitt.search.semanticvectors.viz.PathFinder;

/**
 *
 * @author mihailupu
 */
public class MediaEvalSemanticVectors {

    String topicsFile;
    private static final Logger LOG = Logger.getLogger(MediaEvalSemanticVectors.class.getName());
    private Search semanticVectorsSearch;
    private TreeSet<String> allowedImages = new TreeSet<>();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        MediaEvalSemanticVectors mesv = new MediaEvalSemanticVectors();
        try {
            mesv.run(args);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, e.getLocalizedMessage(), e);
        }
    }

    public void run(String[] args) throws FileNotFoundException, IOException {
        topicsFile = args[0];

        File parentFolder = new File(topicsFile).getParentFile();

        BufferedReader br = new BufferedReader(new FileReader(topicsFile));
        String line = "";
        EnglishMinimalStemmer ems = new EnglishMinimalStemmer();
        while ((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line, "|");
            String topic = st.nextToken();
            String latitude = st.nextToken();
            String longitude = st.nextToken();
            String url = st.nextToken();
            String query = st.nextToken();
            allowedImages = new TreeSet<>();
            String imageListFilename = parentFolder.getAbsolutePath() + File.separator + "xml4solr.imagesLists" + File.separator + query + ".xml.images.txt";
            BufferedReader br1 = new BufferedReader(new FileReader(imageListFilename));
            String line1 = "";
            while ((line1 = br1.readLine()) != null) {
                allowedImages.add(line1.trim());
            }
            br1.close();
            ArrayList<String> stemmedQuery = new ArrayList<>();
            for (String term : query.split(" ")) {
                char[] termCharArray = term.toCharArray();
                int stemmedLength = ems.stem(termCharArray, term.length());
                stemmedQuery.add(new String(Arrays.copyOfRange(termCharArray, 0, stemmedLength)));

            }
            search(Arrays.copyOfRange(args, 1, args.length), stemmedQuery.toArray(new String[]{}), topic);

        }
        br.close();
    }

    private void search(String[] args, String[] queryTerms, String topic) throws IOException {
        FlagConfig flagConfig;
        List<SearchResult> results;
        try {
            flagConfig = FlagConfig.getFlagConfig(args);
            flagConfig.remainingArgs = queryTerms;
            results = runSearch(flagConfig);
        } catch (IllegalArgumentException e) {
            System.err.println(usageMessage);
            throw e;
        }

        // Print out results.
        int ranking = 0;
        if (results.size() > 0) {
            VerbatimLogger.info("Search output follows ...\n");
            for (SearchResult result : results) {
                
                if (allowedImages.contains(result.getObjectVector().getObject().toString()) && ranking<50) {
                    ++ranking;
                    System.out.println(result.toTrecString(Integer.parseInt(topic), ranking));
                }

                if (flagConfig.boundvectorfile().isEmpty() && flagConfig.elementalvectorfile().isEmpty()) {
                    PsiUtils.printNearestPredicate(flagConfig);
                }
            }

            if (!flagConfig.jsonfile().isEmpty()) {
                PathFinder.pathfinderWriterWrapper(flagConfig, results);
            }
        } else {
            VerbatimLogger.info("No search output.\n");
        }
    }

}
