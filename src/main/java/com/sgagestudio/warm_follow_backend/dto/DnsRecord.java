package com.sgagestudio.warm_follow_backend.dto;

public record DnsRecord(
        DnsRecordType type,
        String name,
        String value,
        Integer ttl,
        Integer priority,
        DnsRecordPurpose purpose
) {
}
