/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javaapplication2;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPANames;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import java.util.Hashtable;
import java.util.Random;
import jade.content.lang.Codec;
import jade.content.onto.*;

import jade.content.*;
import jade.content.lang.sl.SLCodec;
import java.lang.invoke.MethodHandles;
public class Consumidor extends Agent {
    int ciclos = 0;
    // Potencia Necesitada KWh
    float potenciaNecesitada = 0;
    // Potencia actual
    float potenciaAcumulada, gastoAcumulado = 0;
    float precio = 0;
    // The title of the book to buy
    private String targetBookTitle;
    // The list of known seller agents
    private AID[] agentesProductores;
    private AID agenteProductor;    // Contiene el AID del agente al que hemos comprado la energía.    
    // Create the catalogue de productores
    private static final Codec codec = new SLCodec();
    private static final Ontology ontology = Ontologia.getInstance();

    // Put agent initializations here
    protected void setup() {
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontology);
        // Printout a welcome message
        System.out.println("Hello! Agente Consumidor "+getAID().getName()+" is ready.");

        // Get the title of the book to buy as a start-up argument
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
	    potenciaNecesitada = (float) args[0];
	    System.out.println("Potencia Necesitada Inicial "+potenciaNecesitada);
	
	    // Add a TickerBehaviour that schedules a request to seller agents every minute
	    addBehaviour(new TickerBehaviour(this, 5000) {
                protected void onTick() {
                    
                    System.out.println("");
                    System.out.println("Ciclo: " + ciclos + " - Intentando comprar energía. Energía necesitada "+String.valueOf(potenciaNecesitada));
                    System.out.println("Potencia Necesitada : " + potenciaNecesitada + "  -  Precio : " + precio);
                    System.out.println("Actual Productor : " + agenteProductor);
                    ciclos++;
                    
                    // Update the list of seller agents
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("Productor-produciendo");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template); 
                        //System.out.println("Encontrados los siguientes productores:");
                        agentesProductores = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            agentesProductores[i] = result[i].getName();
                            //System.out.println(agentesProductores[i].getName());
                        }
                        
                    }
                    catch (FIPAException fe) {
                      fe.printStackTrace();
                    }
                      
                
                    // Perform the request
                    myAgent.addBehaviour(new RequestPerformer());
                }
	    });
        }
        else {
          // Make the agent terminate
          System.out.println("No target book title specified");
          doDelete();
        }
    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("Buyer-agent "+getAID().getName()+" terminating.");
    }
  
    
    /**
       Inner class RequestPerformer.
       This is the behaviour used by Book-buyer agents to request seller 
       agents the target book.
     */
    private class RequestPerformer extends Behaviour {
        private AID mejorProductor; // The agent who provides the best offer 
        private float mejorPrecio;  // The best offered price
        private int repliesCnt = 0; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;
        private Consumidor_Predicado conPred;
        
        public void action() {
	    switch (step) {            
	    case 0:
                // Send the cfp to all sellers
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                for (int i = 0; i < agentesProductores.length; ++i) {
                    System.out.println("cfp.addReceiver("+agentesProductores[i]);
                    cfp.addReceiver(agentesProductores[i]);
                } 
                cfp.setSender(getAID());
                cfp.setLanguage(codec.getName());
                cfp.setOntology(ontology.getName());
                conPred = new Consumidor_Predicado();
                conPred.setName(getAID().getName());
                conPred.setPotenciaNecesitada(potenciaNecesitada);
                conPred.setPrecio(precio);
                conPred.setOwner(getAID());
                try{
                    getContentManager().fillContent(cfp, conPred);
                }catch(Exception e){
                    System.out.println("ERROR - " + e.getMessage());
                }
                
                //cfp.setContent(targetBookTitle);
                //cfp.setContent(String.valueOf(potenciaNecesitada));     // Indicamos la potencia necesitada
                cfp.setConversationId("Energia-intercambio");
                cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                System.out.println("Step 0 => CONSUMIDOR send messaje CFP " + cfp.getReplyWith());
                myAgent.send(cfp);
                // Prepare the template to get proposals
                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Energia-intercambio"), MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                step = 1;
	      break;
	    case 1:
                // Receive all proposals/refusals from seller agents
                ACLMessage reply = myAgent.receive(mt);
                if (reply != null) {
                    // Reply received
                    if (reply.getPerformative() == ACLMessage.PROPOSE) {
                        // This is an offer 
                        float price = Float.parseFloat(reply.getContent());
                        if (price > mejorPrecio) {
                            // This is the best offer at present
                            mejorPrecio = price;
                            mejorProductor = reply.getSender();   
                            System.out.println("Propuesta aceptada.");
                        }
                    } else {
                        block();
                    }
                    repliesCnt++;
                    
                    if (repliesCnt >= agentesProductores.length) {
                      // We received all replies
                      step = 5; 
                      System.out.println("Step 1 => Recibidas todas los precios de venta");                
                    }
                                          
                } else {
                  block();
                }
	      break;
	    case 2:
                
                // Send the purchase order to the seller that provided the best offer
                ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                order.setSender(getAID());
                order.setLanguage(codec.getName());
                order.setOntology(ontology.getName());                
                order.addReceiver(mejorProductor);
                order.setConversationId("Energia-aceptarProposal");
                order.setReplyWith("order"+System.currentTimeMillis()); // Unique value
                
                conPred = new Consumidor_Predicado();
                conPred.setName(getAID().getName());
                conPred.setPotenciaNecesitada(potenciaNecesitada);
                conPred.setPrecio(mejorPrecio);
                conPred.setOwner(getAID());
                try{
                    getContentManager().fillContent(order, conPred);
                }catch(Exception e){
                    System.out.println("ERROR - " + e.getMessage());
                }
                System.out.println("Step 2 => CONSUMIDO send messaje ACCEPT_PROPOSAL");
                myAgent.send(order);
                // Prepare the template to get proposals
                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Energia-aceptarProposal"), MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                
                
                
                /*
                ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                
                order.addReceiver(mejorProductor);
                order.setContent(String.valueOf(mejorPrecio));
                order.setConversationId("Energia-intercambio");
                order.setReplyWith("order"+System.currentTimeMillis());
                myAgent.send(order);
                // Prepare the template to get the purchase order reply
                mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Energia-intercambio"), MessageTemplate.MatchInReplyTo(order.getReplyWith()));*/
                step = 3;
	      break;
	    case 3:      
                // Receive the purchase order reply
                reply = myAgent.receive(mt);
                if (reply != null) {
                    // Purchase order reply received
                    if (reply.getPerformative() == ACLMessage.INFORM) {
                        // Purchase successful. We can terminate
                        //System.out.println(targetBookTitle+" successfully purchased from agent "+reply.getSender().getName());
                        System.out.println(" Comprados " + potenciaNecesitada + "KWh al agente "+reply.getSender().getName() + " y precio ");
                        System.out.println("Precio = "+mejorPrecio);
                        //myAgent.doDelete();
                    } else {
                        block();
                        //System.out.println("Attempt failed: requested book already sold.");
                    }	        	
                    step = 4;
                    
                } else {
                    block();
                }
	      break;
            case 5:
                if(agenteProductor != null)
                {
                    if(agenteProductor.getName() != mejorProductor.getName())
                    {
                        // Enviamos cancelación de contrato
                        ACLMessage cancel = new ACLMessage(ACLMessage.CANCEL);
                        cancel.setSender(getAID());
                        cancel.setLanguage(codec.getName());
                        cancel.setOntology(ontology.getName());                
                        cancel.addReceiver(agenteProductor);
                        cancel.setConversationId("Cancelar-contrato");
                        cancel.setReplyWith("cancel"+System.currentTimeMillis()); // Unique value
                        try{
                            getContentManager().fillContent(cancel, conPred);
                        }catch(Exception e){
                            System.out.println("ERROR - " + e.getMessage());
                        }
                        System.out.println("Step 5 => CONSUMIDOR send messaje CANCEL");
                        myAgent.send(cancel);
                        // Prepare the template to get proposals
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Cancelar-contrato"), MessageTemplate.MatchInReplyTo(cancel.getReplyWith()));

                        /*ACLMessage cancel = new ACLMessage(ACLMessage.CANCEL);
                        cancel.setLanguage(FIPANames.ContentLanguage.FIPA_SL0);
                        cancel.setOntology(Ontologia.NAME);
                        Consumidor_Predicado conPred = new Consumidor_Predicado();
                        
                        cancel.addReceiver(agenteProductor);
                        cancel.setContent(String.valueOf(precio));     // Indicamos la potencia necesitada
                        cancel.setConversationId("Cancelar-contrato");
                        conPred.setName("Con");
                        conPred.setPotenciaNecesitada(potenciaNecesitada);
                        conPred.setPrecio(precio);
                        cancel.setReplyWith("cancel"+System.currentTimeMillis()); // Unique value
                        myAgent.send(cancel);
                        // Prepare the template to get proposals
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Cancelar-contrato"), MessageTemplate.MatchInReplyTo(cancel.getReplyWith()));*/
                        step = 6;
                    }else{
                        step = 2;
                    }
                }else{
                    agenteProductor = mejorProductor;
                    precio = mejorPrecio;
                    step = 2;
                }
                break;
            case 6:
                reply = myAgent.receive(mt);
                if (reply != null) {
                    if (reply.getPerformative() == ACLMessage.CANCEL) {
                        agenteProductor = mejorProductor;
                        precio = mejorPrecio;
                        step = 2;
                    } else if (reply.getPerformative() == ACLMessage.FAILURE) {
                        step = 5;
                    }else {
                        block();
                        // System.out.println("Fallo al intentar recibir cancelación de contrato de energía.");
                    }	 
                } else {
                    block();
                }
                break;
	    }        
	  }
	
	public boolean done() {
            if (step == 2 && mejorProductor == null) {
	  	System.out.println("Attempt failed: "+targetBookTitle+" not available for sale");
            }
            boolean res = ((step == 2 && mejorProductor == null) || step == 4);
            if(res) {
                System.out.println("done CIERTO");
                //myAgent.removeBehaviour(new RequestPerformer());
            }
            
            return res;
           
	}
    }  // End of inner class RequestPerformer
}
