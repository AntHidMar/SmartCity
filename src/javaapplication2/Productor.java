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
import com.google.gson.*;
import jade.core.AID;

public class Productor extends Agent {
  
    MessageTemplate mt =    MessageTemplate.and(  
                                MessageTemplate.MatchOntology(ontology.getName()),
                                MessageTemplate.MatchLanguage(codec.getName())                        
                            );
    
  // The catalogue of books for sale (maps the title of a book to its price)
  private Hashtable contratos;
  // Capacidad de suministro de energía que tiene el productor
  float maxProduccion = 30000;
  // Capacidad actual de suministro
  float producionActual = 0;
  // Precio en € del KWh
  float precioKWhBase = 0;
  float precioKWh = 0.15f;

  int luminosidad = 100;    // Expresado en porcentaje
  int viento = 100;         // Expresado en porcentaje
  int caudal = 100;         // Expresado en porcentaje
  int temperatura = 100;    // Expresado en porcentaje
  int mercado = 100;        // Expresado en porcentaje
  
  private AID agenteLuminosidad;
  private AID agenteViento;
  private AID agenteCaudal;
  private AID agenteTemperatura;
  private AID agenteMercado;
  
  private String conversationId = "";
  
  // The GUI by means of which the user can add books in the catalogue
  private ProductorGui myGui;
  
  Random r = new Random();
  private int _onTick = 0;
  private static final Codec codec = new SLCodec();
  private static final Ontology ontology = Ontologia.getInstance();
  private JavaApplication2.TipoProductor tipoProductor;
  
  // Put agent initializations here
  protected void setup() {
    getContentManager().registerLanguage(codec, FIPANames.ContentLanguage.FIPA_SL);
    getContentManager().registerOntology(ontology);
        
    // Create the catalogue
    contratos = new Hashtable();
    // Create and show the GUI 
    //myGui = new ProductorGui(this);
    //myGui.show();
    
    // Guardamos los parámetros precio y máximo producido
    Object[] args = getArguments();
    if (args != null && args.length > 0) {
	maxProduccion = (float) args[0];
        precioKWhBase = (float) args[1];
        tipoProductor = (JavaApplication2.TipoProductor) args[2];
                
        // Por el momento indicamos que produzca el máximo de la planta.
        producionActual = maxProduccion;
        precioKWh = precioKWhBase;
        
        System.out.println("    productor Creado " + getName() + "   maxProducion: " + String.valueOf(producionActual) + " - Precio KWh: " + precioKWh + "   -   " + tipoProductor.getNombre() );
        
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

        // Consumidor - Behaviours
        addBehaviour(new OfrecerEnergia());

        addBehaviour(new OfertaEnergiaAceptada());

        addBehaviour(new CancelarContrato());
        
        
        // Agentes Entornos            
        addBehaviour(new TickerBehaviour(this, 5000) {
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                
            @Override
            //public void action() {
            protected void onTick() {

                try{
                    if(tipoProductor != null && tipoProductor != JavaApplication2.TipoProductor.DESCONOCIDO){
                        // Update the list of seller agents
                        DFAgentDescription template = new DFAgentDescription();
                        ServiceDescription sd = new ServiceDescription();
                        sd.setType("Agente-Entorno");        
                        template.addServices(sd);

                        DFAgentDescription[] result = DFService.search(myAgent, template); 
                        if(result != null){

                            request.clearAllReceiver();

                            for (int i = 0; i < result.length; ++i) {
                                String nom = result[i].getName().getName();
                                nom = nom.substring(0, nom.indexOf("@"));
                                if(nom.equalsIgnoreCase("Agente_Luminosidad") && tipoProductor == JavaApplication2.TipoProductor.SOLAR){
                                    agenteLuminosidad = result[i].getName();
                                    request.addReceiver(agenteLuminosidad);
                                }else if(nom.equalsIgnoreCase("Agente_Caudal") && tipoProductor == JavaApplication2.TipoProductor.HIDROELECTRICA){
                                    agenteCaudal = result[i].getName();
                                    request.addReceiver(agenteCaudal);
                                }else if(nom.equalsIgnoreCase("Agente_Temperatura") && tipoProductor == JavaApplication2.TipoProductor.GEOTERMICO){
                                    agenteTemperatura = result[i].getName();
                                    request.addReceiver(agenteTemperatura);
                                }else if(nom.equalsIgnoreCase("Agente_Viento") && tipoProductor == JavaApplication2.TipoProductor.EOLICO){
                                    agenteViento = result[i].getName();
                                    request.addReceiver(agenteViento);
                                }else if(nom.equalsIgnoreCase("Agente_Mercado") && (tipoProductor == JavaApplication2.TipoProductor.ELECTRICA || tipoProductor == JavaApplication2.TipoProductor.GAS
                                                                                        || tipoProductor == JavaApplication2.TipoProductor.PETROLEO)){
                                    agenteMercado = result[i].getName();
                                    request.addReceiver(agenteMercado);
                                }

                                //System.out.println(agentesProductores[i].getName());
                            }

                        }                        
                        request.setSender(getAID());
                        request.setLanguage(codec.getName());
                        request.setOntology(ontology.getName());

                        conversationId = "AgenteEntorno";
                        request.setConversationId(conversationId);
                        request.setReplyWith(conversationId); 
                        request.setContent("Solicitar Datos");

                        System.out.println("    PRODUCTOR send messaje REQUEST => Solicitar información a agentes Entornos.");
                        myAgent.send(request);
                    }

                } catch (FIPAException fe) {
                    fe.printStackTrace();
                } catch(Exception e) {
                    System.out.println("Error - " + e.getMessage());
                }

            }
        }); 
        
        ///////////////////////////////////////
        
        // Agentes Entorno
        // Luminosidad          
        /*addBehaviour(new TickerBehaviour(this, 30000) {
            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);

            @Override
            //public void action() {
            protected void onTick() {

                try{
                    // Update the list of seller agents
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("Agente-Entorno-Luminosidad");
                    template.addServices(sd);

                    DFAgentDescription[] result = DFService.search(myAgent, template); 
                    if(result != null && result.length > 0) agenteLuminosidad = result[0].getName();
                    request.clearAllReceiver();
                    request.addReceiver(agenteLuminosidad);
                    request.setSender(getAID());
                    request.setLanguage(codec.getName());
                    request.setOntology(ontology.getName());

                    conversationId = "AgenteEntorno-Luminosidad";
                    request.setConversationId(conversationId);
                    request.setReplyWith(conversationId); 
                    request.setContent("Solicitar Luz");

                    System.out.println("    PRODUCTOR send messaje REQUEST => Solicitar porcetaje de luminosidad actual.");
                    myAgent.send(request);

                } catch (FIPAException fe) {
                    fe.printStackTrace();
                } catch(Exception e) {
                    System.out.println("Error - " + e.getMessage());
                }

            }
        }); 
        */
        addBehaviour(new EscucharLuminosidad());
        
        ///////////////////////////////////////
        
        
        // Add the TickerBehaviour (period 1 sec)
        /*addBehaviour(new TickerBehaviour(this, 10000) {
          protected void onTick() {
              
            precioKWh = (float)(r.nextInt(1000)+1)/1000;   // Precio que cuesta producir
            _onTick++;
            System.out.println(String.valueOf(_onTick) + " - Productor " + getAID().getName() + "  -  producionActual : " + producionActual + "  -  precio KWh : " + precioKWh + "   -   " + tipoProductor.getNombre() );
          } 
        });*/
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

    /////////////////////////////////////
    // Consumidor - Funciones que interactuan con Consumidor
    private class OfrecerEnergia extends CyclicBehaviour {
        public void action() {
            try{
                MessageTemplate.MatchOntology(ontology.getName());
                MessageTemplate.MatchLanguage(codec.getName());
                mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                
                               
                ACLMessage msg = myAgent.receive(mt);
                
                if (msg != null){
                    if(msg.getPerformative() == ACLMessage.CFP) {

                        System.out.println("    " + getName() + " | Precio : " + precioKWh + " | MaxProducción : " + producionActual + "   -   " + tipoProductor.getNombre() + " - Contratos : " + contratos);
                        
                        ACLMessage reply = msg.createReply();
                        Agente_Predicado cp = (Agente_Predicado)getContentManager().extractContent(msg); 

                        if(cp != null){
                            float prodTemp = producionActual - cp.getPotenciaNecesitada();
                            if(prodTemp > 0){
                                reply.setPerformative(ACLMessage.PROPOSE);
                                reply.setContent(String.valueOf(precioKWh));
                            }else{
                                reply.setPerformative(ACLMessage.FAILURE);
                                reply.setContent("not-capacity-production");
                            }
                        }else{
                            reply.setPerformative(ACLMessage.FAILURE);
                            reply.setContent("error-cast-ConsumidorPredicado");
                        }
                        //System.out.println("Send Reply CFP - (" + reply.getPerformative() + ")");
                        myAgent.send(reply);
                    }else{
                        //System.out.println("    Block - Productor - Received msg.getPerformative() != CFP");
                        block();
                    }
                } else {
                    //System.out.println("    Block - Productor - msg == NULL");
                    block();
                }
            }catch(Exception e){
                System.out.println(e.getMessage());
            }
            
        }
    }
    
    private class OfertaEnergiaAceptada extends CyclicBehaviour {
        public void action() {
            try{
                mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
                ACLMessage msg = myAgent.receive(mt);
                ContentElement ce = null;             
                if (msg != null){
                    if(msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                      
                        ce = getContentManager().extractContent(msg);      
                        ACLMessage reply = msg.createReply(); 

                        if (ce instanceof Agente_Predicado){

                            Agente_Predicado cp = (Agente_Predicado) ce;
                            float maxProd_Temp = producionActual - cp.getPotenciaNecesitada();
                            if(maxProd_Temp > 0){
                                /*Consumidor_Predicado c = new Consumidor_Predicado();
                                c.setName(msg.getSender().getName());
                                c.setPotenciaNecesitada(cp.getPotenciaNecesitada());
                                c.setPrecio(precioKWh);*/
                                contratos.put(msg.getSender().getName(), cp);
                                producionActual = maxProd_Temp;
                                System.out.println("    " + getName() + " - producionActual : " + producionActual + " PUT contrato : " + contratos);
                                reply.setPerformative(ACLMessage.INFORM);
                                //System.out.println("    Productor - Capacidad : " + producionActual + " ; Vendido " + cp.getPotenciaNecesitada() + "KWh a "+msg.getSender().getName() + " por el precio de " + cp.getPrecio() + " - HASHCODE : " + msg.hashCode());
                            }else {
                                // The requested book has been sold to another buyer in the meanwhile .
                                reply.setPerformative(ACLMessage.FAILURE);
                                reply.setContent("maxProd_Temp-Menor_0");
                            }
                        }else {
                            // The requested book has been sold to another buyer in the meanwhile .
                            reply.setPerformative(ACLMessage.FAILURE);
                            reply.setContent("waited-objet-instanceof-Consumidor_Predicado");
                        }
                        //Thread.sleep(1000);
                        myAgent.send(reply);
                    } else {
                        block();
                    }
                } else {
                    //System.out.println("Received message ACCEPT_PROPOSAL == NULL");
                    block();
                }
            }catch(Exception e){
                System.out.println("Error " + e.getMessage());
            }
            
        }
    }
        
    private class CancelarContrato extends CyclicBehaviour {
        public void action() {
            try{
                mt = MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
                ACLMessage msg = myAgent.receive(mt);
                ContentElement ce = null;             
                if (msg != null){
                    if(msg.getPerformative() == ACLMessage.CANCEL) {
                      
                        ACLMessage reply = msg.createReply(); 

                        Agente_Predicado cp = (Agente_Predicado)contratos.get(msg.getSender().getName());
                        if(cp != null)
                        {
                            producionActual = producionActual + cp.getPotenciaNecesitada();
                                    
                            contratos.remove(msg.getSender().getName());
                            reply.setPerformative(ACLMessage.INFORM);
                            System.out.println("    " + getName() + " - Contrato cancelado. producionActual : " + producionActual + " - (" + msg.getSender().getName() + ") - Contratos : " + contratos);                                                
                            myAgent.send(reply);
                        }
                    } else {
                        block();
                    }
                } else {
                    //System.out.println("Received message ACCEPT_PROPOSAL == NULL");
                    block();
                }
            }catch(Exception e){
                System.out.println("Error " + e.getMessage());
            }
            
        }
    }
    // FIN Consumidor
    
    //////////////////////////////////////////////////////////////
    // Entorno
    
    
    // Consumidor - Funciones que interactuan con Consumidor
    private class EscucharLuminosidad extends CyclicBehaviour {
        public void action() {
            try{
                MessageTemplate.MatchOntology(ontology.getName());
                MessageTemplate.MatchLanguage(codec.getName());                
                mt = MessageTemplate.MatchConversationId("AgenteEntorno");
                mt = MessageTemplate.MatchPerformative(ACLMessage.AGREE);
                                               
                ACLMessage msg = myAgent.receive(mt);
                
                if (msg != null){
                    
                    if(msg.getPerformative() == ACLMessage.AGREE) {

                        String str_json = msg.getContent();
                        Gson gs = new Gson();
                        Agente_Predicado cp = gs.fromJson(str_json, Agente_Predicado.class);
                        JavaApplication2.TipoAgente ta = cp.getTipoAgente();
                        
                        if(cp instanceof Agente_Predicado){
                            if(tipoProductor != null && ta != null && ta != JavaApplication2.TipoAgente.DESCONOCIDO){
                                float produccion_Temp = 0, precio_Temp = 0;
                                switch(ta)
                                {
                                    case LUZ:
                                            if(tipoProductor == JavaApplication2.TipoProductor.SOLAR){                                            
                                                float luzAnt = luminosidad;
                                                luminosidad = cp.getLuz();
                                                float l = (float)((float)luminosidad/100);
                                                produccion_Temp = maxProduccion * l;
                                                precio_Temp = precioKWhBase * (2 - l);
                                                System.out.println("    Productor REQUEST LUZ           - : " + getName() + " - TipoProductor : " + tipoProductor.getNombre() + " - Luz Ant : " + luzAnt + " - Luz : " + luminosidad + " potenciaNecesitada Ant : " + producionActual + "  -  Nueva Potencia Necesitada:" + produccion_Temp + "    -    Precio Old: " + precioKWh + "  -  Precio New : " + precio_Temp );
                                                producionActual = produccion_Temp;
                                                precioKWh = precio_Temp;
                                            }
                                        break;
                                    case CAUDAL:
                                            if(tipoProductor == JavaApplication2.TipoProductor.HIDROELECTRICA){
                                                float caudalAnt = caudal;
                                                caudal = cp.getCaudal();
                                                float c = (float)((float)caudal/100);
                                                produccion_Temp = maxProduccion * c;
                                                precio_Temp = precioKWhBase * (2 - c);                                            
                                                System.out.println("    Productor REQUEST HIDROELECTRICA - : " + getName() + " - TipoProductor : " + tipoProductor.getNombre() + " - Caudal Ant : " + caudalAnt + " - Caudal : " + caudal + " potenciaNecesitada Ant : " + producionActual + "  -  Nueva Potencia Necesitada:" + produccion_Temp + "    -    Precio Old: " + precioKWh + "  -  Precio New : " + precio_Temp);
                                                producionActual = produccion_Temp;
                                                precioKWh = precio_Temp;
                                            }
                                        break;
                                    case MERCADO:
                                            if(tipoProductor == JavaApplication2.TipoProductor.ELECTRICA || tipoProductor == JavaApplication2.TipoProductor.PETROLEO){
                                                float mercadoAnt = mercado;
                                                mercado = cp.getMercado();
                                                float m = (float)((float)mercado/100);
                                                produccion_Temp = maxProduccion * m;
                                                precio_Temp = precioKWhBase * (2 - m);
                                                System.out.println("    Productor REQUEST MERCADO - : " + getName() + " - TipoProductor : " + tipoProductor.getNombre() + " - Mercado Ant : " + mercadoAnt + " - Mercado : " + mercado + " potenciaNecesitada Ant : " + producionActual + "  -  Nueva Potencia Necesitada:" + produccion_Temp  + "    -    Precio Old: " + precioKWh + "  -  Precio New : " + precio_Temp);
                                                producionActual = produccion_Temp;
                                                precioKWh = precio_Temp;
                                            }
                                        break;
                                    case TEMPERATURA:                                        
                                            if(tipoProductor == JavaApplication2.TipoProductor.GEOTERMICO){
                                                float temperaturaAnt = temperatura;
                                                temperatura = cp.getTemperatura();
                                                float t = (float)((float)temperatura/100);
                                                produccion_Temp = maxProduccion * t;
                                                precio_Temp = precioKWhBase * (2 - t);                                            
                                                System.out.println("    Productor REQUEST TEMPERATURA - : " + getName() + " - TipoProductor : " + tipoProductor.getNombre() + " - Temperatura Ant : " + temperaturaAnt + " - Temperatura : " + temperatura + " potenciaNecesitada Ant : " + producionActual + "  -  Nueva Potencia Necesitada:" + produccion_Temp  + "    -    Precio Old: " + precioKWh + "  -  Precio New : " + precio_Temp);
                                                producionActual = produccion_Temp;
                                                precioKWh = precio_Temp;
                                            }                                            
                                        break;
                                    case VIENTO:
                                            if(tipoProductor == JavaApplication2.TipoProductor.EOLICO){
                                                float vientoAnt = viento;
                                                viento = cp.getViento();
                                                float v = (float)((float)viento/100);
                                                produccion_Temp = maxProduccion * v;
                                                precio_Temp = precioKWhBase * (2 - v);                                            
                                                System.out.println("    Productor REQUEST VIENTO - : " + getName() + " - TipoProductor : " + tipoProductor.getNombre() + " - Viento Ant : " + vientoAnt + " - Viento : " + viento + " potenciaNecesitada Ant : " + producionActual + "  -  Nueva Potencia Necesitada:" + produccion_Temp  + "    -    Precio Old: " + precioKWh + "  -  Precio New : " + precio_Temp);
                                                producionActual = produccion_Temp;
                                                precioKWh = precio_Temp;
                                            }
                                        break;
                                }
                            }
                        }
                        
                        /*
                        String str_luz = msg.getContent();
                        
                        if(str_luz != null && str_luz.trim() != ""){
                            
                            //System.out.println("    Productor - Recibido cambio de luz ant:" + luminosidad + "  -  nueva: " + str_luz);                            
                            luminosidad = Integer.parseInt(str_luz);
                            float l = (float)(Float.parseFloat(str_luz)/100);
                            float produccionActual_Temp = maxProduccion * l;
                            System.out.println("    Productor - Producción Anterior : " + producionActual + "  -  Produccion Actual :" + produccionActual_Temp );
                            producionActual = produccionActual_Temp;
                        }                        
                        */
                    }else{
                        //System.out.println("    Block - Productor - Received msg.getPerformative() != CFP");
                        block();
                    }
                } else {
                    //System.out.println("    Block - Productor - msg == NULL");
                    block();
                }
            }catch(Exception e){
                System.out.println(e.getMessage());
            }
            
        }
    }
    // FIN Entorno
    
    
    
    
    
} 
