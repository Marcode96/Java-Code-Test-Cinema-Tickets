package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    /** Ticket payment service. */
    private final TicketPaymentService ticketPaymentService;

    /** Seat reservation service. */
    private final SeatReservationService seatReservationService;

    /** Constructor. **/
    public TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        // mocked implementation to make sure unit tests compile and fail, until the code is added
        ticketPaymentService.makePayment(1L, 25);
        seatReservationService.reserveSeat(1L, 1);
    }

}
