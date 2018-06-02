package tools;

import co.usc.peg.Bridge;
import co.usc.ulordj.core.Sha256Hash;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Prepare_eth_call {
    public static void main(String []args){
        try
        {
            Process proc = null;
            Runtime rt = Runtime.getRuntime();

            proc = rt.exec("ulord-cli getblockcount");
            InputStream inStr = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(inStr);
            BufferedReader br = new BufferedReader(isr);

            int blockCount = Integer.parseInt(br.readLine());

            StringBuilder builder = new StringBuilder();
            String line = null;
            for(int i=1; i<blockCount; ++i){
                proc = rt.exec("ulord-cli getblockhash "+ i);
                inStr = proc.getInputStream();
                isr = new InputStreamReader(inStr);
                br = new BufferedReader(isr);

                line = br.readLine();
                proc = rt.exec("ulord-cli getblockheader " + line +" false");
                inStr = proc.getInputStream();
                isr = new InputStreamReader(inStr);
                br = new BufferedReader(isr);
                line = null;

                line = br.readLine();
                if(line != null) {
                    builder.append(line);
                    builder.append(" ");
                }

                if(i%500==0 || i == blockCount){
                    builder.insert(0, "receiveHeaders ");
                    String curl = "curl -X POST --data '{\"jsonrpc\":\"2.0\",\"method\":\"eth_call\",\"params\":[{\"to\":\"0x0000000000000000000000000000000001000006\", \"data\": \""+ getReceiveHeadersString(builder.toString().split(" ")) +"\" },\"latest\"], \"id\":1}' -H \"Content-Type:application/json\" localhost:44444";

                    System.out.println(curl);

                    try{

                        //TODO : Need to find a way to send request to rpc of USC using java.


                        /*
                        HttpClient httpClient = HttpClientBuilder.create().build();

                        HttpPost request = new HttpPost("http://localhost:44444");
                        StringEntity params =new StringEntity("{\"jsonrpc\":\"2.0\",\"method\":\"eth_call\",\"params\":[{\"to\":\"0x0000000000000000000000000000000001000006\", \"data\": \"" + getReceiveHeadersString(builder.toString().split(" ")) +"\" },\"latest\"], \"id\":1}");
                        request.addHeader("content-type", "application/json");
                        request.setEntity(params);
                        HttpResponse response = httpClient.execute(request);
                        */
                    }catch(Exception ex){
                        System.out.println(ex.getMessage());
                    }

                    Thread.sleep(10000);
                    builder = new StringBuilder();
                }
            }
        } catch (Throwable t)
        {
            t.printStackTrace();
        }
    }

    private static String getReceiveHeadersString(String[] args) {
        if(args.length < 2)
            return "receiveHeaders <headers seperated by space>";

        byte[][] blocks = new byte[args.length - 1][140];
        for(int i = 0; i < args.length - 1; ++i) {
            byte[] b = Sha256Hash.hexStringToByteArray(args[i + 1]);
            blocks[i] = b;
        }
        return Sha256Hash.bytesToHex((Bridge.RECEIVE_HEADERS.encode(new Object[]{blocks})));
    }
}
