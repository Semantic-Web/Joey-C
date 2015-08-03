package semanticweb.jena;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.riot.IO_Jena;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.LangBuilder;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParserRegistry;
import org.apache.jena.riot.WebContent;

/**
 * Utility class and methods for making Jena easier to work with.
 * @author Joey Carson <jcarson8@fau.edu>
 */
public class JenaUtil {

	/**
	 * Checks to see if the given language is installed.
	 * @param langName - The name of the language to check for.
	 * @return Returns true if the language associated with the given name is installed
	 * 		   and has a ReaderRIOTFactory associated with it, and false otherwise.
	 */
	public static boolean isLanguageInstalled(String langName)
	{
		Lang lang = RDFLanguages.nameToLang(langName);
		return isLanguageInstalled(lang);
	}
	
	public static boolean isLanguageInstalled(Lang lang)
	{
		return lang != null && RDFLanguages.isRegistered(lang) && RDFParserRegistry.getFactory(lang) != null;
	}
	
	/**
	 * Builds and installs a new Language into Jena with the given parameters.
	 * Check out GitHub for an awesome example program to understand this methods internals, or just read it. :P
	 * https://github.com/apache/jena/blob/master/jena-arq/src-examples/arq/examples/riot/ExRIOT_5.java
	 * 
	 * @param name - The name of the Language.
	 * @param contentType - The ContentType of the associated language.  The content-type of 
	 * 						a Language is only identified as the content type section of the
	 * 						associated MIME type, not the charset.  This can cause confusion
	 * 						in Jena, so calling this method with a ContentType object is safer
	 * 						than simply accepting the appropriate string.
	 * @param riotFactoryClass - A class that extends ReaderRIOTFactory, the class that generates
	 * 							 the parser instance.
	 * @param extensions - Variadic argument with all associated file extensions to be associated 
	 * 					   with language e.g. (txt, html, pdf).
	 */
	public static void installLanguage(JenaLangAdapter langAdapter)
	{
		Lang lang = LangBuilder.create(langAdapter.name(), langAdapter.contentType().getContentType()).addFileExtensions(langAdapter.fileExtensions()).build() ;
		if ( lang != null ) { 
			System.out.println("JenaUtil::installLanguage Installing language: " + lang + " into Jena.");
			RDFLanguages.register(lang) ;
			RDFParserRegistry.registerLangTriples(lang, langAdapter.readerFactory());
			IO_Jena.registerForModelRead(langAdapter.name(), langAdapter.rdfReaderClass());
		}
	}
	
	/**
	 * Resolves the language associated with the given URL string.
	 * @param urlStr - A URL string.
	 * @return The Lang that is registered and is associated to either the MIME type of the URL
	 * 		   or the file extension of the URL, and null otherwise.
	 */
	public static Lang resolveLang(String urlStr)
	{
		Lang lang = null;
		
		try {
			
			URL url = new URL(urlStr);
			URLConnection uc = url.openConnection();
			
			if ( uc instanceof HttpURLConnection ) {
				// If we're dealing with an http connection, we need to set the accept header
				// and ensure that we're doing a HEAD request, lest we download a mega huge file
				// that we can't read anyway.  We should do this for all associated URL connections
				// that we can encounter.  For now this is fine since most are http.
				HttpURLConnection conn = (HttpURLConnection)uc;
				conn.setRequestMethod("HEAD");
				conn.setRequestProperty("Accept", WebContent.defaultGraphAcceptHeader);				
			}

			// Make the connection and try to get the content type.
			uc.connect();	
			String contentTypeHeader = uc.getContentType();
			
			if ( contentTypeHeader != null ) {
				// If we've got a content-type header back, use it to determine if 
				// Jena is configured with a parser that can handle the given data.
				ContentType ct = ContentType.create(contentTypeHeader);
				lang = RDFLanguages.contentTypeToLang(ct.getContentType());
				
				if ( lang != null && RDFParserRegistry.getFactory(lang) != null ) {
					// Supported!
					System.out.println("JenaUtil::resolveLang: " + lang + " resolved by server content type for " + urlStr);
				} else {
					// Hmm, URL connection couldn't resolve content type.  Either the server is silly, or it's a local file
					// without the assistance of a configured web server.  Perhaps we can guess it by name?
					lang = RDFLanguages.filenameToLang(urlStr);
					
					if ( lang != null && RDFParserRegistry.getFactory(lang) != null ) {
						System.out.println("JenaUtil::resolveLang: " + lang + " resolved by file name for " + urlStr);
					} else {
						System.err.println("JenaUtil::resolveLang: no language parser for content type: " + contentTypeHeader);
					}
				}
			}
			
		} catch (Exception e) {
			System.out.println("JenaUtil::resolveLang.  URL connection failed.");
			//e.printStackTrace();
		}
		
		return lang;
	}
}
