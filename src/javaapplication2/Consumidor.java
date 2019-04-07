/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javaapplication2;

import com.google.gson.Gson;
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
import java.util.Properties;
import jdk.nashorn.internal.ir.RuntimeNode;
public class Consumidor extends Agent {
    private int ciclo = 0;
    // Potencia Necesitada KWh
    private float potenciaBase = 0;
    private float potenciaNecesitada = 0;
    // Potencia actual
    private float potenciaAcumulada, gastoAcumulado = 0;
    
    // Lista de productores
    private AID[] agentesProductores;
    private AID agenteProductor;            // Contiene el AID del agente al que hemos comprado la energía.    
    private AID agenteLuminosidad;
    private AID agenteTemperatura;
    private float precio = 0;               // Contiene el mejor precio conseguido de los productores.
    // Create the catalogue de productores
    private static final Codec codec = new SLCodec();
    private static final Ontology ontology = Ontologia.getInstance();
    private MessageTemplate mt_CFD;
    private MessageTemplate mt_ACCEPT_PROPOSAL;
    private MessageTemplate mt = MessageTemplate.and ( MessageTemplate.MatchLanguage(codec.getName()), MessageTemplate.MatchOntology(ontology.getName()) ); 
    private String conversationId = "";
    private int repliesCnt = 0;
    private boolean cambioPrecio = false;
    private int luminosidad = 100;      // Expresado en porcentaje
    private int viento = 100;           // Expresado en porcentaje
    private int caudal = 100;           // Expresado en porcentaje
    private int temperatura = 100;      // Expresado en porcentaje
    private int mercado = 100;          // Expresado en porcentaje
  
    // Put agent initializations here
    protected void setup() {
                
        getContentManager().registerLanguage(codec, FIPANames.ContentLanguage.FIPA_SL);
        getContentManager().registerOntology(ontology);
        // Printout a welcome message
        System.out.println("Hola! Soy el agente Consumidor "+getAID().getName()+" y estoy preparado.");

        // Get the title of the book to buy as a start-up argument
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
	    potenciaBase = (float) args[0];
	    System.out.println("Potencia Base Inicial " + potenciaBase);
            potenciaNecesitada = potenciaBase;
	
            // CFP
            //addBehaviour(new CyclicBehaviour() {
            addBehaviour(new TickerBehaviour(this, 3000) {
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                private Agente_Predicado conPred;
                
                @Override
                //public void action() {
                protected void onTick() {
                
                    try{
                        ciclo++;
                        // Update the list of seller agents
                        DFAgentDescription template = new DFAgentDescription();
                        ServiceDescription sd = new ServiceDescription();
                        sd.setType("Productor-produciendo");
                        template.addServices(sd);
                        
                        DFAgentDescription[] result = DFService.search(myAgent, template); 
                        //System.out.println("Encontrados los siguientes productores:");
                        agentesProductores = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            agentesProductores[i] = result[i].getName();
                            //System.out.println(agentesProductores[i].getName());
                        }

                        
                        
                        cfp.clearAllReceiver();
                        for (int i = 0; i < agentesProductores.length; ++i) {                            
                            //System.out.println("cfp.addReceiver("+agentesProductores[i]);
                            cfp.addReceiver(agentesProductores[i]);
                        } 
                        cfp.setSender(getAID());
                        cfp.setLanguage(codec.getName());
                        cfp.setOntology(ontology.getName());

                        conversationId = "Energia-intercambio_" + getName() + "_" + System.currentTimeMillis();
                        cfp.setConversationId(conversationId);
                        cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                        //System.out.println("ciclos == 1 - ciclos : " + ciclos);
                        //mt_CFD = MessageTemplate.and(MessageTemplate.MatchConversationId("Energia-intercambio"), MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));

                        conPred = new Agente_Predicado();
                        conPred.setName(getAID().getName());
                        conPred.setPotenciaNecesitada(potenciaNecesitada);
                        conPred.setPrecio(precio);
                        conPred.setOwner(getAID());
                        conPred.setCiclo(ciclo);
                        try{
                            getContentManager().fillContent(cfp, conPred);
                        }catch(Exception e){
                            System.out.println("ERROR - " + e.getMessage());
                        }

                        System.out.println();                        
                        System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                        System.out.println("    CICLO : " + ciclo);
                        System.out.println("    Precio : " + precio + "    -    Productor : " + (agenteProductor == null ? "":agenteProductor.getName()));
                        System.out.println("Step 0 => CONSUMIDOR send messaje CFP (" + cfp.getPerformative() +  ") HashCode: " + cfp.hashCode());
                        //System.out.println("CFP => " + cfp);
                        repliesCnt = 0;
                        cambioPrecio = false;
                        myAgent.send(cfp);
                    
                        //Thread.sleep(5000);
                        
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    } catch(Exception e) {
                        System.out.println("Error - " + e.getMessage());
                    }
                    
                }
            });            
            
            // REPLY Received
            addBehaviour(new CyclicBehaviour(){
                
                private ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                private ACLMessage cancel;
                private Agente_Predicado conPred;
                private float precio_Temp = 0f;
                private AID productor_Temp = null;                
                
                @Override
                public void action() {
                    try{
                        // Receive all proposals/refusals from seller agents
                        ACLMessage reply = myAgent.receive(mt);
                        boolean fallo, rechazo = false;
                        //System.out.println("reply : " + reply);

                        if (reply != null) {
                            
                            if(reply.getPerformative() == ACLMessage.AGREE) {
                                
                                String str_json = reply.getContent();
                                Gson gs = new Gson();
                                Agente_Predicado cp = gs.fromJson(str_json, Agente_Predicado.class);
                                JavaApplication2.TipoAgente ta = cp.getTipoAgente();
                                
                                if(cp instanceof Agente_Predicado){
                                    if(ta != null && ta != JavaApplication2.TipoAgente.DESCONOCIDO){
                                
                                        switch(ta)
                                        {
                                            case LUZ:
                                                    //System.out.println("Consumidor - Recibido cambio de luz ant:" + luminosidad + "  -  nueva: " + str_luz);                            
                                                    luminosidad = cp.getLuz();
                                                    float l = (float)(2 - ((float)luminosidad/100));                                                        
                                                    float potenciaNecesitada_Temp = potenciaBase * l;
                                                    System.out.println("Consumidor - Potencia Necesitada Anterior : " + potenciaNecesitada + "  -  Nueva Potencia Necesitada:" + potenciaNecesitada_Temp );
                                                    potenciaNecesitada = potenciaNecesitada_Temp;
                                                break;
                                            case CAUDAL:
                                                    
                                                break;
                                            case MERCADO:
                                                break;
                                            case TEMPERATURA:
                                                    float temp = (float) cp.getTemperatura();
                                                    temp = Math.abs(temp - 24);  // Ha 24 grados es la temperatura con menos gasto de energético.
                                                    temp = 1 + (temp * 0.05f);
                                                    float potNecNEW = potenciaBase * temp;
                                                    System.out.println("Consumidor - ( " + cp.getTipoAgente().getNombre() + " |Temp: " + cp.getTemperatura() + " ) - Potencia Necesitada Anterior : " + potenciaNecesitada + "  -  Nueva Potencia Necesitada:" + potNecNEW );
                                                    potenciaNecesitada = potNecNEW;
                                                break;
                                            case VIENTO:
                                                break;
                                        }

                                    }  
                                }
                            }else{
                            
                                String convId = reply.getConversationId();
                                //System.out.println("conversationId == convId => " + conversationId + " == " + convId);
                                if(conversationId == convId){
                                    //System.out.println("reply != null - " + reply.getPerformative());
                                    switch ( reply.getPerformative() ){
                                        case ACLMessage.PROPOSE:                                    

                                            float price = Float.parseFloat(reply.getContent());

                                            if(agenteProductor != null && agenteProductor.getName() == reply.getSender().getName() && price != precio){
                                                // Ha cambiado el precio del productor al que actualmente compra el consumidor.
                                                System.out.println("Ha cambiado el precio del agente productor - Precio : " + precio + " - Agente Productor : " + agenteProductor.getName());
                                                precio_Temp = price;
                                                precio = precio_Temp;
                                            }
                                            if (precio_Temp == 0 || price < precio_Temp) {
                                                // Si mejora el precio cambiaremos el contrato
                                                cambioPrecio = true;
                                                precio_Temp = price;
                                                productor_Temp = reply.getSender();                                        
                                            }                                          

                                            break;
                                        case ACLMessage.FAILURE:
                                            if(agenteProductor != null && agenteProductor.getName() == reply.getSender().getName()){
                                                
                                                System.out.println("Consumidor - " + reply.getSender().getName() + " failure. " + reply.getContent());
                                                
                                                cancel = new ACLMessage(ACLMessage.CANCEL);
                                                cancel.setSender(getAID());
                                                cancel.setLanguage(codec.getName());
                                                cancel.setOntology(ontology.getName());
                                                cancel.addReceiver(agenteProductor);
                                                cancel.setConversationId("cancel-contract-FAILURE");
                                                System.out.println("Step Cancel Contract => " + productor_Temp.getName() + " => send messaje CANCEL by FAILURE. ");                                                                    
                                                myAgent.send(cancel);
                                                
                                                agenteProductor = null;
                                                precio = 0;
                                                
                                            }
                                            break;
                                        case ACLMessage.REFUSE:
                                            if(agenteProductor != null && agenteProductor.getName() == reply.getSender().getName()){
                                                
                                                System.out.println("Consumidor - " + reply.getSender().getName() + " rechaza propuesta. ");
                                                
                                                cancel = new ACLMessage(ACLMessage.CANCEL);
                                                cancel.setSender(getAID());
                                                cancel.setLanguage(codec.getName());
                                                cancel.setOntology(ontology.getName());
                                                cancel.addReceiver(agenteProductor);
                                                cancel.setConversationId("cancel-contract-REFUSE");
                                                System.out.println("Step Cancel Contract => " + productor_Temp.getName() + " => send messaje CANCEL by REFUSE. ");                                                                    
                                                myAgent.send(cancel);
                                                
                                                agenteProductor = null;
                                                precio = 0;
                                                
                                            }
                                            break;
                                        case ACLMessage.DISCONFIRM:

                                            Gson gson = new Gson();
                                            Agente_Predicado cp = gson.fromJson(reply.getContent(), Agente_Predicado.class);

                                            if(cp instanceof Agente_Predicado){
                                                precio = cp.getPrecio();
                                                System.out.println("Consumidor - " + reply.getSender().getName() + " - ha cambiado el precio. " + reply.getContent());
                                            }

                                            break;
                                        case ACLMessage.INFORM:
                                            System.out.println("Consumidor ACLMessage.INFORM - Contrato realizado - " + reply.getSender());
                                            break;
                                        default :
                                            block();
                                            break;
                                    }

                                    repliesCnt++;
                                    System.out.println("repliesCnt : " + repliesCnt);

                                    if (cambioPrecio && repliesCnt >= agentesProductores.length) {

                                        // Si el agente ha cambiado debemos cancelar el contrado con el productor antiguo.
                                        if(agenteProductor != null && agenteProductor != productor_Temp){
                                            cancel = new ACLMessage(ACLMessage.CANCEL);
                                            cancel.setSender(getAID());
                                            cancel.setLanguage(codec.getName());
                                            cancel.setOntology(ontology.getName());
                                            cancel.addReceiver(agenteProductor);
                                            cancel.setConversationId("cancel-contract");
                                            System.out.println("Step Cancel Contract => " + productor_Temp.getName() + " => send messaje CANCEL. ");                                                                    
                                            myAgent.send(cancel);
                                        }

                                        precio = precio_Temp;
                                        agenteProductor = productor_Temp;

                                        // ENVIAMOS MENSAJE DE PROPUESTA ACEPTADA
                                        order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                                        order.setSender(getAID());
                                        order.setLanguage(codec.getName());
                                        order.setOntology(ontology.getName());                
                                        order.addReceiver(agenteProductor);

                                        //order.setConversationId("Energia-aceptarProposal");
                                        //order.setReplyWith("order"+System.currentTimeMillis()); // Unique value
                                        //mt_ACCEPT_PROPOSAL = MessageTemplate.and(MessageTemplate.MatchConversationId("Energia-aceptarProposal"), MessageTemplate.MatchInReplyTo(order.getReplyWith()));

                                        conPred = new Agente_Predicado();
                                        conPred.setName(getAID().getName());
                                        conPred.setPotenciaNecesitada(potenciaNecesitada);
                                        conPred.setPrecio(precio);
                                        conPred.setOwner(getAID());

                                        getContentManager().fillContent(order, conPred);

                                        System.out.println("Step 2 => PROPUESTA ACEPTADA => send messaje ACCEPT_PROPOSAL. Mejor Precio : " + precio + " - Mejor Productor : " + agenteProductor.getName());                                
                                        myAgent.send(order);
                                    }

                                }else{
                                    block();
                                }
                            }
                        }else{
                            block();
                        }
                    }catch(Exception e)
                    {
                        System.out.println("Error : " + e.getMessage());
                    }                     
                }
            });
            
            // Agentes Entornos            
            addBehaviour(new TickerBehaviour(this, 5000) {
                ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                
                @Override
                //public void action() {
                protected void onTick() {
                
                    try{
                        // Update the list of seller agents
                        DFAgentDescription template = new DFAgentDescription();
                        ServiceDescription sd = new ServiceDescription();
                        sd.setType("Agente-Entorno");        
                        template.addServices(sd);
                        
                        DFAgentDescription[] result = DFService.search(myAgent, template); 
                        if(result != null){
                            for (int i = 0; i < result.length; ++i) {
                                String nom = result[i].getName().getName();
                                nom = nom.substring(0, nom.indexOf("@"));
                                if(nom.equalsIgnoreCase("Agente_Luminosidad")){
                                    agenteLuminosidad = result[i].getName();
                                }else if(nom.equalsIgnoreCase("Agente_Temperatura")){
                                    agenteTemperatura = result[i].getName();
                                }
                                    
                                //System.out.println(agentesProductores[i].getName());
                            }
                            request.clearAllReceiver();
                            request.addReceiver(agenteLuminosidad);
                            request.addReceiver(agenteTemperatura);
                        }                        
                        request.setSender(getAID());
                        request.setLanguage(codec.getName());
                        request.setOntology(ontology.getName());

                        conversationId = "AgenteEntorno";
                        request.setConversationId(conversationId);
                        request.setReplyWith(conversationId); 
                        request.setContent("Solicitar Datos");
                        
                        //System.out.println("CONSUMIDOR send messaje REQUEST => Solicitar porcetaje de luminosidad actual.");
                        //myAgent.send(request);
                        
                    } catch (FIPAException fe) {
                        fe.printStackTrace();
                    } catch(Exception e) {
                        System.out.println("Error - " + e.getMessage());
                    }
                    
                }
            }); 
            
            //addBehaviour(new EscucharLuminosidad());
        }
        else {
          // Make the agent terminate
          System.out.println("No se han pasado parámetros");
          doDelete();
        }
    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("Buyer-agent "+getAID().getName()+" terminating.");
    }
    
    // Consumidor - Funciones que interactuan con Consumidor
    private class EscucharLuminosidad extends CyclicBehaviour {
        public void action() {
            try{
                MessageTemplate.MatchOntology(ontology.getName());
                MessageTemplate.MatchLanguage(codec.getName());
                mt = MessageTemplate.MatchPerformative(ACLMessage.AGREE);
                mt = MessageTemplate.MatchConversationId("AgenteEntorno-Luminosidad");
                                               
                ACLMessage msg = myAgent.receive(mt);
                
                if (msg != null){
                    
                } else {
                    //System.out.println("    Block - Productor - msg == NULL");
                    block();
                }
            }catch(Exception e){
                System.out.println(e.getMessage());
            }
            
        }
    }
}
