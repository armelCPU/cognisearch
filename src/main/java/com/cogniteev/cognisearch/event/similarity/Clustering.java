package com.cogniteev.cognisearch.event.similarity;

import au.com.bytecode.opencsv.CSVReader;
import com.cogniteev.cognisearch.event.model.Cluster;
import com.cogniteev.cognisearch.event.model.EventEntity;
import com.cogniteev.cognisearch.event.pipe.ElasicsearchObject;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by armel on 07/07/17.
 */
public class Clustering {

  private Utils tools;
  private List<EventEntity> events;
  private List<Cluster> title_clusters;
  private List<Cluster> cat_clusters;
  private List<Cluster> perf_clusters;
  private ElasicsearchObject es;
  private int nb_EN = 0;

  private final double TITLE_THRESHOLD = 0.9;
  private final double CAT_THRESHOLD = 0.50;
  private final double PERF_THRESHOLD = 0.50;


  public Clustering (String es_host, int es_port, String index, String type){
    tools =  new Utils(es_host, es_port, index, type);
    es = new ElasicsearchObject(es_host, es_port);
  }

  /**
   * Load Clusters data from ElasticSearch
   */
  public void loadEventsClusters(){
    this.title_clusters = tools.getPropClusters("title", "clusters_events", "event_cluster");
    this.cat_clusters = tools.getPropClusters("category", "clusters_events", "event_cluster");
    this.perf_clusters = tools.getPropClusters("performer", "clusters_events", "event_cluster");

    /*
    System.out.println("Nb events : " + this.nb_EN);
    System.out.println("Nb title clusters : " + this.title_clusters.size());
    System.out.println("Nb cat clusters : " + this.cat_clusters.size());
    System.out.println("Nb perf clusters : " + this.perf_clusters.size());
    */
  }

  public void printClusters( List<Cluster> c){
    for ( Cluster c1 : c)
      System.out.println(c1);

    System.out.println();
  }

  /**
   * Load the index
   */
  public void loadIndex(){
    this.events = this.tools.getAll();
    this.nb_EN = this.events.size();
  }

  /**
   *
   * @return
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public List<Cluster> buildCategoryClusters() throws IOException, SAXException, ParserConfigurationException {
    this.loadIndex();


    List<Cluster> clusters = new ArrayList<>();

    Cluster firstCluster = new Cluster();
    firstCluster.setName("Cluster 1");
    firstCluster.setCentroid(this.events.get(0).getCategories());
    firstCluster.getElements().add(this.events.get(0).getId());

    clusters.add(firstCluster);

    for(int i=1; i< this.events.size(); i++) {
      if ( events.get(i).getCategories() == null)
          continue;

      boolean found = false;

      for ( Cluster clust : clusters) {
        if ( tools.ensembleSimilarity(events.get(i).getCategories(), (List<String>) clust.getCentroid(), "Concept") > CAT_THRESHOLD) {
          // Add in the cluster
          clust.getElements().add(this.events.get(i).getId());

          // Update the centroid
          for ( String cat : events.get(i).getCategories()){
            if ( ! ((List<String>) clust.getCentroid()).contains(cat) ){
              ((List<String>) clust.getCentroid()).add(cat);
            }
          }

          found = true;
          break;
        }
      }

      // If not found build a new one
      if( !found)
      {
        Cluster newCluster = new Cluster();
        newCluster.setName("Cluster " + (i + 1));
        newCluster.setCentroid(this.events.get(i).getCategories());
        newCluster.getElements().add(this.events.get(i).getId());

        clusters.add(newCluster);
      }

    }

    return  clusters;
  }

  /**
   *
   * @return
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public List<Cluster> buildPerformerClusters() throws IOException, SAXException, ParserConfigurationException {
    this.loadIndex();

    List<Cluster> clusters = new ArrayList<>();

    Cluster firstCluster = new Cluster();
    firstCluster.setName("Cluster 1");

    firstCluster.setCentroid(this.events.get(0).getPerformers());
    firstCluster.getElements().add(this.events.get(0).getId());

    clusters.add(firstCluster);

    for(int i=1; i< this.events.size(); i++) {
      if ( events.get(i).getPerformers() == null)
        continue;

      boolean found = false;

      for ( Cluster clust : clusters) {
        if ( tools.ensembleSimilarity(events.get(i).getPerformers(), (List<String>) clust.getCentroid(), "ShortText") > PERF_THRESHOLD) {
          // Add in the cluster
          clust.getElements().add(this.events.get(i).getId());

          // Update the centroid
          for ( String perf : events.get(i).getPerformers()){
            if ( ! ((List<String>) clust.getCentroid()).contains(perf.toLowerCase()) ){
              ((List<String>) clust.getCentroid()).add(perf.toLowerCase());
            }
          }

          found = true;
          break;
        }
      }

      // If not found build a new one
      if( !found)
      {
        Cluster newCluster = new Cluster();
        newCluster.setName("Cluster " + (i + 1));
        newCluster.setCentroid(this.events.get(i).getPerformers());
        newCluster.getElements().add(this.events.get(i).getId());

        clusters.add(newCluster);
      }

    }

    return  clusters;
  }

  /**
   * Build Cluster from index
   * @return
   */
  public List<Cluster> buildTitleClusters(){

    List<Cluster> clusters = new ArrayList<>();

    Cluster firstCluster = new Cluster();
    firstCluster.setName("Cluster 1");
    firstCluster.setCentroid(this.events.get(0).getTitle());
    firstCluster.getElements().add(this.events.get(0).getId());

    clusters.add(firstCluster);

    for(int i=1; i< this.events.size(); i++) {
      boolean found = false;
      for ( Cluster clust : clusters) {
        if ( tools.softTFIDFscore(this.events.get(i).getTitle(), (String)clust.getCentroid()) > TITLE_THRESHOLD) {
          // Add in the cluster
          clust.getElements().add(this.events.get(i).getId());

          // Update the centroid
          List<String> centroid_tokens = new ArrayList<>();
          for( String token : ((String) clust.getCentroid()).replace("-", " ").split(" "))
              centroid_tokens.add(token);

          List<String> current_title_tokens = new ArrayList<>();
          for ( String token : this.events.get(i).getTitle().replace("-", " ").split(" "))
              current_title_tokens.add(token);

          for ( String current_title_token : current_title_tokens){
            if (! centroid_tokens.contains(current_title_token))
              centroid_tokens.add(current_title_token);
          }

          clust.setCentroidFromArray(centroid_tokens);

          found = true;
          break;
        }
      }

      // If not found build a new one
      if( !found)
      {
        Cluster newCluster = new Cluster();
        newCluster.setName("Cluster " + (i + 1));
        newCluster.setCentroid(this.events.get(i).getTitle());
        newCluster.getElements().add(this.events.get(i).getId());

        clusters.add(newCluster);
      }

    }

    return  clusters;
  }

  /**
   *
   * @throws ParserConfigurationException
   * @throws SAXException
   * @throws IOException
   */
  public void buildClusters() throws ParserConfigurationException, SAXException, IOException, ParseException {

    // Load event for building clusters
    this.loadIndex();

    this.title_clusters = this.buildTitleClusters();

    this.cat_clusters = this.buildCategoryClusters();

    this.perf_clusters = this.buildPerformerClusters();
    /*
      index cluster
    */

    this.es.indexEventClusters(this.title_clusters, this.cat_clusters, this.perf_clusters);
  }

  /**
   * This function computes the module of a vector given as parameter
   * @param v
   * @return
   */
  public double module(double[] v){
    double square_sum = 0.0;

    for ( double d : v) {
      square_sum += Math.pow(d, 2);
    }
    return  Math.sqrt(square_sum);
  }

  public double scalar_product(double[] v1, double[] v2){
    double scalar_product = 0.0;
    for (int i =0; i< v1.length; i++)
      scalar_product += (v1[i]*v2[i]);

    return scalar_product;
  }

  /**
   * This function computes the cosine between two vectors of the space
   * @param v1
   * @param v2
   * @return
   */
  private double cosine(double[] v1, double[] v2){
    if ( v1 == null || v2 == null) {
      System.out.println("Attention un des deux vecteurs est nul");
      return 0.0;
    }

    if (v1.length != v2.length){
      System.out.println("Les deux vecteurs doivent avoir la même taille");
      return 0.0;
    }

    double scalar_product = 0.0;
    for (int i =0; i< v1.length; i++)
      scalar_product += (v1[i]*v2[i]);

    return scalar_product/(this.module(v1)*this.module(v2));
  }

  private double similarityWithClustering(String id1, String id2) throws ParserConfigurationException, SAXException, IOException {

    double[] v1 = this.eventVector(id1);
    double[] v2 = this.eventVector(id2);

    return this.cosine(v1, v2);
  }

  /**
   * Compute the IDF of a cluster
   * @param c
   * @return
   */
  private double cluster_inverse_frequency(Cluster c){
    return Math.log((double)(this.nb_EN/c.getElements().size()));
  }

  /**
   * Build a cluster vector
   * @param id
   * @return
   * @throws IOException
   * @throws SAXException
   * @throws ParserConfigurationException
   */
  public double[] eventVector(String id) throws IOException, SAXException, ParserConfigurationException {

    double[] vect = new double[this.title_clusters.size() + this.cat_clusters.size() + this.perf_clusters.size()];

    EventEntity e =  tools.getEvent(id);

    int i = 0;

    for( Cluster c : title_clusters) {
      // Get the clusters
      if (e.getTitle() != null) {
        double coef = tools.softTFIDFscore(e.getTitle(), (String) c.getCentroid()) > this.TITLE_THRESHOLD ? 1 : 0;
        vect[i] = this.cluster_inverse_frequency(c)*coef;

      } else {
        vect[i] = 0.0;
      }

      i++;
    }

    for ( Cluster c : cat_clusters) {
      if ( e.getCategories() != null ) {
        double coef = tools.ensembleSimilarity(e.getCategories(), c.getCentroid() instanceof String ? Arrays.asList((String) c.getCentroid()) : (List < String >) c.getCentroid(), "Concept") > this.CAT_THRESHOLD ? 1 : 0;
        vect[i] = this.cluster_inverse_frequency(c)*coef;
        //vect[i] = this.cluster_inverse_frequency(c)*tools.ensembleSimilarity(e.getCategories(), c.getCentroid() instanceof String ? Arrays.asList((String) c.getCentroid()) : (List < String >) c.getCentroid(), "Concept");

        if (Double.isNaN(vect[i])){
          System.out.println(e.getCategories() + "      " + c.getCentroid());
          System.out.println(this.cluster_inverse_frequency(c));
        }


      }
      else {
        vect[i] = 0.0;
      }

      i++;
    }

    for (Cluster c : perf_clusters) {
      if ( e.getPerformers() != null ) {
        double coef = tools.ensembleSimilarity(e.getPerformers(),c.getCentroid() instanceof String ? Arrays.asList((String) c.getCentroid()) : (List<String>) c.getCentroid(), "ShortText") > this.PERF_THRESHOLD ? 1 : 0;
        vect[i] = this.cluster_inverse_frequency(c)*coef;
        // vect[i] = this.cluster_inverse_frequency(c)*tools.ensembleSimilarity(e.getPerformers(),c.getCentroid() instanceof String ? Arrays.asList((String) c.getCentroid()) : (List<String>) c.getCentroid(), "ShortText");
      }
      else {
        vect[i] = 0.0;
      }

      i++;
    }

    return  vect;
  }

  /**
   * Compute the similarity of a set of event couple
   * @param file
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   */
  public void similarityOfSet(String file) throws IOException, ParserConfigurationException, SAXException {
    this.loadIndex();

    // Load data from ES
    this.loadEventsClusters();

    File inputFile = new File(file);
    // Read existing file
    CSVReader reader = new CSVReader(new FileReader(inputFile), '\t');
    List<String[]> csvBody = reader.readAll();

    int i = 0;
    for ( String[] line : csvBody){
      if (i == 0) {
        i++;
        continue;
      }
      line[9] = "" + this.similarityWithClustering(line[1], line[2]);
      System.out.print(tools.printArray(line));
    }
  }


  public static void main(String[] args) throws Exception {
    Clustering clut = new Clustering("localhost", 9300, "events_similarity", "event");
    // clut.buildClusters();
    //clut.similarityOfSet("resources/testset.csv");
    //clut.loadEventsClusters();
    double[] v1 = {0.75, 0.31, 0.181, 0,  0,   0,  0.75, 0.2, 0.15};
    double[] v2 = {0.70, 0.31, 0.181, 0.42, 0.82, 0.05, 0, 0 ,0};

    System.out.println(clut.module(v1));
    System.out.println(clut.module(v2));
    System.out.println(clut.scalar_product(v1, v2));
    System.out.println(clut.cosine(v1, v2));
  }
}
