package com.makerspacetools.service;

import com.makerspacetools.client.MakerSpaceClient;
import com.makerspacetools.makerspace.request.MakerSpaceBasketRequest;
import com.makerspacetools.model.SetupData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for submitting booking requests.
 */
@Service
class BookingSubmitter {

    private final MakerSpaceClient client;
    private final SetupData setupData;

    @Autowired
    BookingSubmitter(MakerSpaceClient client, SetupData setupData) {
        this.client = client;
        this.setupData = setupData;
    }

    void submitBooking(BookingTiming timing, String uniqueId) {
        MakerSpaceBasketRequest makerSpaceBasketRequest = buildBasket(uniqueId, timing);
        client.bookProduct(makerSpaceBasketRequest);
    }

    private MakerSpaceBasketRequest buildBasket(String uniqueId, BookingTiming timing) {
        MakerSpaceBasketRequest.Booking booking = MakerSpaceBasketRequest.Booking.builder()
                .uniqueId(uniqueId)
                .fromTime(NexudusBookingTimeAdjuster.adjust(timing.startDateTime()))
                .toTime(NexudusBookingTimeAdjuster.adjust(timing.endDateTime()))
                .resourceId(setupData.embroideryMachine().id())
                .coworkerId(setupData.coworker().id())
                .build();
        MakerSpaceBasketRequest.BasketItem item = MakerSpaceBasketRequest.BasketItem.of(booking);
        return MakerSpaceBasketRequest.of(item);
    }
}
