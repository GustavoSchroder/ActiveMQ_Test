package publishsubscriber;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

public class Publisher {

    private ConnectionFactory factory = null;
    private Connection connection = null;
    private Session session = null;
    private Destination destination = null;
    private MessageProducer producer = null;
    List<String> mensagensEnviadas;

    public Publisher() {
        this.mensagensEnviadas = new ArrayList<>();
    }

    public void calculaTotal(Integer i) {
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    sendMessage(i);
                } catch (InterruptedException ex) {

                }
            }
        };

        t.start();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            public void run() {
                System.out.println("30 Seconds Later");
                if (null != t) {
                    t.interrupt();
                }
            }
        }, 100000);

    }

    public void sendMessage(Integer i) throws InterruptedException {
        SimpleDateFormat simpleDate = new SimpleDateFormat("HH:MM:SS");
        try {
//            factory = new ActiveMQConnectionFactory(
//                    ActiveMQConnection.DEFAULT_BROKER_URL);
            factory = new ActiveMQConnectionFactory("admin", "admin", "tcp://131.108.101.210:61616");
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            destination = session.createQueue("SAMPLEQUEUE1");
            producer = session.createProducer(destination);

            TextMessage message = session.createTextMessage();
            for (int j = 0; j < 1000; j++) {
                Thread.sleep(randomValueGenerator(100.00, 300.00).intValue());
                //message.setText("Sender: " + i + " - ID: " + log.toString() + " - Teste - " + simpleDate.format(new Date()));
                message.setText(i + ";" + randomOperation() + ";" + randomValueGenerator(10.00, 10000.00) + ";" + simpleDate.format(new Date()));
                mensagensEnviadas.add(message.getText());

                try {
                    producer.send(message);
                } catch (JMSException e) {
                    try {
                        writeFile(mensagensEnviadas);
                    } catch (IOException ex) {
                        Logger.getLogger(Publisher.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                //System.out.println("Enviado: " + message.getText());
            }

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private void writeFile(List<String> result) throws IOException {
        String content = "";
        for (String output : result) {
            content += (output) + "\n";
        }

        BufferedWriter bw = null;
        FileWriter fw = null;

        try {
            fw = new FileWriter("/Users/gustavolazarottoschroeder/Documents/GitHub/ActiveMQ_Test/PublishSubscriber/src/publishsubscriber/output/sender.csv", true);
            bw = new BufferedWriter(fw);
            bw.write(content);

            System.out.println("Done");

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            try {
                if (bw != null) {
                    bw.close();
                }

                if (fw != null) {
                    fw.close();
                }

            } catch (IOException ex) {

                ex.printStackTrace();

            }

        }
    }

    private Double randomValueGenerator(Double rangeMin, Double rangeMax) {
        return (rangeMin + (rangeMax - rangeMin) * (new Random()).nextDouble());
    }

    private String randomOperation() {
        int random = (int) (Math.random() * 2 + 1);
        if (random == 1) {
            return "Depositar";
        } else {
            return "Sacar";
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Sender");
        for (int i = 0; i < 300; i++) {
            //System.out.println(i);
            Publisher sender = new Publisher();
            sender.calculaTotal(i);
            Thread.sleep(100);
        }
    }
}
