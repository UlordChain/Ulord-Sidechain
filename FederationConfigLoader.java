package tools;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;

public class FederationConfigLoader {
    public Config getConfigFromFiles() {
        String file = System.getProperty("federation.conf.file");
        Config cmdLineConfigFile = file != null ? ConfigFactory.parseFile(new File(file)) : ConfigFactory.empty();


        Config userConfig = ConfigFactory.systemProperties()
                .withFallback(cmdLineConfigFile);

        return userConfig;
    }
}
