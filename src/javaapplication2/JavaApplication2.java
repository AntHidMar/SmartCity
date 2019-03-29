/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javaapplication2;
import jade.wrapper.ContainerController;
import jade.wrapper.AgentController;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;
import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import java.util.Random;
/**
 *
 * @author kamha
 */
public class JavaApplication2 {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
       /*
       Runtime runtime = Runtime.instance();
       Profile profile = new ProfileImpl();
       profile.setParameter(Profile.MAIN_HOST, "localhost");
       profile.setParameter(Profile.GUI, "true");
       ContainerController containerController = runtime.createMainContainer(profile);
       */
        Random r = new Random();
        Runtime rt = Runtime.instance();    
        rt.setCloseVM(true);
        // Creation of a new main container

        Profile p = new ProfileImpl();
        p.setParameter(Profile.MAIN_HOST, "192.168.10.1");
        p.setParameter(Profile.MAIN_PORT, "1099");
        AgentContainer ac = rt.createMainContainer(p);
        // Agent creation on local container
        /*try{
            Object[] argumentos = new Object[2];
            argumentos[0] = (float)(r.nextInt(1000)+1);   // Máximo producido
            argumentos[1] = (float)(r.nextInt(1000)+1)/1000;   // Precio que cuesta producir
            
            AgentController agente = ac.createNewAgent ("AHM", "javaapplication2.Productor", argumentos);
            agente.start();
        } catch (StaleProxyException e) {
           e.printStackTrace();
        }*/
       
       for(int i=1; i<2; i++){
           AgentController consumidorController;
           try {
               Object[] argumentos = new Object[1];
               argumentos[0] = (float)i;
               consumidorController = ac.createNewAgent("Consumidor_"+i, "javaapplication2.Consumidor", argumentos);
               consumidorController.start();    
           } catch (StaleProxyException e) {
               e.printStackTrace();
           }
       }

       for(int i=1; i<2; i++){
           AgentController productorController;
           try {
               Object[] argumentos = new Object[2];
               argumentos[0] = (float)(r.nextInt(1000)+1);   // Máximo producido
               argumentos[1] = (float)(r.nextInt(1000)+1)/1000;   // Precio que cuesta producir
               productorController = ac.createNewAgent("Productor_"+i, "javaapplication2.Productor", argumentos);
               productorController.start();    
           } catch (StaleProxyException e) {
               e.printStackTrace();
           }
        }
    }
    
}
