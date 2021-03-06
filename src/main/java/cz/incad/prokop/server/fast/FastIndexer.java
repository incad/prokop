package cz.incad.prokop.server.fast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fastsearch.esp.content.ContentManagerException;
import com.fastsearch.esp.content.ContentManagerFactory;
import com.fastsearch.esp.content.FactoryException;
import com.fastsearch.esp.content.IContentManager;
import com.fastsearch.esp.content.IContentManagerFactory;
import com.fastsearch.esp.content.IDocument;
import com.fastsearch.esp.content.config.ISubsystem;

/**
 *
 * @author alberto
 */
public class FastIndexer {

    static final Logger logger = Logger.getLogger(FastIndexer.class.getName());
    String host;
    String collection;
    int batchSize;
    DateFormat formatter;
    ArrayList<IDocument> recordsToInsert = new ArrayList<IDocument>();
    ArrayList<IDocument> recordsToModify = new ArrayList<IDocument>();
    ArrayList<IDocument> recordsToDelete = new ArrayList<IDocument>();
    public int NumInserts = 0;
    public int NumUpdates = 0;
    public int NumDeletes = 0;

    public FastIndexer(String host, String collection, int batchSize) {
        this.host = host;
        this.collection = collection;
        this.batchSize = batchSize;
//        host = conf.getProperty("fastHost");
//        collection = conf.getProperty("fastCollection");
//        batchSize = Integer.parseInt(conf.getProperty("fastBatchSize"));
    }

    /*
    private IDocument createDocument(Record fr) throws Exception {
    IDocument doc = DocumentFactory.newDocument(fr.id);
    //Add String elements to the document
    try {
    boolean hasData = false;
    Iterator it = fr.getFields().keySet().iterator();
    while (it.hasNext()) {
    Field ff = fr.getFields().get((String) it.next());
    if (ff.name.equalsIgnoreCase("data")) {
    hasData = true;
    }
    if (ff.type == FieldMappingType.STRING || ff.type == FieldMappingType.CLOB) {
    //if(ff.isMultiple){
    //doc.addElement(DocumentFactory.newStringCollection(ff.name, ff.values));
    //}else{
    doc.addElement(DocumentFactory.newString(ff.name, ff.stringValue()));
    //}

    } else if (ff.type == FieldMappingType.INTEGER) {
    doc.addElement(DocumentFactory.newIntegerCollection(ff.name, ff.values));
    } else if (ff.type == FieldMappingType.BOOLEAN) {
    doc.addElement(DocumentFactory.newBoolean(ff.name, (Boolean) ff.values.get(0)));
    } else if (ff.type == FieldMappingType.DATE) {
    doc.addElement(DocumentFactory.newDate(ff.name, (Date) ff.values.get(0)));
    } else if (ff.type == FieldMappingType.BINARY) {
    doc.addElement(DocumentFactory.newByteArray(ff.name, (byte[]) ff.values.get(0)));
    }

    }
    if (!hasData) {
    doc.addElement(DocumentFactory.newString("data", fr.id));
    }


    } catch (DuplicateElementException e) {
    logger.log(Level.WARNING, "error creating fast document: {0}", fr.id);
    logger.warning(e.toString());
    }
    return doc;
    }
     */
    public void showResults() {
        logger.log(Level.INFO, "Currently... Inserts: {0}. Updates: {1}. Deletes: {2}", new Object[]{NumInserts, NumUpdates, NumDeletes});
    }

    private void sendDeletedRecords() throws Exception {
        if (!recordsToDelete.isEmpty()) {
            sendRecords(recordsToDelete, IndexTypes.DELETED);
            NumDeletes += recordsToDelete.size();
            recordsToDelete.clear();
        }
        showResults();
    }

    private void sendModifiedRecords() throws Exception {
        if (!recordsToModify.isEmpty()) {
            sendRecords(recordsToModify, IndexTypes.MODIFIED);
            NumUpdates += recordsToModify.size();
            recordsToModify.clear();
        }
        showResults();
    }

    private void sendInsertedRecords() throws Exception {
        if (!recordsToInsert.isEmpty()) {
            sendRecords(recordsToInsert, IndexTypes.INSERTED);
            NumInserts += recordsToInsert.size();
            recordsToInsert.clear();
        }
        showResults();
    }

    public void sendPendingRecords() throws Exception {
        sendDeletedRecords();
        sendInsertedRecords();
        sendModifiedRecords();
    }

    private void checkSendRecords(ArrayList<IDocument> records, IndexTypes indexType) throws Exception {
        if (!records.isEmpty()) {
            if (records.size() >= batchSize) {
                sendRecords(records, indexType);
                if (indexType == IndexTypes.INSERTED) {
                    NumInserts += records.size();
                } else if (indexType == IndexTypes.MODIFIED) {
                    NumUpdates += records.size();
                } else if (indexType == IndexTypes.DELETED) {
                    NumDeletes += records.size();
                }
                showResults();
                records.clear();
            }
        }
    }

    public void sendRecords(ArrayList<IDocument> docs, IndexTypes indexType) throws Exception {

        logger.info(String.format("Sending %d records to fast...", docs.size()));
        boolean liveCallbackEnabled = true;
        IContentManager contentManager = null;
        try {

            //Create a IContentManager that uses contentdistributor
            //on "localhost:16100". Callbacks will be received on an Callback instance
            Properties p = new Properties();
            p.put("com.fastsearch.esp.content.http.contentdistributors", host);

            //Create a IContentManagerFacotory using the contentdistributor in the Properties
            // collection
            IContentManagerFactory contentManagerFactory = ContentManagerFactory.newInstance(p);

            //Create callback object that handles callbacks from FAST ESP
            Callback cb = new Callback(liveCallbackEnabled);
            contentManager = contentManagerFactory.create(collection, cb);


            //Configure indexing system to enable/disable live/completed callback
            ISubsystem indexing = contentManager.getSystemConfig().getSubsystem("indexing");
            if (indexing != null) {
                indexing.setCompletedCallbackEnabled(liveCallbackEnabled);
            }

            ArrayList<String> docIds = new ArrayList<String>();
            if (indexType == IndexTypes.DELETED) {
                for (int i = 0; i < docs.size(); ++i) {
                    docIds.add(docs.get(i).getID());
                }

            }
            String batchId = "";
            if (indexType == IndexTypes.INSERTED) {
                batchId = contentManager.addContents(docs);
            } else if (indexType == IndexTypes.MODIFIED) {
                batchId = contentManager.removeContents(docIds);
                batchId = contentManager.addContents(docs);

                //To muzeme delat, xmlmapper ne funguje s PARTIALUPDATE
                //batchId = contentManager.updateContents(docs);

            } else if (indexType == IndexTypes.DELETED) {
                batchId = contentManager.removeContents(docIds);
            }
            logger.log(Level.INFO, "Batch: ''{0}'' sent to Fast ESP...", batchId);

            //wait until the batch is completed
            while (!cb.isSecured(batchId)) {
                try {
                    Thread.sleep(1000);
                    //need to call checkSession to ensure that session to content distributor is
                    // alive
                    contentManager.checkSession();
                } catch (InterruptedException e) {
                    logger.info(".");
                }
            }

            //logger.info("Batch: '" + batchId + "' completed");
            logger.info(String.format("%d records sent to fast", docs.size()));
        } catch (FactoryException e) {
            logger.log(Level.SEVERE, "Failed to create ContentManager {0}", e.toString());
            throw new Exception(e);
        } catch (ContentManagerException e) {
            logger.log(Level.SEVERE, "An error has occured {0}", e.toString());
            throw new Exception(e);
        } finally {
            //free resources
            if (contentManager != null) {
                contentManager.deactivate();
            }
        }
    }

    public void add(IDocument fr, IndexTypes indexType) throws Exception {
        if (indexType == IndexTypes.DELETED) {
            recordsToDelete.add(fr);
            checkSendRecords(recordsToDelete, IndexTypes.DELETED);
        } else if (indexType == IndexTypes.MODIFIED) {
            recordsToModify.add(fr);
            checkSendRecords(recordsToModify, IndexTypes.MODIFIED);
        } else {
            recordsToInsert.add(fr);
            checkSendRecords(recordsToInsert, IndexTypes.INSERTED);
        }
    }
}
