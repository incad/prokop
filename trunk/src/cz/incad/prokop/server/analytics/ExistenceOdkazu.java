package cz.incad.prokop.server.analytics;

import static org.aplikator.server.data.RecordUtils.newRecord;
import static org.aplikator.server.data.RecordUtils.newSubrecord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.io.Files;
import org.aplikator.client.shared.data.Operation;
import org.aplikator.client.shared.data.Record;
import org.aplikator.client.shared.data.RecordContainer;
import org.aplikator.client.shared.rpc.impl.ProcessRecords;
import org.aplikator.server.Context;
import org.aplikator.server.data.BinaryData;
import org.aplikator.server.persistence.PersisterFactory;
import org.aplikator.server.util.Configurator;

import com.google.common.base.Objects;

import cz.incad.prokop.server.Structure;

public class ExistenceOdkazu implements Analytic {

    Logger log = Logger.getLogger(ExistenceOdkazu.class.getName());




    private static final String query = "select zaz.Zaznam_ID,zaz.url, zaz.hlavniNazev, id.hodnota  from identifikator id left outer join zaznam zaz on id.zaznam = zaz.Zaznam_ID where id.typ = 'cCNB' order by id.hodnota, zaz.hlavniNazev";


    /*
     *  ODKAZY – existence platnost
a)      Katalog NKCR – vypsat záznamy, které mají link do K4 a link není platný (error UUID). Report : ID záznamu/link/status

     * @see cz.incad.prokop.server.analytics.Analytic#analyze(java.lang.String, org.aplikator.client.data.Record, org.aplikator.server.Context)
     */
    @Override
    public void analyze(String params, Record analyza, Context context) {
        //ukázka, jak použít parametry
        String userHome = Configurator.get().getConfig().getString(Configurator.HOME);
        String configFileName = userHome+System.getProperty("file.separator")+params;
        log.info("Random harvester config file name: "+configFileName);
        Radek prvni = null;
        Radek druhy = null;
        Connection conn = PersisterFactory.getPersister().getJDBCConnection();
        Statement st = null;
        ResultSet rs = null;
        File tempFile = null;
        try{
            File tempDir = Files.createTempDir();
            tempFile = new File(tempDir, UUID.randomUUID().toString());
            tempFile.createNewFile();
            log.info("ExistenceOdkazu TEMPFILE:" + tempFile);
            Writer vysledek = new FileWriter(tempFile);

            st = conn.createStatement();
            rs = st.executeQuery(query);
            while (rs.next()){
                druhy = new Radek();
                druhy.id = rs.getString("hodnota");
                druhy.nazev = rs.getString("hlavniNazev");
                druhy.text.append(rs.getInt("Zaznam_ID")).append("\t").append(rs.getString("url")).append("\t").append(rs.getString("hodnota")).append("\t").append(rs.getString("hlavniNazev")).append("\n");
                if (prvni != null ){
                    if (Objects.equal(prvni.id, druhy.id) && !Objects.equal(prvni.nazev,druhy.nazev)){
                        if (!prvni.zapsan){
                            vysledek.append(prvni.text);
                        }
                        vysledek.append(druhy.text);
                        druhy.zapsan = true;
                    }
                }
                prvni = druhy;
            }
            vysledek.close();

            if (tempFile != null){
                BinaryData bd  = new BinaryData("ExistenceOdkazu.txt", new FileInputStream(tempFile), tempFile.length());
                Structure.analyza.vysledek.setValue(analyza, bd);
            }
        } catch (Exception ex){
            log.log(Level.SEVERE, "Chyba v analyze", ex);
        } finally{
            if (rs != null){
                try {
                    rs.close();
                } catch (SQLException e) {}
            }
            if (st != null){
                try {
                    st.close();
                } catch (SQLException e) {}
            }
            if (conn != null){
                try {
                    conn.close();
                } catch (SQLException e) {}
            }
        }
    }

    private static class Radek{
        public String id;
        public String nazev;
        public StringBuilder text = new StringBuilder();
        public boolean zapsan = false;
    }

    @SuppressWarnings("unused")
    private void importRecord(Record sklizen, Context context, int i) {
        RecordContainer rc = new RecordContainer(); //nový prázdný kontejner

        Record zaznam = newRecord(Structure.zaznam); //nový záznam pro tabulku zaznam
        Structure.zaznam.hlavniNazev.setValue(zaznam, "Náhodný název " + Math.random());//nastavit hodnoty v polích nového záznamu
        Structure.zaznam.sklizen.setValue(zaznam, sklizen.getPrimaryKey().getId());//nastavit referenci na související záznam sklizně
        Structure.zaznam.sourceXML.setValue(zaznam, "<xml>Náhodné xml " + Math.random() + " </xml>");
        rc.addRecord(null, zaznam, zaznam, Operation.CREATE);//přidat záznam do kontejneru

        Record identifikator = newSubrecord(zaznam.getPrimaryKey(), Structure.zaznam.identifikator);//nový záznam pro opakované pole (tabulku) identifikátor
        Structure.identifikator.hodnota.setValue(identifikator, "Náhodný id " + Math.random());//opět nastavit hodnoty v záznamu opakovaného pole
        Structure.identifikator.typ.setValue(identifikator, "ISSN");
        rc.addRecord(null, identifikator, identifikator, Operation.CREATE);//přidat záznam opakovaného pole do kontejneru

        Structure.sklizen.pocet.setValue(sklizen, i);  //aktualizovat hodnotu o počtu sklizených titulů v záznamu sklizně
        rc.addRecord(null, sklizen, sklizen, Operation.UPDATE); //přidat záznam sklizně do kontejneru pro aktualizaci

        rc = context.getAplikatorService().processRecords(rc);  //příkaz ProcessRecords uloží všechny záznamy v kontejenru do databáze (v jediné nové transakci) a vrátí zpět kontejner s aktualizovanými daty (tedy dočasné primární klíče nahradí skutečnými)

    }

}
