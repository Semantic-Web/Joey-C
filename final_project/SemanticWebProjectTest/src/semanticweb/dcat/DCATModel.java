package semanticweb.dcat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.riot.Lang;

import semanticweb.jena.JenaUtil;
import semanticweb.jena.parsers.BlahReader;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.InfModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import com.hp.hpl.jena.vocabulary.VCARD;
import com.hp.hpl.jena.vocabulary.XSD;

/**
 * 
 * @author Joey Carson <jcarson8@fau.edu>
 *
 */
public class DCATModel {
	
	/**
	 * Dublin Core Base URI.
	 */
	public static final String DCT_URI_BASE = "http://purl.org/dc/terms/";
	
	public static final String DCTYPE_URI_BASE = "http://purl.org/dc/dcmitype/";
	
	public static final String DCT_URI_TITLE = DCT_URI_BASE + "title";
	
	
	/**
	 * Data Catalog Base URI.
	 */
	public static final String DCAT_URI_BASE = "http://www.w3.org/ns/dcat#";
	
	public static final String DCAT_URI_DISTRIBUTION = DCAT_URI_BASE + "distribution";
	
	public static final String DCAT_URI_KEYWORD = DCAT_URI_BASE + "keyword";
	
	/**
	 * FOAF Catalog Base.
	 */
	public static final String SKOS_URI_BASE = "http://www.w3.org/2004/02/skos/core#"; 
	
	/**
	 * List of standard namespaces that are used in DCAT documents.
	 * http://www.w3.org/TR/vocab-dcat/#namespaces-1
	 * 
	 * These namespaces will be automatically added as prefixes in all
	 * queries so that query consumers don't have to add them manually.
	 */
	private HashMap<String, String> prefixNamespaces;
	
	/**
	 * The prefix string used for queries.  The prefix string is generated
	 * for all queries no matter which model is targeted.
	 */
	private String prefixStr = null;

	/**
	 * The model used to store and query the DCAT document.  We use 
	 * a standard RDF model since we don't need any kind of inference
	 * to be made, we only need to query metadata about the datasets.
	 */
	private OntModel dcatModel = null;
	
	/**
	 * The model that we will load from the selected dataset distributions.
	 * We use the OntModel since we want to inherit OWL basic reasoning.
	 * @see https://jena.apache.org/documentation/javadoc/jena/com/hp/hpl/jena/rdf/model/ModelFactory.html#createOntologyModel()
	 */
	private OntModel datasetModel = null;
	
	/**
	 * The model used to load and store schemas (ontologies).
	 */
	private OntModel ontologiesModel = ModelFactory.createOntologyModel();
	
	/**
	 * The inferred model generated based on the 
	 */
	private InfModel inferredModel = null;
	
	/**
	 * Constructor.
	 * Defaults the prefix URI map to the following.
	 * http://www.w3.org/TR/vocab-dcat/#namespaces-1
	 * 
	 * Prefix	Namespace
	 * dcat		http://www.w3.org/ns/dcat#
	 * dct		http://purl.org/dc/terms/
	 * dctype	http://purl.org/dc/dcmitype/
	 * foaf		http://xmlns.com/foaf/0.1/
	 * rdf		http://www.w3.org/1999/02/22-rdf-syntax-ns#
	 * rdfs		http://www.w3.org/2000/01/rdf-schema#
	 * skos		http://www.w3.org/2004/02/skos/core#
	 * vcard	http://www.w3.org/2006/vcard/ns#
	 * xsd		http://www.w3.org/2001/XMLSchema#
	 * 
	 */
	public DCATModel()
	{
		// Standard namespaces used in DCAT documents.  Where Jena defines
		// them for ease of use we take their URI, but others are hardcoded.
		// Interesting that owl is not included.  But we should it by default.
		prefixNamespaces = new HashMap<String, String>();
		setNsPrefix("dcat", DCAT_URI_BASE);
		setNsPrefix("dct", DCT_URI_BASE);
		setNsPrefix("dctype", DCTYPE_URI_BASE);
		setNsPrefix("foaf", FOAF.getURI());
		setNsPrefix("rdf", RDF.getURI());
		setNsPrefix("rdfs", RDFS.getURI());
		setNsPrefix("skos", SKOS_URI_BASE);
		setNsPrefix("vcard", VCARD.getURI());
		setNsPrefix("xsd", XSD.getURI());
	}
	
	/**
	 * Allows the caller to load all relevant ontologies for the intended query.
	 * @param ontologyURL
	 */
	public void loadOnotology(String ontologyURL)
	{
		ontologiesModel.read(ontologyURL);
	}
	
	public void clearOnotologies()
	{
		ontologiesModel.removeAll();
		inferredModel = null;
	}
	
	static // TODO: Register default handlers for common MIME types found in DCAT distributions  
	{	   // with the Jena if they haven't been registered yet.  This is useful if a consumer
		   // wants to provide their own implementation of certain types that DCATModel already 
		   // supports, but they wish to override with their own implementation.
		if ( !JenaUtil.isLanguageInstalled(BlahReader.LangName) ) {
			JenaUtil.installLanguage(BlahReader.LangAdapter);
		}
	}
	
	/**
	 * Adds the prefix with associated namespace URI for the query context.
	 * If the given prefix is one of the prepopulated standard DCAT prefixes,
	 * then the standard prefix is overwritten with the new one.  A prefix can
	 * be removed by setting a null namespaceURI parameter for the associated
	 * prefix string.
	 * @param prefix - The namespace prefix to use.
	 * @param namespaceURI - The namespace URI to associated with the prefix.
	 */
	public void setNsPrefix(String prefix, String namespaceURI) 
	{
		if ( namespaceURI == null && prefixNamespaces.containsKey(prefix) ) {
			// We're removing the given prefix.
			prefixNamespaces.remove(prefix);
		} else if ( prefix != null ) {
			// We're adding or overwriting the given prefix -> namespace pair.
			prefixNamespaces.put(prefix, namespaceURI);
		}
		
		// Finally, reset the prefixString to null so that it can be regenerated
		// the next time we execute a query.
		prefixStr = null;
	}
	
	
	
	/**
	 * Loads the DCAT model into memory.
	 * @param URI - A URI that is known to produce a valid DCAT document.
	 * @param baseURI - A URI string used as the base URI for fully qualifying any
	 * 					relative URI's encountered in all payloads.  Without using
	 * 					parameter, a caller has no real way of directly addressing
	 * 					elements that aren't fully qualified. 
	 * @return An ArrayList<String> of URL's from the DCAT document that 
	 * 		   were able to be successfully loaded into the dataset model.
	 *         These can then be used by the caller to set them as prefixes
	 *         of relative URI's in each model, similar to the base URI
	 *         that would be passed to {@link Model#read(InputStream, String)}.
	 */
	public ArrayList<String> load(String URI, String baseURI)
	{
		ArrayList<String> distributionURLs = new ArrayList<String>();
		// 1. Load the DCAT model into memory.
		dcatModel = ModelFactory.createOntologyModel();
		dcatModel.read(URI);
		
		// 2.  Query the datasets for each distribution and select a distribution that we can handle.
		// TODO: Implement optional reading of both downloadURL and accessURL for each distribution
		// such that handling code can make a better decision of what to do with it.
		StringBuffer distQuery = new StringBuffer("SELECT DISTINCT ?dist ?url ?format \n");
								 distQuery.append("where { \n");
								 distQuery.append("	?subj dcat:distribution ?dist.\n");
								 distQuery.append("	OPTIONAL { ?dist dct:format ?f. ?f rdf:value ?format. } \n");
								 distQuery.append("	OPTIONAL { ?dist dcat:downloadURL ?url. } \n");
								 distQuery.append("	OPTIONAL { ?dist dcat:accessURL ?url. } \n");
								 distQuery.append("}");
		
		QueryExecution qe = prepQuery(distQuery.toString(), dcatModel);
		List<HashMap<String, RDFNode>> dcatResults = runQuery(qe);
		
		// 3.  For each of the distribution URI's, check to see whether or not we have a custom
		// adapter installed.  If so, first run the custom adapter passing in the dataset model
		// and an input stream of the associated resource.  In order to do such a thing, we'd
		// need a mime type of the file, just like the Model interface would.
		datasetModel = ModelFactory.createOntologyModel();
		inferredModel = null;

		Iterator<HashMap<String, RDFNode>> resultIter = dcatResults.listIterator();
		while ( resultIter.hasNext() ) {
			
			HashMap<String, RDFNode> r = resultIter.next();
			
			if ( r.containsKey("url") ) {
				
				// Extract the URL and see if an appropriate language is understood
				// according to the ContentType or the file extension.
				String url = r.get("url").toString();
				Lang urlLang = JenaUtil.resolveLang(url);
				
				if ( urlLang != null ) {
					try {
						datasetModel.read(url, baseURI, urlLang.getName());
						distributionURLs.add(url);
					} catch (Exception e) {
						System.err.println("Failed reading " + url + " into model.");
						System.err.println(e);
					}
				} else {
					System.err.println("Jena is not configured to handle distribution " + url);
				}
			}
		}
		
		return distributionURLs;
	}
	
	public void addOntologyAssertion(Resource s, Property p, RDFNode o) {
		this.ontologiesModel.add(s, p, o);
	}
	
	/**
	 * Prepare a query bound to the DCAT Model object for retrieval of DCAT metadata.
	 * @param queryRequest - The query string.
	 * @return A QueryExecution object ready for use if load has been called successfully, null otherwise.
	 */
	public QueryExecution prepDcatQuery(String queryRequest)
	{
		return dcatModel != null ? prepQuery(queryRequest, dcatModel) : null;
	}
	
	/**
	 * Prepare a query bound to the DCAT distribution model.
	 * @param queryRequest - The query string.
	 * @return A QueryExecution object ready for use if load has been called successfully, null otherwise.
	 */
	public QueryExecution prepDataQuery(String queryRequest)
	{
		return datasetModel!= null ? prepQuery(queryRequest, datasetModel) : null;
	}
	
	public QueryExecution prepInferredQuery(String queryRequest)
	{
		// If the dataset model isn't populate, we definitely can't create an inferred query.
		if ( datasetModel == null ) return null;
		
		if ( inferredModel == null ) {
		    Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
		    reasoner = reasoner.bindSchema(ontologiesModel);
		    inferredModel = ModelFactory.createInfModel(reasoner, datasetModel);
		}
		
		return inferredModel != null ? prepQuery(queryRequest, inferredModel) : null;
	}
	
	/**
	 * Convenience method for preparing a QueryExecution object.
	 */
	private QueryExecution prepQuery(String queryRequest, Model model) {
		
		StringBuffer queryStr = new StringBuffer();

		// Apply all prefixes to the query.
		if ( prefixStr == null ) {
			// If the prefix string hasn't been generated yet, use the queryStr buffer to build it
			// and assign the prefixStr to that generated string.
			for (String prefix : prefixNamespaces.keySet() ) {
				queryStr.append("PREFIX ").append(prefix).append(": <").append(prefixNamespaces.get(prefix)).append("> \n");
			}
			
			// Save a copy of the new string.  But since queryStr buffer is already set up, we can 
			// just append the queryRequest string to it afterward.
			prefixStr = queryStr.toString();
		} else {
			// The prefix string is already generated, append it as the first part of the queryStr buffer.
			queryStr.append(prefixStr);
		}
		
		//Now add the query
		queryStr.append(queryRequest);
		System.out.println(queryStr);
		
		// Build the query execution wrapper and give it back.	
		Query query = QueryFactory.create(queryStr.toString());
		QueryExecution qexec = QueryExecutionFactory.create(query, model);

		return qexec;
	}
	
	/**
	 * Execute the given query and return a list of mapped variable name to value pairs.
	 * @param qe - A QueryExecution object (like one produced by prepQuery).
	 * @return - A list of HashMap<String, RDFNode> containing each resultant row.
	 */
	public List<HashMap<String, RDFNode>> runQuery(QueryExecution qe)
	{
		ArrayList<HashMap<String, RDFNode>> results = new ArrayList<HashMap<String, RDFNode>>();
		
		try {

			ResultSet response = qe.execSelect();
			System.out.println("Preparing Query Results.");
			// The good old fashioned extraction loop like with relational databases.			
			while( response.hasNext() ) 
			{
				QuerySolution soln = response.nextSolution();
				HashMap<String, RDFNode> resultMap = new HashMap<String, RDFNode>();
				
				// Traverse all values returned in this solution.
				Iterator<String> vars = soln.varNames();
				while ( vars.hasNext() ) {
					String varName = vars.next();
					RDFNode node = soln.get(varName);
					
					System.out.println(varName + " : " + node + " ");
					resultMap.put(varName, node);
				}
				
				System.out.println("");
				
				// Add it to the list.
				results.add(resultMap);
			}
			
		} catch (Exception e) {
			System.out.println("query exception: " + e);
		} finally { 
			qe.close();
		}
		
		return results;
	}
	
	
	// Example of manual graph navigation.
	/*
	String uri = "http://catalog.data.gov/dataset/consumer-complaint-database";
	Resource dataSetResource = dcatModel.getResource(uri);
	
	
	//String propString = "dcat:distribution";
	Property searchProperty = dcatModel.createProperty(DCT_URI_TITLE);
	StmtIterator i = dataSetResource.listProperties(searchProperty);
	
	while ( i.hasNext() ) {
		
		Statement s = i.nextStatement();
		System.out.println(s);
		
		if ( s.isResource() ) {
			Resource r = s.getResource();
			StmtIterator j = r.listProperties();
			
			while ( j.hasNext() ) {
				Statement statement = j.nextStatement();
				RDFNode node = statement.getObject();
				Property p = statement.getPredicate();
				
				if ( !node.isLiteral() ) {
					System.out.println("	resource: " + p + " : " + node);
				} else {
					System.out.println("	literal: " + p + " : " + node.asLiteral());
				}
			}
		}
	}		

	Statement foundStmt = dataSetResource.getProperty(searchProperty);
	if ( foundStmt != null ) System.out.println(foundStmt);
	*/
			
}
