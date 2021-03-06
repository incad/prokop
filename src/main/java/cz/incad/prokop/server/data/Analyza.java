package cz.incad.prokop.server.data;

import cz.incad.prokop.server.Structure;
import org.aplikator.client.shared.data.ListItem;
import org.aplikator.server.data.BinaryData;
import org.aplikator.server.descriptor.*;

import java.util.Date;

import static org.aplikator.server.descriptor.Panel.column;
import static org.aplikator.server.descriptor.Panel.row;

public class Analyza extends Entity {

    public static enum Stav implements ListItem  {
        ZAHAJENA("zahajena"), UKONCENA("ukoncena"), CHYBA("chyba");

        private Stav(String value){
            this.value = value;
        }
        private String value;
        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String getName() {
            return value;
        }
    }

    public Property<Date> spusteni;
    public Property<Date> ukonceni;
    public Property<String> stav;
    public Property<BinaryData> vysledek;
    public Property<String> uzivatel;
    public Property<String> parametry;
    public Reference<Modul> modul;
    public Property<String> zdroj;

    public Analyza() {
        super("Analyza","Analyza","Analyza_ID");
        initFields();
    }

    protected void initFields() {
        spusteni = dateProperty("spusteni");
        ukonceni = dateProperty("ukonceni");
        stav = stringProperty("stav");
        vysledek = binaryProperty("vysledek");
        uzivatel = stringProperty("uzivatel");
        //zdroj = stringProperty("zdroj");
        modul = referenceProperty(Structure.modul, "modul");
        parametry = stringProperty("parametry");
    }

    @Override
    protected View initDefaultView() {
        View retval = new View(this);
        retval.addProperty(spusteni).addProperty(uzivatel).addProperty(ukonceni).addProperty(stav);
        retval.form(column(
                row(spusteni,ukonceni, stav,uzivatel, parametry),
                new BinaryField(vysledek)
            ));
        return retval;
    }

}
