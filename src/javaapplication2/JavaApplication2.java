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
        
        // Agentes entornos
        try{
            
            // Agente Luz
            TipoAgente ta = TipoAgente.LUZ;
            Object[] argumentos = new Object[2];
            argumentos[0] = 80;         // Porcentaje de luz            
            argumentos[1] = ta;            
            AgentController agente = ac.createNewAgent ("Agente_Luminosidad", "javaapplication2.Agente_Entorno", argumentos);
            agente.start();
            
            Thread.sleep(100);
            
            // Agente Temperatura
            ta = TipoAgente.TEMPERATURA;
            argumentos = new Object[2];
            argumentos[0] = 80;         // Porcentaje de temperatura
            argumentos[1] = ta;            
            agente = ac.createNewAgent ("Agente_Temperatura", "javaapplication2.Agente_Entorno", argumentos);
            agente.start();
            
            Thread.sleep(100);
            
            // Agente Caudal
            ta = TipoAgente.CAUDAL;
            argumentos = new Object[2];
            argumentos[0] = 80;         // Porcentaje de temperatura
            argumentos[1] = ta;            
            agente = ac.createNewAgent ("Agente_Caudal", "javaapplication2.Agente_Entorno", argumentos);
            agente.start();
            
            Thread.sleep(100);
            
            // Agente Viento
            ta = TipoAgente.VIENTO;
            argumentos = new Object[2];
            argumentos[0] = 80;         // Porcentaje de temperatura
            argumentos[1] = ta;            
            agente = ac.createNewAgent ("Agente_Viento", "javaapplication2.Agente_Entorno", argumentos);
            agente.start();
            
            Thread.sleep(100);
            
            // Agente Mercado
            ta = TipoAgente.MERCADO;
            argumentos = new Object[2];
            argumentos[0] = 80;         // Porcentaje de temperatura
            argumentos[1] = ta;            
            agente = ac.createNewAgent ("Agente_Mercado", "javaapplication2.Agente_Entorno", argumentos);
            agente.start();
            
        } catch (StaleProxyException e) {
           e.printStackTrace();
        } catch (Exception e)
        {
            System.out.println("-Error 1 - " + e.getMessage());
        }
       
       

       for(int i=1; i<3; i++){
           AgentController productorController;
           try {
               Object[] argumentos = new Object[3];
               argumentos[0] = (float)(r.nextInt(1000)+1);   // Máximo producido
               argumentos[1] = (float)(r.nextInt(1000)+1)/1000;   // Precio que cuesta producir
               argumentos[2] = TipoProductor.getTipoProductor(i % TipoProductor.values().length);
               
               productorController = ac.createNewAgent("Productor_"+i, "javaapplication2.Productor", argumentos);
               productorController.start();    
           } catch (StaleProxyException e) {
               e.printStackTrace();
           }
        }
       
        try{
            Thread.sleep(1000);
        }catch(Exception e){
            System.out.println("Error - " + e.getMessage());
        }
        
       for(int i=1; i<2; i++){
           AgentController consumidorController;
           try {
               Object[] argumentos = new Object[1];
               argumentos[0] = (float)i + 3;
               consumidorController = ac.createNewAgent("Consumidor_"+i, "javaapplication2.Consumidor", argumentos);
               consumidorController.start();    
           } catch (StaleProxyException e) {
               e.printStackTrace();
           }
       }
    }
    
    public enum TipoAgente {

        DESCONOCIDO     ("DESCONOCIDO", 0),
        LUZ             ("LUZ        ", 1),    //Separamos con comas
        VIENTO          ("VIENTO     ", 2),
        TEMPERATURA     ("TEMPERATURA", 3),
        CAUDAL          ("CAUDAL     ", 4),
        MERCADO         ("MERCADO    ", 5),
        CONSUMIDOR      ("CONSUMIDOR ", 6),
        PRODUCTOR       ("PRODUCTOR  ", 7);  //Cuando terminamos cerramos con ;

        //Campos tipo constante   
        private final String nombre; //Color de la madera
        private final int id; //Peso específico de la madera


        TipoAgente (String nombre, int id) { 

            this.nombre = nombre;
            this.id = id;

        } //Cierre del constructor

        //Métodos de la clase tipo Enum
        public String getNombre() { return nombre; }

        public int getId() { return id; }
        
        public static TipoAgente getTipoAgenteEntorno(int id)
        {
            TipoAgente res = TipoAgente.DESCONOCIDO;
            switch(id){
                case 1: res = TipoAgente.LUZ;
                    break;
                case 2: res = TipoAgente.VIENTO;
                    break;
                case 3: res = TipoAgente.TEMPERATURA;
                    break;
                case 4: res = TipoAgente.CAUDAL;
                    break;
                case 5: res = TipoAgente.MERCADO;
                    break;
                case 6: res = TipoAgente.CONSUMIDOR;
                    break;
                case 7: res = TipoAgente.PRODUCTOR;
                    break;
            }
            return res;
        }

    }
    
    
    
    // Clase enum con los tipos de productores que existen
    public enum TipoProductor {

        DESCONOCIDO     ("DESCONOCIDO   ", 0),
        SOLAR           ("SOLAR         ", 1), //Separamos con comas
        EOLICO          ("EOLICO        ", 2),
        GEOTERMICO      ("GEOTERMICO    ", 3),
        PETROLEO        ("PETROLEO      ", 4),
        GAS             ("GAS           ", 5),
        HIDROELECTRICA  ("HIDROELECTRICA", 6),
        ELECTRICA       ("ELECTRICA     ", 7);  //Cuando terminamos cerramos con ;

        //Campos tipo constante   
        private final String nombre; //Color de la madera
        private final int id; //Peso específico de la madera


        TipoProductor (String nombre, int id) { 

            this.nombre = nombre;
            this.id = id;

        } //Cierre del constructor

        //Métodos de la clase tipo Enum
        public String getNombre() { return nombre; }

        public int getId() { return id; }
        
        public static TipoProductor getTipoProductor(int id)
        {
            TipoProductor res = TipoProductor.DESCONOCIDO;
            switch(id){
                case 1: res = TipoProductor.SOLAR;
                    break;
                case 2: res = TipoProductor.EOLICO;
                    break;
                case 3: res = TipoProductor.GEOTERMICO;
                    break;
                case 4: res = TipoProductor.PETROLEO;
                    break;
                case 5: res = TipoProductor.GAS;
                    break;
                case 6: res = TipoProductor.HIDROELECTRICA;
                    break;
                case 7: res = TipoProductor.ELECTRICA;
                    break;
            }
            return res;
        }

    } //Cierre del enum
    // FIN enum TipoProductor
    
}
