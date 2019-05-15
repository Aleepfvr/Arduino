package br.com.fiap.arduino.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONArray;

import br.com.fiap.arduino.beans.Sensor;

public class MyMqtt {
	private IMqttClient mqttClient; 
	private String lastMessage = null;
	
	public MyMqtt() throws MqttException {
		try {
			String url = "tcp://iot.eclipse.org:1883";
			String clientId = UUID.randomUUID().toString();
			//Por padrão o Paho usa persistência em disco,
			//mas pode ter problema com permissão quando usado em um Webservice
			MqttClientPersistence persist = new MemoryPersistence();
			mqttClient = new MqttClient(url, clientId, persist);
			
			MqttConnectOptions options = new MqttConnectOptions();
			options.setAutomaticReconnect(true);
			options.setCleanSession(true);
			options.setConnectionTimeout(10);
			
			mqttClient.connect(options);
		} catch (MqttException e) {
			throw new RuntimeException(e);
		}
	}
	@GET @Path("{topic}")
	@Produces(MediaType.TEXT_PLAIN)
    public synchronized String getMessage(@PathParam("topic") String topic) throws MqttSecurityException, MqttException, InterruptedException {	
		lastMessage = null;
		mqttClient.subscribe(topic.replace('_', '/'), new IMqttMessageListener() {
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				synchronized (MyMqtt.this) {
					lastMessage = message.toString();
					MyMqtt.this.notifyAll();
				}
			}
		});
		this.wait();
		return lastMessage;
    }
	
	@GET @Path("{topic}")
	@Produces(MediaType.TEXT_PLAIN)
    public synchronized List<Sensor> getSensores(@PathParam("topic") String topic) throws MqttSecurityException, MqttException, InterruptedException {	
		List<Sensor> sensores = new ArrayList<Sensor>();
		Sensor sensor = new Sensor();
		mqttClient.subscribe(topic.replace('_', '/'), new IMqttMessageListener() {
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				synchronized (MyMqtt.this) {
					JSONArray json = new JSONArray(message.toString());
					for(int i = 0; i < json.length(); i++) {
						sensor.setLeitora(json.getJSONObject(i).getString("leitora"));
						sensor.setNome(json.getJSONObject(i).getString("sensor"));
						sensores.add(sensor);
					}
					MyMqtt.this.notifyAll();
				}
			}
		});
		this.wait();
		return sensores;
    }
	
	@GET @Path("{topic}")
	@Produces(MediaType.TEXT_PLAIN)
    public synchronized List<String> getSensoresDp(@PathParam("topic") String topic) throws MqttSecurityException, MqttException, InterruptedException {	
		List<String> disponiveis = new ArrayList<String>();
		mqttClient.subscribe(topic.replace('_', '/'), new IMqttMessageListener() {
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				synchronized (MyMqtt.this) {
					JSONArray json = new JSONArray(message.toString());
					for(int i = 0; i < json.length(); i++) {
						disponiveis.add(json.getJSONObject(i).getString("sensor"));
					}
					MyMqtt.this.notifyAll();
				}
			}
		});
		this.wait();
		return disponiveis;
    }
	
	@GET @Path("{topic}")
	@Produces(MediaType.TEXT_PLAIN)
    public synchronized List<String> getComandos(@PathParam("topic") String topic) throws MqttSecurityException, MqttException, InterruptedException {	
		List<String> disponiveis = new ArrayList<String>();
		mqttClient.subscribe(topic.replace('_', '/'), new IMqttMessageListener() {
			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {
				synchronized (MyMqtt.this) {
					JSONArray json = new JSONArray(message.toString());
					for(int i = 0; i < json.length(); i++) {
						disponiveis.add(json.getString(i));
					}
					MyMqtt.this.notifyAll();
				}
			}
		});
		this.wait();
		return disponiveis;
    }
	
	@POST @Path("publish/{topic}")
	@Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public String getCliente(@PathParam("topic") String topic, String message) throws MqttPersistenceException, MqttException {	
		topic = topic.replace('_', '/');
		if(mqttClient.isConnected()) {
			MqttMessage msg = new MqttMessage(message.getBytes());
	        msg.setQos(0);
	        msg.setRetained(false);
	        mqttClient.publish(topic,msg);
	        return "Message sent to topic: "+ topic;
		} else return "Client not connected!";
    }
	
}
