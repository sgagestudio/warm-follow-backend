package com.sgagestudio.warm_follow_backend.service;

import com.sgagestudio.warm_follow_backend.model.Channel;
import com.sgagestudio.warm_follow_backend.model.ConsentStatus;
import com.sgagestudio.warm_follow_backend.model.Customer;
import com.sgagestudio.warm_follow_backend.model.DeliveryChannel;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ContactPolicyService {

    public boolean hasConsent(Customer customer, Channel channel) {
        if (!isContactable(customer)) {
            return false;
        }
        Set<String> channels = normalizeChannels(customer.getConsentChannels());
        if (channel == Channel.both) {
            return channels.contains("email") && channels.contains("sms");
        }
        return channels.contains(channel.name().toLowerCase(Locale.ROOT));
    }

    public boolean isContactable(Customer customer, DeliveryChannel channel) {
        if (!isContactable(customer)) {
            return false;
        }
        Set<String> channels = normalizeChannels(customer.getConsentChannels());
        return channels.contains(channel.name().toLowerCase(Locale.ROOT));
    }

    private boolean isContactable(Customer customer) {
        if (customer == null || customer.isErased()) {
            return false;
        }
        return customer.getConsentStatus() == ConsentStatus.granted;
    }

    private Set<String> normalizeChannels(String[] consentChannels) {
        if (consentChannels == null || consentChannels.length == 0) {
            return Set.of();
        }
        Set<String> normalized = new HashSet<>();
        Arrays.stream(consentChannels)
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .forEach(normalized::add);
        return normalized;
    }
}
