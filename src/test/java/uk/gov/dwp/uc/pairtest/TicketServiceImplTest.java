package uk.gov.dwp.uc.pairtest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.CHILD;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.INFANT;

@RunWith(MockitoJUnitRunner.class)
public class TicketServiceImplTest {

  @Mock
  TicketPaymentService ticketPaymentService;

  @Mock
  SeatReservationService seatReservationService;

  @InjectMocks
  TicketServiceImpl ticketService;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  ArgumentCaptor<Long> accountIdForPaymentCaptor;
  ArgumentCaptor<Integer> amountToPayCaptor;
  ArgumentCaptor<Long> accountIdForSeatReservationCaptor;
  ArgumentCaptor<Integer> seatsReservedCaptor;

  @Before
  public void setupArgumentsToCapture() {
    accountIdForPaymentCaptor = ArgumentCaptor.forClass(Long.class);
    amountToPayCaptor = ArgumentCaptor.forClass(Integer.class);
    accountIdForSeatReservationCaptor = ArgumentCaptor.forClass(Long.class);
    seatsReservedCaptor = ArgumentCaptor.forClass(Integer.class);
  }

  /** Happy path test */
  @Test
  public void whenOneTicketRequestIsValidThenTheTicketPaymentServiceAndSeatReservationServiceAreCalledWithTheCorrectParameters() {

    ticketService.purchaseTickets(1L, new TicketTypeRequest(ADULT, 1));

    verify(ticketPaymentService, times(1)).makePayment(accountIdForPaymentCaptor.capture(), amountToPayCaptor.capture());
    verify(seatReservationService, times(1)).reserveSeat(accountIdForSeatReservationCaptor.capture(),seatsReservedCaptor.capture());
    assertEquals("Account id is 1", 1L, accountIdForPaymentCaptor.getValue().longValue());
    assertEquals("Amount to pay is £25", 25, amountToPayCaptor.getValue().intValue());
    assertEquals("Account id is 1", 1L, accountIdForSeatReservationCaptor.getValue().longValue());
    assertEquals("The number of seats reserved is 1", 1, seatsReservedCaptor.getValue().intValue());
  }

  /** More complicated happy path test */
  @Test
  public void whenTheTicketRequestsAreAllValidThenTheTicketPaymentServiceAndSeatReservationServiceAreCalledWithTheCorrectParameters() {

    ticketService.purchaseTickets(1L, new TicketTypeRequest(ADULT, 2),
                                                new TicketTypeRequest(CHILD, 1),
                                                new TicketTypeRequest(INFANT, 1));

    verify(ticketPaymentService, times(1)).makePayment(accountIdForPaymentCaptor.capture(), amountToPayCaptor.capture());
    verify(seatReservationService, times(1)).reserveSeat(accountIdForSeatReservationCaptor.capture(),seatsReservedCaptor.capture());
    assertEquals("Account id is 1", 1L, accountIdForPaymentCaptor.getValue().longValue());
    assertEquals("Amount to pay is £65", 65, amountToPayCaptor.getValue().intValue());
    assertEquals("Account id is 1", 1L, accountIdForSeatReservationCaptor.getValue().longValue());
    assertEquals("The number of seats reserved is 3", 3, seatsReservedCaptor.getValue().intValue());
  }

  /** Business requirements */
  @Test
  public void whenAPaymentRequestForOneChildWithNoAdultIsMadeThenAnExceptionIsThrown() {

    ticketService.purchaseTickets(1L, new TicketTypeRequest(CHILD, 1));

    thrown.expect(InvalidPurchaseException.class);
    thrown.expectMessage("An adult ticket purchase is required");
  }

  @Test
  public void whenAPaymentRequestForOneInfantWithNoAdultIsMadeThenAnExceptionIsThrown() {

    ticketService.purchaseTickets(1L, new TicketTypeRequest(INFANT, 1));

    thrown.expect(InvalidPurchaseException.class);
    thrown.expectMessage("An adult ticket purchase is required");
  }

  @Test
  public void whenTheNumberOfRequestedTicketsIsZeroThenAnExceptionIsThrown() {
    ticketService.purchaseTickets(1L, new TicketTypeRequest(ADULT, 0));

    thrown.expect(InvalidPurchaseException.class);
    thrown.expectMessage("Invalid number of tickets requested");
  }

  @Test
  public void whenTheNumberOfRequestedTicketsIsNegativeThenAnExceptionIsThrown() {
    ticketService.purchaseTickets(1L, new TicketTypeRequest(ADULT, -1));

    thrown.expect(InvalidPurchaseException.class);
    thrown.expectMessage("Invalid number of tickets requested");
  }

  @Test
  public void whenTheNumberOfRequestedTicketsIsMoreThanTheLimitThenAnExceptionIsThrown() {
    ticketService.purchaseTickets(1L, new TicketTypeRequest(ADULT, 26));

    thrown.expect(InvalidPurchaseException.class);
    thrown.expectMessage("Invalid number of tickets requested");
  }

  /** Invalid request tests */
  @Test
  public void whenTheTicketTypeIsInvalidThenAnExceptionIsThrown() {

    ticketService.purchaseTickets(1L, new TicketTypeRequest(null, 1));

    thrown.expect(InvalidPurchaseException.class);
    thrown.expectMessage("Invalid type of ticket requested");
  }

  @Test
  public void whenAnEmptyRequestIsReceivedThenAnExceptionIsThrown() {

    ticketService.purchaseTickets(1L, new TicketTypeRequest(null, 0));

    thrown.expect(InvalidPurchaseException.class);
    thrown.expectMessage("Invalid ticket request");
  }

  @Test
  public void whenANullRequestIsReceivedThenAnExceptionIsThrown() {

    ticketService.purchaseTickets(1L, null);

    thrown.expect(InvalidPurchaseException.class);
    thrown.expectMessage("Invalid ticket request");
  }

  @Test
  public void whenAnInvalidAccountIdIsReceivedThenAnExceptionIsThrown() {
    ticketService.purchaseTickets(null, new TicketTypeRequest(ADULT, 1));

    thrown.expect(InvalidPurchaseException.class);
    thrown.expectMessage("Invalid account id");
  }

  @Test
  public void whenAZeroAccountIdIsReceivedThenAnExceptionIsThrown() {

    ticketService.purchaseTickets(0L, new TicketTypeRequest(ADULT, 1));

    thrown.expect(InvalidPurchaseException.class);
    thrown.expectMessage("Invalid account id");
  }

}
