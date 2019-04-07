/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javaapplication2;

import jade.content.Predicate;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.core.AID;
/**
* @author Angelo Difino - CSELT S.p.A
* @version $Date: 2002/07/31 15:27:34 $ $Revision: 2.1 $
*/
public class Agente_Predicado implements Predicate  {
	
	private String 	_name;		
        private AID _owner;
	private Float    _potenciaNecesitada;							//Person's age
	//private Address _address;					//Address' age
        private Float  _precio;
        private int _ciclo;
        private String _info;
	private int _luz = 100;             // Expresado en porcentaje
        private int _viento = 100;          // Expresado en porcentaje
        private int _caudal = 100;          // Expresado en porcentaje
        private int _temperatura = 100;     // Expresado en porcentaje
        private int _mercado = 100;          // Expresado en porcentaje
        private JavaApplication2.TipoProductor _tipoProductor = JavaApplication2.TipoProductor.DESCONOCIDO;
        private JavaApplication2.TipoAgente _tipoAgente = JavaApplication2.TipoAgente.DESCONOCIDO;
        
        public void setTipoAgente(JavaApplication2.TipoAgente tipoAgente)
        {
            _tipoAgente = tipoAgente;
        }
        public JavaApplication2.TipoAgente getTipoAgente(){
            return _tipoAgente;
        }
        public void setTipoProductor(JavaApplication2.TipoProductor tipoProductor)
        {
            _tipoProductor = tipoProductor;
        }
        public JavaApplication2.TipoProductor getTipoProductor(){
            return _tipoProductor;
        }
	// Methods required to use this class to represent the PERSON role
        public void setInfo(String info) {
		_info=info;
	}
	public String getInfo() {
		return _info;
	}
        public void setPrecio(Float precio) {
		_precio=precio;
	}
	public Float getPrecio() {
		return _precio;
	}
        public void setOwner(AID owner) {
		_owner=owner;
	}
	public AID getOwner() {
		return _owner;
	}
	public void setName(String name) {
		_name=name;
	}
	public String getName() {
		return _name;
	}
	public void setPotenciaNecesitada(Float potenciaNecesitada) {
		_potenciaNecesitada=potenciaNecesitada;
	}
	public Float getPotenciaNecesitada() {
		return _potenciaNecesitada;
        }
        public void setCiclo(int ciclo) {
		_ciclo=ciclo;
	}
	public int getCiclo() {
		return _ciclo;
	}
        public void setLuz(int luz) {
		_luz=luz;
	}
	public int getLuz() {
		return _luz;
	}
        public void setViento(int viento) {
		_viento=viento;
	}
	public int getViento() {
		return _viento;
	}
        public void setCaudal(int caudal) {
		_caudal=caudal;
	}
	public int getCaudal() {
		return _caudal;
	}
        public void setTemperatura(int temperatura) {
		_temperatura=temperatura;
	}
	public int getTemperatura() {
		return _temperatura;
	}
        public void setMercado(int mercado) {
		_mercado=mercado;
	}
	public int getMercado() {
		return _mercado;
	}
        /*
	public void setAddress(Address address) {
		_address=address;
	}
	public Address getAddress() {
		return _address;
	}
	*/
        /*
	// Other application specific methods
	public boolean equals(Person p){
		if (!_name.equalsIgnoreCase(p.getName()))
			return false;
		if (_age != null && p.getAge() != null) // Age is an optional field
			if (_age.longValue() != p.getAge().longValue())
				return false;
		if (_address != null && p.getAddress() != null) // Address is an optional field
			if (!_address.equals(p.getAddress()))
				return false;
		return true;
	}
        */
}
