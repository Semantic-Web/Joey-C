package semanticweb.jena.parsers;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.ReaderRIOT;
import org.apache.jena.riot.ReaderRIOTFactory;

public class BlahReaderFactory implements ReaderRIOTFactory {

	@Override
	public ReaderRIOT create(Lang arg0) {
		System.out.println("BlahReaderFactory::create");
		return new BlahReader();
	}

}
