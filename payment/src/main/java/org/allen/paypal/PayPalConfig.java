package org.allen.paypal;

import com.paypal.sdk.Environment;
import com.paypal.sdk.PaypalServerSdkClient;
import com.paypal.sdk.authentication.ClientCredentialsAuthModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayPalConfig {
    @Value("${paypal.client.id}") String clientId;
    @Value("${paypal.client.secret}") String clientSecret;

    @Bean
    public PaypalServerSdkClient payPalClient() {
        return new PaypalServerSdkClient.Builder()
                .clientCredentialsAuth(
                        new ClientCredentialsAuthModel.Builder(clientId, clientSecret).build())
                .environment(Environment.SANDBOX)
                .build();
    }

}

