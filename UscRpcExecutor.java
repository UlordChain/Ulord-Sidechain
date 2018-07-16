package tools;

import com.typesafe.config.Config;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class UscRpcExecutor {

    private static String rpc_uri;
    static {
        FederationConfigLoader configLoader = new FederationConfigLoader();
        Config config = configLoader.getConfigFromFiles();
        rpc_uri = "http://" + config.getString("rpc.bind_address") + ":" + config.getString("rpc.port");
    }

    public static String execute(String cmd) throws IOException {
        StringEntity entity = new StringEntity(cmd, ContentType.APPLICATION_JSON);

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(rpc_uri);
        request.setEntity(entity);

        HttpResponse response = httpClient.execute(request);
        return EntityUtils.toString(response.getEntity());
    }
}
