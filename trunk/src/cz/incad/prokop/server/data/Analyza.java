package cz.incad.prokop.server.data;

import static org.aplikator.server.descriptor.Panel.column;
import static org.aplikator.server.descriptor.Panel.row;

import java.util.Date;

import org.aplikator.client.data.ListItem;
import org.aplikator.server.descriptor.Entity;
import org.aplikator.server.descriptor.Property;
import org.aplikator.server.descriptor.Reference;
import org.aplikator.server.descriptor.ReferenceField;
import org.aplikator.server.descriptor.TextArea;
import org.aplikator.server.descriptor.View;

import cz.incad.prokop.server.Structure;

public class Analyza extends Entity {

    public static enum Stav implements ListItem<String>  {
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
    public Property<String> vysledek;
    public Property<String> uzivatel;
    public Reference<Modul> modul;

    public Analyza() {
        super("Analyza","Analyza","Analyza_ID");
        initFields();
    }

    protected void initFields() {
        spusteni = dateProperty("spusteni");
        ukonceni = dateProperty("ukonceni");
        stav = stringProperty("stav");
        vysledek = textProperty("vysledek");
        uzivatel = stringProperty("uzivatel");
        modul = referenceProperty(Structure.modul, "modul");
    }

    @Override
    protected View initDefaultView() {
        View retval = new View(this);
        retval.addProperty(spusteni).addProperty(uzivatel).addProperty(ukonceni).addProperty(stav);
        retval.form(column(
                row(spusteni,ukonceni, stav,uzivatel),
                new TextArea(vysledek).setWidth("100em").setHeight("10em"),
                ReferenceField.reference(modul,Structure.modul.getReverseView(), Structure.modul.nazev)
            ));
        return retval;
    }

}