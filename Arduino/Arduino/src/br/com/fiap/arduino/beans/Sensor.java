package br.com.fiap.arduino.beans;

public class Sensor {
	private String nome;
	private String leitora;
	
	public Sensor() {
		super();
	}
	public Sensor(String nome, String leitora) {
		super();
		this.nome = nome;
		this.leitora = leitora;
	}
	public String getNome() {
		return nome;
	}
	public void setNome(String nome) {
		this.nome = nome;
	}
	public String getLeitora() {
		return leitora;
	}
	public void setLeitora(String leitora) {
		this.leitora = leitora;
	}
	
}
