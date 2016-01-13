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
public class AvahiBrowser {

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
            if (attrs.length == 9 && attrs[3].equals(this.serviceName)) {
                this.hostName = attrs[7];
                this.port = Integer.parseInt(attrs[8]);
            }
        }


    }

    private String execute(String command) {
        StringBuilder sb = new StringBuilder();
        String[] commands = new String[]{"/bin/sh", "-c", command};
        try {
            Process proc = new ProcessBuilder(commands).start();
            BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(proc.getInputStream()));

            BufferedReader stdError = new BufferedReader(new
                    InputStreamReader(proc.getErrorStream()));

            String s = null;
            while ((s = stdInput.readLine()) != null) {
                sb.append(s);
                sb.append("\n");
            }

            while ((s = stdError.readLine()) != null) {
                sb.append(s);
                sb.append("\n");
            }
        } catch (IOException e) {
            return e.getMessage();
        }
        return sb.toString();
    }

}
