package web.semantic;

import java.io.IOException;
import java.io.InputStream;

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
import com.hp.hpl.jena.util.FileManager;



public class Modeler {

	public static final String defaultNameSpace = "http://org.nonexistant.default/whatever#";
	public static final String externalNameSpace = "http://www.semanticweb.org/josephcarson/ontologies/2015/6/untitled-ontology-16#";
	
	public final Model friendsModel = ModelFactory.createOntologyModel();
	
	public final Model alignmentModel = ModelFactory.createOntologyModel();
	
	public InfModel inferredModel = null;
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		Modeler m = new Modeler();
		m.loadFoafamaticModel();
		
		System.out.println("Initial friends from FOAFamatic.");
		m.echoFriends(m.friendsModel);
		
		System.out.println("\nLoading FOAF schema.");
		m.loadFOAFSchema();
		m.echoFriends(m.friendsModel);
		
		System.out.println("\nAfter adding external friends.");
		m.loadExternalFriends();
		m.echoFriends(m.friendsModel);
		
		System.out.println("\nAfter adding alignment statements to the alignment model.");
		m.alignModels();
		m.echoFriends(m.friendsModel);
	
		System.out.println("\nAfter binding reasoner.");
		m.bindReasoner();
		m.echoFriends(m.inferredModel);
		
		System.out.println("done");
	}
	
	/**
	 * Loads the foafamatic model.
	 * @throws IOException 
	 */
	private void loadFoafamaticModel() throws IOException
	{
		loadModel("assets/foafamatic.rdf", friendsModel, defaultNameSpace);
	}
	
	/**
	 * Loads the otherFriends.owl file that contains
	 * other friends in Person taxonomy and the taxonomy itself.
	 * @throws IOException 
	 */
	public void loadExternalFriends() throws IOException
	{
		loadModel("assets/otherFriends.owl", alignmentModel, externalNameSpace);
		//loadModel("assets/otherFriends.owl", friendsModel, externalNameSpace);
	}
	
	private void loadModel(String path, Model m, String baseURI) throws IOException
	{
		InputStream inStream = FileManager.get().open(path);
		m.read(inStream, baseURI);
		inStream.close();
	}
	
	// Load the FOAF schema into the primary and alignment models.
	private void loadFOAFSchema() throws IOException {
        // Of course the ontologies can be read from the interwebz.
        //schema.read("http://xmlns.com/foaf/spec/index.rdf");
        //_friends.read("http://xmlns.com/foaf/spec/index.rdf");
        
		String modelPath = "assets/Ontologies/foaf.rdf";
		loadModel(modelPath, friendsModel, defaultNameSpace);
		loadModel(modelPath, alignmentModel, defaultNameSpace);
    }
	
	/**
	 * Adds necessary statements to the alignment model to align FOAF to Person ontologies.
	 */
	private void alignModels()
	{
		// State that joeys_people:Individual is equivalentClass of foaf:Person.
		Resource resource = alignmentModel.createResource(externalNameSpace + "Individual");
		Property prop = alignmentModel.createProperty("http://www.w3.org/2002/07/owl#equivalentClass");
		Resource obj = alignmentModel.createResource("http://xmlns.com/foaf/0.1/Person");
		alignmentModel.add(resource,prop,obj);
		
		// State that joeys_people:hasName is an equivalentProperty of foaf:name
		resource = alignmentModel.createResource(externalNameSpace + "hasName");
		prop = alignmentModel.createProperty("http://www.w3.org/2002/07/owl#equivalentProperty");
		obj = alignmentModel.createResource("http://xmlns.com/foaf/0.1/name");
		alignmentModel.add(resource,prop,obj);
		
		// State that joeys_people:hasFriend is a subproperty of foaf:knows
		resource = alignmentModel.createResource(externalNameSpace + "hasFriend");
		prop = alignmentModel.createProperty("http://www.w3.org/2000/01/rdf-schema#subPropertyOf");
		obj = alignmentModel.createResource("http://xmlns.com/foaf/0.1/knows");
		alignmentModel.add(resource,prop,obj);

		// State that me is the same as PersonMe.  This associates friends of PersonMe
		// as friends of me.  But those friends are duplicates.  Need to get rid of them.
		resource = alignmentModel.createResource(defaultNameSpace + "me");
		prop = alignmentModel.createProperty("http://www.w3.org/2002/07/owl#sameAs");
		obj = alignmentModel.createResource(externalNameSpace + "PersonMe");
		alignmentModel.add(resource,prop,obj);	
		
		
		// WTF... Aligning these objects doesn't do the trick.
		// Without the me->PersonMe alignment, adding the following
		// statements results in these objects returned in the query.
		// That's bad since they're duplicates of the foafamatic friends.
		
		// State that sem web is the same person as Semantic Web
		resource = alignmentModel.createResource(defaultNameSpace + "RichardO");
		prop = alignmentModel.createProperty("http://www.w3.org/2002/07/owl#sameAs");
		obj = alignmentModel.createResource(externalNameSpace + "PersonDickie");
		alignmentModel.add(resource,prop,obj);
		
		// State that sem web is the same person as Semantic Web
		resource = alignmentModel.createResource(defaultNameSpace + "SamB");
		prop = alignmentModel.createProperty("http://www.w3.org/2002/07/owl#sameAs");
		obj = alignmentModel.createResource(externalNameSpace + "PersonShamir");
		alignmentModel.add(resource,prop,obj);
	}
	
	private void bindReasoner()
	{
	    Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
	    reasoner = reasoner.bindSchema(alignmentModel);
	    inferredModel = ModelFactory.createInfModel(reasoner, friendsModel);
	}
	
	public void echoFriends(Model m)
	{
		//?mbox   ?friend foaf:mbox ?mbox
		QueryExecution qe = prepQuery("select DISTINCT ?name where { default:me foaf:knows ?friend. ?friend foaf:name ?name. }", m);
		//QueryExecution qe = prepQuery("select DISTINCT ?myname ?name  where{  joeys_people:PersonMe foaf:knows ?friend. ?friend foaf:name ?name. } ", m);
		
		try {
			ResultSet response = qe.execSelect();
			
			while( response.hasNext() ) 
			{
				QuerySolution soln = response.nextSolution();
				RDFNode name = soln.get("?name");
				
				// email should be a resource.
				RDFNode email = soln.get("?mbox");
				String mailto = "";
				if ( email != null ) {
					if ( email.isResource() ) {
						Resource emailRsrc = (Resource) email.as(Resource.class);
						mailto = emailRsrc.toString();
					}
				}
				
				if ( name != null ) System.out.println( "echo friend: " + name + " email: " + mailto );
				else System.out.println("No Friends found!");
			}
			
		} catch (Exception e) {
			System.out.println("query exception: " + e);
		} finally { 
			qe.close();
		}
	}

	
	/**
	 * Convenience method for preparing a QueryExecution object.
	 */
	private QueryExecution prepQuery(String queryRequest, Model model){
		
		StringBuffer queryStr = new StringBuffer();
		// Establish Prefixes
		//Set default Name space first
		queryStr.append("PREFIX default" + ": <" + Modeler.defaultNameSpace + "> ");
		queryStr.append("PREFIX untitled-ontology-16" + ": <" + Modeler.externalNameSpace + "> ");
		queryStr.append("PREFIX rdfs" + ": <" + "http://www.w3.org/2000/01/rdf-schema#" + "> ");
		queryStr.append("PREFIX rdf" + ": <" + "http://www.w3.org/1999/02/22-rdf-syntax-ns#" + "> ");
		queryStr.append("PREFIX foaf" + ": <" + "http://xmlns.com/foaf/0.1/" + "> ");
		
		//Now add query
		queryStr.append(queryRequest);
		
		Query query = QueryFactory.create(queryStr.toString());
		QueryExecution qexec = QueryExecutionFactory.create(query, model);

		return qexec;
	}

}
