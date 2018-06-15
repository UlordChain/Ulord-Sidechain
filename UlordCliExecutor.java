package tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class UlordCliExecutor {
    public static String execute(String command) throws IOException {
        Process proc = null;
        Runtime runtime = Runtime.getRuntime();
        proc = runtime.exec(new String[] {"bash", "-c", command});

        InputStream inputStream = proc.getInputStream();
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        String result = Utils.BufferedReaderToString(bufferedReader);

        if(result.equals("")) {
            inputStream = proc.getErrorStream();
            inputStreamReader = new InputStreamReader(inputStream);
            bufferedReader = new BufferedReader(inputStreamReader);
            return Utils.BufferedReaderToString(bufferedReader);
        }
        return result;
    }
}
