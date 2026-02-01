package com.makerspacetools.service;

import com.makerspacetools.client.MakerSpaceClient;
import com.makerspacetools.makerspace.response.MakerSpaceResourceAvailabilityResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Queries machine availability from the MakerSpace API.
 */
@Service
class MachineQueryService {

    private final MakerSpaceClient client;

    @Autowired
    MachineQueryService(MakerSpaceClient client) {
        this.client = client;
    }

    MakerSpaceResourceAvailabilityResponse checkAvailability(int days, String guid, String startTime, int interval) {
        return client.checkAvailability(days, guid, startTime, interval, Map.of());
    }
}
