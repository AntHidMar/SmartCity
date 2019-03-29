/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javaapplication2;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.ContentElement;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPANames;
import java.text.SimpleDateFormat;

import java.util.*;

public class Productor extends Agent {
    MessageTemplate mt = MessageTemplate.and ( MessageTemplate.MatchLanguage(codec.getName()), MessageTemplate.MatchOntology(ontology.getName()) ); 
  // The catalogue of books for sale (maps the title of a book to its price)
  private Hashtable catalogue;
  // Capacidad de suministro de energía que tiene el productor
  float maxProducido = 30000;
  // Capacidad actual de suministro
  float producido = 0;
  // Precio en € del KWh
  float precioKWh = 0.15f;

  // The GUI by means of which the user can add books in the catalogue
  private ProductorGui myGui;
  
  Random r = new Random();
  private int _onTick = 0;
  private static final Codec codec = new SLCodec();
  private static final Ontology ontology = Ontologia.getInstance();

  // Put agent initializations here
  protected void setup() {
    getContentManager().registerLanguage(codec, FIPANames.ContentLanguage.FIPA_SL);
    getContentManager().registerOntology(ontology);
        
    // Create the catalogue
    catalogue = new Hashtable();
    // Create and show the GUI 
    //myGui = new ProductorGui(this);
    //myGui.show();

    // Guardamos los parámetros precio y máximo producido
    Object[] args = getArguments();
    if (args != null && args.length > 0) {
	maxProducido = (float) args[0];
        precioKWh = (float) args[1];
    
        System.out.println("Creado productor " + getName() + "   maxProducion: " + String.valueOf(maxProducido) + " - Precio KWh: " + precioKWh);
        
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Productor-produciendo");
        sd.setName("JADE-energia-intercambio");
        dfd.addServices(sd);
        try {
          DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
          fe.printStackTrace();
        }

        // Add the behaviour serving queries from buyer agents
        addBehaviour(new OfferRequestsServer());

        // Add the behaviour serving purchase orders from buyer agents
        addBehaviour(new PurchaseOrdersServer());
        
        addBehaviour(new CancelOfferCustomer());
        
        // Add the TickerBehaviour (period 1 sec)
        addBehaviour(new TickerBehaviour(this, 50000) {
          protected void onTick() {
              
            precioKWh = (float)(r.nextInt(1000)+1)/1000;   // Precio que cuesta producir
            _onTick++;
            System.out.println(String.valueOf(_onTick) + " - Productor " + getAID().getName() + "  -  maxProducido : " + maxProducido + "  -  precio KWh : " + precioKWh);
          } 
        });
    }else{
        // Make the agent terminate
        System.out.println("No se le han pasado parámetros al productor");
        doDelete();
    }
  }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Deregister from the yellow pages
        try {
            DFService.deregister(this);
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Close the GUI
        myGui.dispose();
        // Printout a dismissal message
        
        System.out.println(" : Productor "+getAID().getName()+" terminando.");
    }

  /**
     This is invoked by the GUI when the user adds a new book for sale
   */
  public void updateCatalogue(final String title, final int price) {
    addBehaviour(new OneShotBehaviour() {
        public void action() {
            catalogue.put(title, new Integer(price));
            System.out.println(title+" inserted into catalogue. Price = "+price);
        }
    });
  }
  
    /**
       Inner class OfferRequestsServer.
       This is the behaviour used by Book-seller agents to serve incoming requests 
       for offer from buyer agents.
       If the requested book is in the local catalogue the seller agent replies 
       with a PROPOSE message specifying the price. Otherwise a REFUSE message is
       sent back.
     */
    private class OfferRequestsServer extends CyclicBehaviour {
        public void action() {
            try{
                //MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                //MessageTemplate mt = MessageTemplate.and ( MessageTemplate.MatchLanguage(codec.getName()), MessageTemplate.MatchOntology(ontology.getName()) ); 
                ACLMessage msg = myAgent.receive(mt);
                
                if (msg != null){
                    if(msg.getPerformative() == ACLMessage.CFP) {
                        ContentElement ce = null;                         
                        System.out.println("CFP");
                        // CFP Message received. Process it
                        //String title = msg.getContent();
                        //String potenciaNecesitada_str = msg.getContent();

                            ce = getContentManager().extractContent(msg); 

                        ACLMessage reply = msg.createReply();

                        //Integer price = (Integer) catalogue.get(title);
                        // if (potenciaNecesitada_str != null) {
                        if (ce instanceof Consumidor_Predicado){
                            //double potenciaNecesitada = Double.valueOf(potenciaNecesitada_str);                
                                Consumidor_Predicado cp = (Consumidor_Predicado) ce;
                            if((maxProducido -  cp.getPotenciaNecesitada()) > 0){

                                // The requested book is available for sale. Reply with the price
                                reply.setPerformative(ACLMessage.PROPOSE);
                                reply.setContent(String.valueOf(precioKWh));
                                System.out.println("Productor - Respondiendo con precio del KWh : " + precioKWh);
                            } else {
                                // No produce suficiente energía para satisfacer al consumidor
                                reply.setPerformative(ACLMessage.REFUSE);
                                reply.setContent("not-energia-suficiente");
                            }
                        } else {
                            // The requested book is NOT available for sale.
                            reply.setPerformative(ACLMessage.REFUSE);
                            reply.setContent("not-available");
                        }
                        myAgent.send(reply);
                    }else{
                        System.out.println("Received message != CFP");
                        block();
                    }
                } else {
                    System.out.println("Received message CFP == NULL");
                    block();
                }
            }catch(Exception e){
                System.out.println(e.getMessage());
            }
        }
    }  // End of inner class OfferRequestsServer

    /**
       Inner class PurchaseOrdersServer.
       This is the behaviour used by Book-seller agents to serve incoming 
       offer acceptances (i.e. purchase orders) from buyer agents.
       The seller agent removes the purchased book from its catalogue 
       and replies with an INFORM message to notify the buyer that the
       purchase has been sucesfully completed.
     */
    private class PurchaseOrdersServer extends CyclicBehaviour {
        public void action() {
            //MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            //ACLMessage msg = myAgent.receive(mt);
            //MessageTemplate mt = MessageTemplate.and ( MessageTemplate.MatchLanguage(codec.getName()), MessageTemplate.MatchOntology(ontology.getName()) ); 
            ACLMessage msg = myAgent.receive(mt);
            ContentElement ce = null;             
            if (msg != null && msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                System.out.println("Productor - Recibido mensaje ACCEPT_PROPOSAL");
                // ACCEPT_PROPOSAL Message received. Process it
                //String title = msg.getContent();
                //ACLMessage reply = msg.createReply();

                try{
                    ce = getContentManager().extractContent(msg); 
                }catch(Exception e){
                    System.out.println(e.getMessage());
                }
                ACLMessage reply = msg.createReply();

                //Integer price = (Integer) catalogue.get(title);
                // if (potenciaNecesitada_str != null) {
                if (ce instanceof Consumidor_Predicado){
                    //double potenciaNecesitada = Double.valueOf(potenciaNecesitada_str);                
                    Consumidor_Predicado cp = (Consumidor_Predicado) ce;
                
                    //String potenciaNecesitada_str = msg.getContent();
                    //float potenciaNecesitada = Float.valueOf(potenciaNecesitada_str);  
                    
                    if((maxProducido - cp.getPotenciaNecesitada()) > 0){
                        /*Consumidor_Predicado c = new Consumidor_Predicado();
                        c.setName(msg.getSender().getName());
                        c.setPotenciaNecesitada(cp.getPotenciaNecesitada());
                        c.setPrecio(precioKWh);*/
                        catalogue.put(msg.getSender().getName(), cp);
                        maxProducido = maxProducido - cp.getPotenciaNecesitada();
                        reply.setPerformative(ACLMessage.INFORM);
                        System.out.println("Vendido " + cp.getPotenciaNecesitada() + "KWh a "+msg.getSender().getName() + " por el precio de " + cp.getPrecio());
                    }else {
                        // The requested book has been sold to another buyer in the meanwhile .
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("not-available");
                    }

                    /*Integer price = (Integer) catalogue.remove(title);
                    if (price != null) {
                        reply.setPerformative(ACLMessage.INFORM);
                        System.out.println(title+" sold to agent "+msg.getSender().getName());
                    } else {
                        // The requested book has been sold to another buyer in the meanwhile .
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("not-available");
                    }*/
                    
                } else {
                        // The requested book has been sold to another buyer in the meanwhile .
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("waited-objet-instanceof-Consumidor_Predicado");
                    }
                myAgent.send(reply);
            } else {
                //System.out.println("Received message ACCEPT_PROPOSAL == NULL");
                block();
            }
        }
    }  // End of inner class OfferRequestsServer
    
    private class CancelOfferCustomer extends CyclicBehaviour {
        public void action() {
            //MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
            //ACLMessage msg = myAgent.receive(mt);
            //MessageTemplate mt = MessageTemplate.and ( MessageTemplate.MatchLanguage(codec.getName()), MessageTemplate.MatchOntology(ontology.getName()) ); 
            ACLMessage msg = myAgent.receive(mt);
            ContentElement ce = null;           
            
            if (msg != null && msg.getPerformative() == ACLMessage.CANCEL) {
                System.out.println("Productor - Recibido mensaje CANCEL");
                //ACLMessage reply = msg.createReply();
                //String cancelar = msg.getContent();                
                //if(cancelar != null){
                  
                try{
                    ce = getContentManager().extractContent(msg); 
                }catch(Exception e){
                    System.out.println(e.getMessage());
                }
                ACLMessage reply = msg.createReply();

                //Integer price = (Integer) catalogue.get(title);
                // if (potenciaNecesitada_str != null) {
                if (ce instanceof Consumidor_Predicado){
                    //double potenciaNecesitada = Double.valueOf(potenciaNecesitada_str);                
                    Consumidor_Predicado cp = (Consumidor_Predicado) ce;
                
                    Consumidor_Predicado conPred = (Consumidor_Predicado) catalogue.get(msg.getSender().getName());
                    if (conPred != null && cp != null) {
                        
                        if(conPred.getPotenciaNecesitada() == cp.getPotenciaNecesitada() || conPred.getPrecio() == cp.getPrecio()){
                            maxProducido = maxProducido + conPred.getPotenciaNecesitada();
                            if(catalogue.remove(msg.getSender().getName()) != null){

                                reply.setPerformative(ACLMessage.CANCEL);
                                System.out.println(" Cancelado Contrato con agente " + msg.getSender().getName());
                            }else{
                                reply.setPerformative(ACLMessage.FAILURE);
                                reply.setContent("error-remove-agent");
                            }
                        } else {
                            // The requested book has been sold to another buyer in the meanwhile .
                            reply.setPerformative(ACLMessage.FAILURE);
                            reply.setContent("not-match-data");
                        }
                    } else {
                        // The requested book has been sold to another buyer in the meanwhile .
                        reply.setPerformative(ACLMessage.FAILURE);
                        reply.setContent("not-exist-contract");
                    }
                    
                }else{
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-Cancel");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }
} 
