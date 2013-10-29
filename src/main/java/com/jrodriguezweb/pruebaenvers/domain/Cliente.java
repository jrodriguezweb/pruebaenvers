package com.jrodriguezweb.pruebaenvers.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;

@Entity
@Audited
public class Cliente {

	@Id
	private int idCliente;

	@Column
	private String nombre;

	@OneToMany(mappedBy="cliente")
	private List<Coche> coches;

	public Cliente() {
		coches = new ArrayList<Coche>();
	}
	
	public void addCoche(Coche c){
		coches.add(c);
	}
	
	public int getIdCliente() {
		return idCliente;
	}

	public void setIdCliente(int idCliente) {
		this.idCliente = idCliente;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public List<Coche> getCoches() {
		return coches;
	}

	public void setCoches(List<Coche> coches) {
		this.coches = coches;
	}

}
