/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javaapplication2;

import jade.content.onto.*;
import jade.content.schema.*;
import java.util.*;

/**
   Javadoc documentation for the file EmploymentOntology
   @author Giovanni Caire - CSELT S.p.A.
   @version $Date: 2002/07/31 15:27:34 $ $Revision: 2.5 $
*/

public class Ontologia extends Ontology {

  /**
    A symbolic constant, containing the name of this ontology.
   */
  public static final String NAME = "consumidor-ontology";

  // VOCABULARY
  public static final String CONSUMIDOR_PREDICADO = "consumidor_Predicado";
  public static final String PRECIO = "precio";
  public static final String POTENCIANECESITADA = "potenciaNecesitada";
  
  
  private static Ontology theInstance = new Ontologia();
	
  /**
     This method grants access to the unique instance of the
     ontology.
     @return An <code>Ontology</code> object, containing the concepts
     of the ontology.
  */
   public static Ontology getInstance() {
		return theInstance;
   }
	
  /**
   * Constructor
   */
  private Ontologia() {
    //__CLDC_UNSUPPORTED__BEGIN
  	super(NAME, BasicOntology.getInstance());


    try {
        add(new PredicateSchema(CONSUMIDOR_PREDICADO), Consumidor_Predicado.class);
		//add(new ConceptSchema(ADDRESS), Address.class);
		//add(new ConceptSchema(PERSON), Person.class);
		//add(new ConceptSchema(COMPANY), Company.class);
		//add(new PredicateSchema(WORKS_FOR), WorksFor.class);
		//add(new PredicateSchema(PERSON_TOO_OLD), PersonTooOld.class);
		//add(new PredicateSchema(ENGAGEMENT_ERROR), EngagementError.class);
		//add(new AgentActionSchema(ENGAGE), Engage.class);
		
    	/*ConceptSchema cs = (ConceptSchema)getSchema(ADDRESS);
		cs.add(ADDRESS_NAME, (PrimitiveSchema)getSchema(BasicOntology.STRING));
		cs.add(ADDRESS_NUMBER, (PrimitiveSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
		cs.add(ADDRESS_CITY, (PrimitiveSchema)getSchema(BasicOntology.STRING), ObjectSchema.OPTIONAL);
    	*/
        PredicateSchema cs = (PredicateSchema)getSchema(CONSUMIDOR_PREDICADO);
        cs.add(POTENCIANECESITADA, (PrimitiveSchema)getSchema(BasicOntology.FLOAT), ObjectSchema.OPTIONAL);
        cs.add(PRECIO, (PrimitiveSchema)getSchema(BasicOntology.FLOAT), ObjectSchema.OPTIONAL);
         /*       
    	cs = (ConceptSchema)getSchema(PERSON);
    	cs.add(PERSON_NAME, (PrimitiveSchema)getSchema(BasicOntology.STRING));
    	cs.add(PERSON_AGE, (PrimitiveSchema)getSchema(BasicOntology.INTEGER), ObjectSchema.OPTIONAL);
    	cs.add(PERSON_ADDRESS, (ConceptSchema)getSchema(ADDRESS), ObjectSchema.OPTIONAL);
    	
    	cs = (ConceptSchema)getSchema(COMPANY);
    	cs.add(COMPANY_NAME, (PrimitiveSchema)getSchema(BasicOntology.STRING));
    	cs.add(COMPANY_ADDRESS, (ConceptSchema)getSchema(ADDRESS), ObjectSchema.OPTIONAL);
    	
    	PredicateSchema ps = (PredicateSchema)getSchema(WORKS_FOR);
    	ps.add(WORKS_FOR_PERSON, (ConceptSchema)getSchema(PERSON));
    	ps.add(WORKS_FOR_COMPANY, (ConceptSchema)getSchema(COMPANY));
    	
		AgentActionSchema as = (AgentActionSchema)getSchema(ENGAGE);
		as.add(ENGAGE_PERSON, (ConceptSchema)getSchema(PERSON));
		as.add(ENGAGE_COMPANY, (ConceptSchema)getSchema(COMPANY)); */	
    }
    catch(OntologyException oe) {
      oe.printStackTrace();
    }
  } 
}
