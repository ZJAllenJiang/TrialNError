package org.allen.stripe;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {
    @Value("${stripe.api.key}") String apiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }
}

