package uk.gov.dwp.uc.pairtest;

import java.util.Arrays;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type;
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

        // First we check that the account id is valid
        if(accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Invalid account id");
        }
        validateTicketRequest(ticketTypeRequests);
        ticketPaymentService.makePayment(accountId, calculateAmountToPay(ticketTypeRequests));
        seatReservationService.reserveSeat(accountId, calculateNumberOfSeatsToReserve(ticketTypeRequests));
    }

    private int calculateNumberOfSeatsToReserve(TicketTypeRequest[] ticketTypeRequests) {

        // First we need to check that at least one adult is present
        if (Arrays.stream(ticketTypeRequests).noneMatch(ticketTypeRequest -> ticketTypeRequest.getTicketType() == Type.ADULT)) {
            throw new InvalidPurchaseException("An adult ticket purchase is required");
        }
        int numberOfSeatsToReserve = 0;
        for (TicketTypeRequest ticketTypeRequest : ticketTypeRequests) {

            if(ticketTypeRequest.getTicketType() == Type.ADULT) {
                numberOfSeatsToReserve += ticketTypeRequest.getNoOfTickets();
            }
            if(ticketTypeRequest.getTicketType() == Type.CHILD) {
                numberOfSeatsToReserve += ticketTypeRequest.getNoOfTickets();
            }
            // For infants, we don't want to add a seat
        }
        return numberOfSeatsToReserve;
    }

    private int calculateAmountToPay(TicketTypeRequest[] ticketTypeRequests) {
        int amountToPay = 0;
        for (TicketTypeRequest ticketTypeRequest : ticketTypeRequests) {
            if(ticketTypeRequest.getTicketType() == Type.ADULT) {
                amountToPay += 25 * ticketTypeRequest.getNoOfTickets();
            }
            if(ticketTypeRequest.getTicketType() == Type.CHILD) {
                amountToPay += 15 * ticketTypeRequest.getNoOfTickets();
            }
        }
        return amountToPay;
    }

    private void validateTicketRequest(TicketTypeRequest[] ticketTypeRequests) {
        if(ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("Invalid ticket request");
        }
        for (TicketTypeRequest ticketTypeRequest : ticketTypeRequests) {
            if(ticketTypeRequest == null) {
                throw new InvalidPurchaseException("Invalid ticket request");
            }
            if (ticketTypeRequest.getTicketType() == null) {
                throw new InvalidPurchaseException("Invalid type of ticket requested");
            }
            if(ticketTypeRequest.getNoOfTickets() <= 0 || ticketTypeRequest.getNoOfTickets() > 25) {
                throw new InvalidPurchaseException("Invalid number of tickets requested");
            }
        }
    }

}
