package semanticweb.jena;

import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.ReaderRIOTFactory;
import org.apache.jena.riot.adapters.RDFReaderRIOT;

/**
 * Languages in Jena are broken down into different components used internally
 * for processing including Lang, RDFReaderRIOT, ContentType, etc.  This class
 * creates a common interface to associate all of them together instead of having
 * to work compose them together all over the place. 
 * @author Joey Carson <jcarson8@fau.edu>
 */
public class JenaLangAdapter {
	
	private String name;
	
	private ContentType contentType;
	
	private ReaderRIOTFactory readerFactory;
	
	private Class <? extends RDFReaderRIOT> rdfReaderClass;
	
	private String[] extensions;
	
	/**
	 * Convenience object for grouping associated properties of a Jena language parser.
	 */
	private JenaLangAdapter() {}
	public String name() { return this.name; }
	public ContentType contentType() { return this.contentType; }
	public ReaderRIOTFactory readerFactory() { return this.readerFactory; }
	public Class <? extends RDFReaderRIOT> rdfReaderClass() { return this.rdfReaderClass; }
	public String[] fileExtensions() { return this.extensions; }
	
	/**
	 * Convenience factory method for building a JenaLanguageAdapter.
	 * @param name - The Lang name.
	 * @param contentType - The ContentType associated with the Lang.
	 * @param readerFactory - The factory object for building readers.
	 * @param rdfReaderClass - The Class for generating RDFReaders.
	 * @param extensions - The filename extensions that are associated with the Lang.
	 * @return A JenaLangAdapter configured with the given parameters.
	 */
	public static JenaLangAdapter create(String name, 
										 ContentType contentType, 
										 ReaderRIOTFactory readerFactory, 
										 Class <? extends RDFReaderRIOT> rdfReaderClass, 
										 String... extensions) 
	{
		JenaLangAdapter a = new JenaLangAdapter();
		a.name = name;
		a.contentType = contentType;
		a.readerFactory = readerFactory;
		a.rdfReaderClass = rdfReaderClass;
		a.extensions = extensions;
		
		return a;
	}
}
