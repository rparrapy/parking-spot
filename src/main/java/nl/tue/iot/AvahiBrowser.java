package nl.tue.iot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by rparra on 12/1/16.
 *
 * Calls avahi and parses the result to get the hostname and the port
 * for serviceName
 */
public class AvahiBrowser extends ShellCommandRunner{

    private String serviceName;
    private final String SERVICE_TYPE = "_coap._udp";
    private final String COMMAND = "avahi-browse -rtp " + SERVICE_TYPE;
    private String hostName;
    private int port;

    public AvahiBrowser(String serviceName) {
        this.serviceName = serviceName;
        parseOutput(execute(COMMAND));
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getServiceName() {

        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    private void parseOutput(String output) {
        String[] lines = output.split(System.getProperty("line.separator"));
        for(String line : lines){
            String[] attrs = line.split(";");
            System.out.println(line);
            System.out.println(attrs.length);
            if (attrs.length == 9 && attrs[3].equals(this.serviceName)) {
                System.out.println("parsing");
                this.hostName = attrs[7];
                this.port = Integer.parseInt(attrs[8]);
            }
        }


    }



}
