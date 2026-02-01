package com.makerspacetools.client;

import com.makerspacetools.makerspace.request.MakerSpaceBasketRequest;
import com.makerspacetools.makerspace.request.MakerSpaceCancelBookingRequest;
import com.makerspacetools.makerspace.request.MakerSpaceInvoicePreviewRequestItem;
import com.makerspacetools.makerspace.response.MakerSpaceInvoicePreviewResponse;
import com.makerspacetools.makerspace.response.MakerSpaceMyBookingsResponse;
import com.makerspacetools.makerspace.response.MakerSpaceResourceAvailabilityResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Map;
import java.util.Set;

/**
 * HTTP client for MakerSpace API.
 */
@HttpExchange("/en")
public interface MakerSpaceClient {

    /**
     * Checks availability for a resource.
     *
     * @param days number of days to query
     * @param guid resource guid
     * @param startTime query start time
     * @param interval slot interval in minutes
     * @param body request body payload
     */
    @PostExchange("/bookings/GetAvailabilityAtWithUser")
    MakerSpaceResourceAvailabilityResponse checkAvailability(
            @RequestParam int days,
            @RequestParam String guid,
            @RequestParam String startTime,
            @RequestParam int interval,
            @RequestBody Map<String, Object> body);

    /**
     * Previews an invoice for bookings.
     *
     * @param bookings booking request items
     * @return preview response
     */
    @PostExchange("/basket/PreviewInvoice?createZeroValueInvoice=true&_shape=Id,Currency,UsedExtraServices,UsedBookingCredits,TotalAmount,TaxAmount,CoworkerProductName,LinesRaw.DiscountCode,LinesRaw.DiscountAmount,LinesRaw.BookingUniqueId,LinesRaw.UnitPrice,LinesRaw.SubTotal,LinesRaw.TotalAmount,LinesRaw.TaxAmount,LinesRaw.Description,LinesRaw.CoworkerProductName,Errors,Message,WasSuccessful,Status")
    MakerSpaceInvoicePreviewResponse previewInvoice(@RequestBody Set<MakerSpaceInvoicePreviewRequestItem> bookings);

    /**
     * Creates an invoice for a booking.
     *
     * @param makerSpaceBasketRequest booking basket
     */
    @PostExchange("/basket/CreateInvoice")
    void bookProduct(@RequestBody MakerSpaceBasketRequest makerSpaceBasketRequest);

    /**
     * Loads bookings for the current user.
     *
     * @param depth response depth
     * @return bookings response
     */
    @GetExchange("/bookings/my")
    MakerSpaceMyBookingsResponse myBookings(@RequestParam("_depth") int depth);

    /**
     * Cancels a booking by id.
     *
     * @param bookingId booking id
     * @param request cancellation request
     */
    @PostExchange("/bookings/deletejson/{bookingId}")
    void cancelBooking(@PathVariable long bookingId, @RequestBody MakerSpaceCancelBookingRequest request);
}
