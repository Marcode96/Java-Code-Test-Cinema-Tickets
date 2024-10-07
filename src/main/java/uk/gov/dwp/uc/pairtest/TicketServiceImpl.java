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

    private static final int ADULT_TICKET_PRICE = 25;
    private static final int CHILD_TICKET_PRICE = 15;
    // For now, it could make more sense to not define this, but this might change in the future
    private static final int INFANT_TICKET_PRICE = 0;

    private static final int MAX_NUMBER_OF_TICKETS = 25;

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

        // Then we validate the request
        validateTicketRequest(ticketTypeRequests);

        //And finally we make the 2 calls to the external services in order to pay the correct amount and reserve the relevant seats
        ticketPaymentService.makePayment(accountId, calculateAmountToPay(ticketTypeRequests));
        seatReservationService.reserveSeat(accountId, calculateNumberOfSeatsToReserve(ticketTypeRequests));
    }

    /**
     * This method calculate the relevant number of seats to book considering the business rules,
     * i.e. based on the type of ticket requested
     * @param ticketTypeRequests the request
     * @return numberOfSeatsToReserve number of seats to reserve
     */
    private int calculateNumberOfSeatsToReserve(TicketTypeRequest[] ticketTypeRequests) {

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

    /**
     * This method calculate the correct amount to pay based on the business rules,
     * i.e. depending on the type of ticket requested
     * @param ticketTypeRequests the request
     * @return amountToPay correct amount to pay
     */
    private int calculateAmountToPay(TicketTypeRequest[] ticketTypeRequests) {
        int amountToPay = 0;
        for (TicketTypeRequest ticketTypeRequest : ticketTypeRequests) {
            switch(ticketTypeRequest.getTicketType()) {
                case ADULT:
                    amountToPay += ADULT_TICKET_PRICE * ticketTypeRequest.getNoOfTickets();
                    break;
                case CHILD:
                    amountToPay += CHILD_TICKET_PRICE * ticketTypeRequest.getNoOfTickets();
                    break;
                case INFANT:
                    // As mentioned in the comment above, for now we could do without this, but this is to future-proof the code
                    amountToPay += INFANT_TICKET_PRICE * ticketTypeRequest.getNoOfTickets();
                    break;
            }
        }
        return amountToPay;
    }

    /**
     * This method validate the ticket request received
     * @param ticketTypeRequests
     */
    private void validateTicketRequest(TicketTypeRequest[] ticketTypeRequests) {
        //TODO: this could be replaced with a series of predicates to apply to the ticketTypeRequests stream
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
            if(ticketTypeRequest.getNoOfTickets() <= 0) {
                throw new InvalidPurchaseException("Invalid number of tickets requested");
            }
        }
        // After the basic validation, we need to check that at least one adult is present
        if (Arrays.stream(ticketTypeRequests).noneMatch(ticketTypeRequest -> ticketTypeRequest.getTicketType() == Type.ADULT)) {
            throw new InvalidPurchaseException("An adult ticket purchase is required");
        }
        // We also need to make sure that the total is not over the limit of 25 tickets
        if (Arrays.stream(ticketTypeRequests).mapToInt(TicketTypeRequest::getNoOfTickets).sum() > MAX_NUMBER_OF_TICKETS) {
            throw new InvalidPurchaseException("You can only purchase up to " + MAX_NUMBER_OF_TICKETS + " tickets");
        }
        // And check that there are enough adults compared to the infants
        if (Arrays.stream(ticketTypeRequests).filter(ticketTypeRequest -> ticketTypeRequest.getTicketType() == Type.INFANT)
            .mapToInt(TicketTypeRequest::getNoOfTickets).sum() >
            Arrays.stream(ticketTypeRequests).filter(ticketTypeRequest -> ticketTypeRequest.getTicketType() == Type.ADULT)
                .mapToInt(TicketTypeRequest::getNoOfTickets).sum()) {
            throw new InvalidPurchaseException("There are not enough adults for infants to seat");
        }
    }

}
