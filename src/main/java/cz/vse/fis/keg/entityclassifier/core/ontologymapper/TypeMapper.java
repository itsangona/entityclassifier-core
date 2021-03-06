/*
 * #%L
 * Entityclassifier.eu NER CORE v3.9
 * %%
 * Copyright (C) 2015 Knowledge Engineering Group (KEG) and Web Intelligence Research Group (WIRG) - Milan Dojchinovski (milan.dojchinovski@fit.cvut.cz)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package cz.vse.fis.keg.entityclassifier.core.ontologymapper;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.vocabulary.RDFS;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Milan Dojchinovski <milan.dojchinovski@fit.cvut.cz>
 * http://dojchinovski.mk
 */
public class TypeMapper {
    
    private static TypeMapper instance = null;

    /**
     * @return the en_inferred_mappings
     */
    private static String getEn_inferred_mappings() {
        return en_inferred_mappings;
    }

    /**
     * @param aEn_inferred_mappings the en_inferred_mappings to set
     */
    public static void setEn_inferred_mappings(String aEn_inferred_mappings) {
        en_inferred_mappings = aEn_inferred_mappings;
    }

    /**
     * @return the de_inferred_mappings
     */
    public static String getDe_inferred_mappings() {
        return de_inferred_mappings;
    }

    /**
     * @param aDe_inferred_mappings the de_inferred_mappings to set
     */
    public static void setDe_inferred_mappings(String aDe_inferred_mappings) {
        de_inferred_mappings = aDe_inferred_mappings;
    }

    /**
     * @return the nl_inferred_mappings
     */
    public static String getNl_inferred_mappings() {
        return nl_inferred_mappings;
    }

    /**
     * @param aNl_inferred_mappings the nl_inferred_mappings to set
     */
    public static void setNl_inferred_mappings(String aNl_inferred_mappings) {
        nl_inferred_mappings = aNl_inferred_mappings;
    }
    private  Model enModel = null;
    private  Model deModel = null;
    private  Model nlModel = null;
    
    private static String en_inferred_mappings;
    private static String de_inferred_mappings;
    private static String nl_inferred_mappings;
    
    public static TypeMapper getInstance(){
        if(instance == null) {
            instance = new TypeMapper(); 
        }
        return instance;
    }
    
    public void init() {
        try {
            System.out.println(en_inferred_mappings);
            System.out.println(de_inferred_mappings);
            System.out.println(nl_inferred_mappings);
        System.out.println("started loading EN infered ");
            enModel = ModelFactory.createDefaultModel();
            InputStream inEn = FileManager.get().open( en_inferred_mappings );
            enModel.read(inEn, null, "N-TRIPLE");
            inEn.close();
            
        System.out.println("started loading DE infered ");
            deModel = ModelFactory.createDefaultModel();
            InputStream inDe = FileManager.get().open( de_inferred_mappings );
            deModel.read(inDe, null, "N-TRIPLE");
            inDe.close();
            
        System.out.println("started loading NL infered ");
            nlModel = ModelFactory.createDefaultModel();
            InputStream inNl = FileManager.get().open( nl_inferred_mappings );
            nlModel.read(inNl, null, "N-TRIPLE");
        System.out.println("end ...");
            inNl.close();
            
            
        } catch (IOException ex) {
            Logger.getLogger(TypeMapper.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * The function returns a DBpediaOntology type for a given DBpedia instance resource.
     * E.g <http://dbpedia.org/ontology/Agent> for <http://dbpedia.org/resource/Player>
    */
    public String getTypeMapping(String lang, String typeURI) {
    
        String mappedType = null;
        
        switch(lang) {
        
            case "en":
                
                StmtIterator iterEn = enModel.listStatements( new SimpleSelector(enModel.createResource(typeURI), RDFS.subClassOf,  (RDFNode)null));
                while(iterEn.hasNext()) {
                    Statement stm = iterEn.next();
                    mappedType = stm.getObject().asResource().getURI();
                }
                break;
            
            case "de": 
                StmtIterator iterDe = deModel.listStatements( new SimpleSelector(deModel.createResource(typeURI), RDFS.subClassOf,  (RDFNode)null));
                while(iterDe.hasNext()) {
                    Statement stm = iterDe.next();
                    mappedType = stm.getObject().asResource().getURI();
                }
                break;
                
            case "nl": 
                StmtIterator iterNl = nlModel.listStatements( new SimpleSelector(nlModel.createResource(typeURI), RDFS.subClassOf,  (RDFNode)null));
                while(iterNl.hasNext()) {
                    Statement stm = iterNl.next();
                    mappedType = stm.getObject().asResource().getURI();
                }
                break;
        }
        return mappedType;
    }
}
