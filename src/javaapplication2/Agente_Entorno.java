package javaapplication2;


import com.google.gson.Gson;
import jade.core.Agent;
import java.util.Random;

import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.ContentElement;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author kamha
 */
public class Agente_Entorno extends Agent {
    
    private JavaApplication2.TipoAgente tipoAgente;
    private int luz = 50;  // Expresado en tanto por cientos.
    int viento = 50;         // Expresado en porcentaje
    int caudal = 50;         // Expresado en porcentaje
    int temperatura = 24;    // Expresado en porcentaje
    int mercado = 50;        // Expresado en porcentaje
    private int ciclos = 0;
    private Random r = new Random();
    private static final Codec codec = new SLCodec();
    private static final Ontology ontology = Ontologia.getInstance();  
    private MessageTemplate mt =    MessageTemplate.and(  
                                MessageTemplate.MatchOntology(ontology.getName()),
                                MessageTemplate.MatchLanguage(codec.getName())                        
                            );
    
    protected void setup() {
        
        
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            luz = (int)args[0];
            tipoAgente = (JavaApplication2.TipoAgente)args[1];
        }
        
        // Register the book-selling service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("Agente-Entorno");
        sd.setName("JADE-energia-intercambio");
        dfd.addServices(sd);
        try {
          DFService.register(this, dfd);
        }
        catch (FIPAException fe) {
          fe.printStackTrace();
        }
        
        // Add the TickerBehaviour (period 1 sec)
        addBehaviour(new TickerBehaviour(this, 20000) {
          protected void onTick() {
              
            int porcL = r.nextInt(10);
            int porcM = r.nextInt(10);
            int porcC = r.nextInt(10);
            int porcV = r.nextInt(10);
            int sumORes = r.nextInt(2) + 1;
            
            if(sumORes == 1){
                // Restamos
                if((luz - porcL) < 0) luz = 0;
                    else luz = luz - porcL;
                if((mercado - porcM) < 0) mercado = 0;
                    else mercado = mercado - porcM;
                if((caudal - porcC) < 0) caudal = 0;
                    else caudal = caudal - porcC;
                if((viento - porcV) < 0) viento = 0;
                    else viento = viento - porcV;
                temperatura++;
                if(temperatura > 52)    temperatura = 52;
                         
            }else{
                if((luz + porcL) > 100) luz = 100;
                    else luz = luz + porcL;
                if((mercado - porcM) > 100) mercado = 100;
                    else mercado = mercado + porcM;
                if((caudal - porcC) > 100) caudal = 100;
                    else caudal = caudal + porcC;
                if((viento - porcV) > 100) viento = 100;
                    else viento = viento + porcV;
                temperatura--;
                if(temperatura < - 20)  temperatura = -20;
            }
            ciclos++;
            //System.out.println("        Agente Luz " + getAID() + "  ha realizado un ciclo de comprobación - LUZ : " + luz + "%" );
          } 
        });
        
        // Add the behaviour serving queries from buyer agents
        addBehaviour(new OfrecerInformacion());

    }
    
    // Consumidor - Funciones que interactuan con Consumidor
    private class OfrecerInformacion extends CyclicBehaviour {
        public void action() {
            try{
                if(tipoAgente != null && tipoAgente != JavaApplication2.TipoAgente.DESCONOCIDO){
                        
                    MessageTemplate.MatchOntology(ontology.getName());
                    MessageTemplate.MatchLanguage(codec.getName());
                    mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                    mt = MessageTemplate.MatchConversationId("AgenteEntorno");

                    ACLMessage msg = myAgent.receive(mt);

                    if (msg != null){
                        if(msg.getPerformative() == ACLMessage.REQUEST) {

                            ACLMessage reply = msg.createReply();                        
                            reply.setPerformative(ACLMessage.AGREE);
                            Agente_Predicado ap = new Agente_Predicado();   
                            ap.setTipoAgente(tipoAgente);
                            String res = "- Error -";
                            switch(tipoAgente){
                                case CAUDAL :       ap.setCaudal(caudal);               res = String.valueOf(caudal);
                                    break;
                                case LUZ :          ap.setLuz(luz);                     res = String.valueOf(luz);
                                    break;
                                case MERCADO :      ap.setMercado(mercado);             res = String.valueOf(mercado);
                                    break;
                                case TEMPERATURA :  ap.setTemperatura(temperatura);     res = String.valueOf(temperatura);
                                    break;
                                case VIENTO :       ap.setViento(viento);               res = String.valueOf(viento);
                                    break;
                            }


                            Gson gson = new Gson();
                            String str_json = gson.toJson(ap);

                            reply.setContent(str_json);

                            System.out.println("        Agente Entorno - " + getAID().getName() + " informando a " + msg.getSender().getName() + "   de " + tipoAgente.getNombre() + " : " + res);
                            myAgent.send(reply);

                        }else{
                            //System.out.println("    Block - Productor - Received msg.getPerformative() != CFP");
                            block();
                        }
                    } else {
                        //System.out.println("    Block - Productor - msg == NULL");
                        block();
                    }
                }
            }catch(Exception e){
                System.out.println(e.getMessage());
            }
            
        }
    }
    
}
