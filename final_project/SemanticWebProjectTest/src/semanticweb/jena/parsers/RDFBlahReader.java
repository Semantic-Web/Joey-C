package semanticweb.jena.parsers;

import org.apache.jena.riot.adapters.RDFReaderRIOT;

public class RDFBlahReader extends RDFReaderRIOT {

	public RDFBlahReader()
	{
		super(BlahReader.LangName);
	}

}
