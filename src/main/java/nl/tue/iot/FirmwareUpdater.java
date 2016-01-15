package nl.tue.iot;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created by rparra on 15/1/16.
 */
public class FirmwareUpdater extends ShellCommandRunner{

    private String url;

    public FirmwareUpdater(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void update() {
        try {
            FileUtils.copyURLToFile(new URL(this.url), new File("parking-sense/server.py"));
        } catch (IOException e) {
            System.out.println("Error when downloading update");
        }

        String PIDDiscoveryCommand = "ps aux | grep server.py";
        String pid = getPID(execute(PIDDiscoveryCommand));
        System.out.println(PIDDiscoveryCommand);


        String killCommand = "kill " + pid;
        execute(killCommand);
        System.out.println(killCommand);


        String runCommand = "python /home/pi/parking-sense/server.py";
        executeAsync(runCommand);
        System.out.println(runCommand);

    }

    private String getPID(String output) {
        String pid = "";
        String[] lines = output.split(System.getProperty("line.separator"));
        for(String line : lines){
            String[] columns = line.split(" +");

            System.out.println(line);
            System.out.println(columns.length);
            if (columns[10].equals("python") && columns[11].endsWith("server.py")) {
                System.out.println("parsing");
                pid = columns[1];
            }
        }
        System.out.println(pid);
        return pid;
    }


}
