/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javaapplication2;

import jade.content.Predicate;

/**
* @author Angelo Difino - CSELT S.p.A
* @version $Date: 2002/07/31 15:27:34 $ $Revision: 2.1 $
*/
public class Ontologia_Contenedor implements Predicate {
	
	private String 	_name;						//Person's name
	private Long    _age;							//Person's age
	//private Address _address;					//Address' age
        private Double  _precio;
	
	// Methods required to use this class to represent the PERSON role
        public void setPrecio(Double precio) {
		_precio=precio;
	}
	public Double getPrecio() {
		return _precio;
	}
	public void setName(String name) {
		_name=name;
	}
	public String getName() {
		return _name;
	}
	public void setAge(Long age) {
		_age=age;
	}
	public Long getAge() {
		return _age;
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
