package nl.tue.iot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by rparra on 15/1/16.
 */
public class ShellCommandRunner {

    protected String execute(String command) {
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

    protected void executeAsync(String command) {
        String[] commands = new String[]{"/bin/sh", "-c", command};
        ProcessBuilder builder = new ProcessBuilder(commands);
        try {
            Process process = builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
