package cz.incad.prokop.server.functions;

import com.fastsearch.esp.content.DocumentFactory;
import com.fastsearch.esp.content.IDocument;
import com.typesafe.config.Config;
import cz.incad.prokop.server.Structure;
import cz.incad.prokop.server.fast.FastIndexer;
import java.util.logging.Logger;
import org.aplikator.server.Context;
import org.aplikator.server.function.Executable;
import org.aplikator.server.function.FunctionParameters;
import org.aplikator.server.function.FunctionResult;
import org.aplikator.server.util.Configurator;

public class ReindexFast implements Executable {
    Logger log = Logger.getLogger(ReindexFast.class.getName());
    private FastIndexer fastIndexer;

    @Override
    public FunctionResult execute(FunctionParameters parameters, Context context) {
        Config config = Configurator.get().getConfig();
        fastIndexer = new FastIndexer(config.getString("fastHost"),
                config.getString("fastCollection"),
                config.getInt("fastBatchSize"));
//        IDocument doc = DocumentFactory.newDocument(urlZdroje);
//        doc.addElement(DocumentFactory.newString("title", hlavninazev));
//        doc.addElement(DocumentFactory.newInteger("dbid", z.getPrimaryKey().getId()));
//        doc.addElement(DocumentFactory.newString("url", urlZdroje));
//        doc.addElement(DocumentFactory.newString("leader_format", leader));
//        doc.addElement(DocumentFactory.newString("zdroj", conf.getProperty("zdroj")));
//        doc.addElement(DocumentFactory.newString("base", conf.getProperty("base")));
//        doc.addElement(DocumentFactory.newString("harvester", conf.getProperty("harvester")));
//        doc.addElement(DocumentFactory.newString("originformat", conf.getProperty("originformat")));
//        doc.addElement(DocumentFactory.newString("data", xmlStr));
//        fastIndexer.add(doc, it);
                    
                    
        return new FunctionResult("Reindexovano", true);
    }
    
}