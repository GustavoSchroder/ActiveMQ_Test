package publishsubscriber;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;

/**
 *
 * @author gustavolazarottoschroeder
 */
public class Receiver {

    private ConnectionFactory factory = null;
    private Connection connection = null;
    private Session session = null;
    private Destination destination = null;
    private MessageConsumer consumer = null;
    private ContaCorrente contaCorrente;
    List<String> mensagensRecebidas;

    public Receiver() {
        this.contaCorrente = new ContaCorrente(500);
        this.mensagensRecebidas = new ArrayList<>();
    }

    public void receberTotal() {
        final Thread t;
        t = new Thread() {
            @Override
            public void run() {
                try {
                    receiveMessage();
                } catch (InterruptedException | ParseException ex) {

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
        }, 300000);

    }

    public void receiveMessage() throws InterruptedException, ParseException {
        SimpleDateFormat simpleDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSS");
        try {
//            factory = new ActiveMQConnectionFactory(
//                    ActiveMQConnection.DEFAULT_BROKER_URL);
            factory = new ActiveMQConnectionFactory("admin", "admin", "tcp://131.108.101.210:61616");
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            destination = session.createQueue("SAMPLEQUEUE1");
            consumer = session.createConsumer(destination);

            while (true) {
                Message message;
                try {
                    message = consumer.receive();
                } catch (JMSException e) {
                    try {
                        writeFile(mensagensRecebidas);
                    } catch (IOException ex) {
                        Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    contaCorrente.printResults();
                    return;
                }

                if (message instanceof TextMessage) {
                    TextMessage text = (TextMessage) message;

                    try {
                        String[] info = text.getText().split(";");
                        mensagensRecebidas.add(simpleDate.format(new Date()) + ";" + (diffInSec(new Date(), simpleDate.parse(info[3]))) + ";" + text.getText().getBytes().length);
                        this.contaCorrente.realizarOp(Integer.parseInt(info[0]), info[1], Double.parseDouble(info[2]));
                    } catch (NumberFormatException e) {

                    }
                }
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private Long diffInSec(Date newerDate, Date olderDate) {
        long diffInMillies = Math.abs(newerDate.getTime() - olderDate.getTime());
        return diffInMillies;
    }

    private void writeFile(List<String> result) throws IOException {
        String content = "";
        for (String output : result) {
            content += (output) + "\n";
        }

        BufferedWriter bw = null;
        FileWriter fw = null;

        try {
            fw = new FileWriter("/Users/gustavolazarottoschroeder/Documents/GitHub/ActiveMQ_Test/PublishSubscriber/src/publishsubscriber/output/receiver.csv", true);
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

    public void receiveOneMessage() throws InterruptedException, ParseException {
        SimpleDateFormat simpleDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSS");
        try {
            factory = new ActiveMQConnectionFactory("admin", "admin", "tcp://131.108.101.210:61616");
            connection = factory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            destination = session.createQueue("SAMPLEQUEUE1");
            consumer = session.createConsumer(destination);

            Message message;
            try {
                message = consumer.receive();
                System.out.println("R;" + simpleDate.format(new Date()));
            } catch (JMSException e) {

            }

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException, ParseException {
        System.out.println("Receiver");
//        Receiver receiver = new Receiver();
//        Publisher publisher = new Publisher();
//        publisher.sendMessageOneMessage();
//        receiver.receiveOneMessage();
        for (int i = 0; i < 2; i++) {
            Receiver receiver = new Receiver();
            receiver.receberTotal();
        }
        
        System.out.println("Sender");

        for (int i = 0; i < 500; i++) {
            Publisher sender = new Publisher();
            sender.setMAX_THREADS(i);
            sender.calculaTotal(i);
            Thread.sleep(200);
        }
    }
}
