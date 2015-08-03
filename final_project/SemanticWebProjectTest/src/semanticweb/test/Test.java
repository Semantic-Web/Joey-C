package semanticweb.test;

import semanticweb.dcat.DCATModel;
import semanticweb.jena.parsers.BlahPerson;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.sparql.vocabulary.FOAF;
import com.hp.hpl.jena.vocabulary.OWL;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		System.out.println("Starting...");
		String foaf_friends = "http://dcat.query.defaultns#";
		
		DCATModel d = new DCATModel();
		d.setNsPrefix("foaf_friends", foaf_friends);
		d.load("test/dcat_test.rdf", foaf_friends);
		
		d.setNsPrefix("blah", BlahPerson.getURI());
		
		// Create alignment using the following assertions.
		d.addOntologyAssertion(FOAF.Person, OWL.equivalentClass, BlahPerson.BlahPerson);
		d.addOntologyAssertion(FOAF.name, OWL.equivalentProperty, BlahPerson.hasFirstName);
		
		Resource foafKnowsPropertyResource = ResourceFactory.createResource(FOAF.knows.getURI());
		Resource blahHasFriendPropertyResource = ResourceFactory.createResource(BlahPerson.hasFriend.getURI());
		d.addOntologyAssertion(foafKnowsPropertyResource, OWL.equivalentProperty, blahHasFriendPropertyResource);
		
		// Make me and Joey the same.
		Resource foafMe = ResourceFactory.createResource(foaf_friends + "me");
		Resource blahJoey = ResourceFactory.createResource(BlahPerson.getURI() + "Joey");
		d.addOntologyAssertion(foafMe, OWL.sameAs, blahJoey);
		
		// Select all predicates.
		//QueryExecution qe = d.prepInferredQuery("SELECT DISTINCT ?s ?n where { ?s a foaf:Person; foaf:name ?n. }");
		QueryExecution qe = d.prepInferredQuery("SELECT DISTINCT ?f ?n where { foaf_friends:me foaf:knows ?f. ?f foaf:name ?n. }");
		d.runQuery(qe);
		
		System.out.println("done");
	}

}
