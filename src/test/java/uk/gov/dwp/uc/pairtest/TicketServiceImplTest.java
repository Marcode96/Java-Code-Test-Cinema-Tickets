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
  public ExpectedException expectedException = ExpectedException.none();

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
                                                new TicketTypeRequest(INFANT, 1),
                                                new TicketTypeRequest(ADULT, 1));

    verify(ticketPaymentService, times(1)).makePayment(accountIdForPaymentCaptor.capture(), amountToPayCaptor.capture());
    verify(seatReservationService, times(1)).reserveSeat(accountIdForSeatReservationCaptor.capture(),seatsReservedCaptor.capture());
    assertEquals("Account id is 1", 1L, accountIdForPaymentCaptor.getValue().longValue());
    assertEquals("Amount to pay is £90", 90, amountToPayCaptor.getValue().intValue());
    assertEquals("Account id is 1", 1L, accountIdForSeatReservationCaptor.getValue().longValue());
    assertEquals("The number of seats reserved is 4", 4, seatsReservedCaptor.getValue().intValue());
  }

  /** Business requirements */
  @Test
  public void whenAPaymentRequestForOneChildWithNoAdultIsMadeThenAnExceptionIsThrown() {

    expectedException.expect(InvalidPurchaseException.class);
    expectedException.expectMessage("An adult ticket purchase is required");
    ticketService.purchaseTickets(1L, new TicketTypeRequest(CHILD, 1));
  }

  @Test
  public void whenAPaymentRequestForOneInfantWithNoAdultIsMadeThenAnExceptionIsThrown() {

    expectedException.expect(InvalidPurchaseException.class);
    expectedException.expectMessage("An adult ticket purchase is required");
    ticketService.purchaseTickets(1L, new TicketTypeRequest(INFANT, 1));
  }

  @Test
  public void whenTheNumberOfRequestedTicketsIsZeroThenAnExceptionIsThrown() {

    expectedException.expect(InvalidPurchaseException.class);
    expectedException.expectMessage("Invalid number of tickets requested");
    ticketService.purchaseTickets(1L, new TicketTypeRequest(ADULT, 0));
  }

  @Test
  public void whenTheNumberOfRequestedTicketsIsNegativeThenAnExceptionIsThrown() {

    expectedException.expect(InvalidPurchaseException.class);
    expectedException.expectMessage("Invalid number of tickets requested");
    ticketService.purchaseTickets(1L, new TicketTypeRequest(ADULT, -1));
  }

  @Test
  public void whenTheNumberOfRequestedTicketsPerPersonIsMoreThanTheLimitThenAnExceptionIsThrown() {

    expectedException.expect(InvalidPurchaseException.class);
    expectedException.expectMessage("Too many tickets");
    ticketService.purchaseTickets(1L, new TicketTypeRequest(ADULT, 26));
  }

  @Test
  public void whenTheNumberOfRequestedTicketsIsMoreThanTheLimitThenAnExceptionIsThrown() {

    expectedException.expect(InvalidPurchaseException.class);
    expectedException.expectMessage("Too many tickets");
    ticketService.purchaseTickets(1L, new TicketTypeRequest(ADULT, 25),
                                                new TicketTypeRequest(INFANT, 1),
                                                new TicketTypeRequest(CHILD, 1));
  }

  /** Invalid request tests */
  @Test
  public void whenTheTicketTypeIsInvalidThenAnExceptionIsThrown() {

    expectedException.expect(InvalidPurchaseException.class);
    expectedException.expectMessage("Invalid type of ticket requested");
    ticketService.purchaseTickets(1L, new TicketTypeRequest(null, 1));
  }

  @Test
  public void whenOneValidAndOneInvalidRequestAreReceivedThenAnExceptionIsThrown() {

    expectedException.expect(InvalidPurchaseException.class);
    expectedException.expectMessage("Invalid ticket request");
    ticketService.purchaseTickets(1L, new TicketTypeRequest(ADULT, 1), null);
  }

  @Test
  public void whenANullRequestIsReceivedThenAnExceptionIsThrown() {

    expectedException.expect(InvalidPurchaseException.class);
    expectedException.expectMessage("Invalid ticket request");
    ticketService.purchaseTickets(1L, null);
  }

  @Test
  public void whenAnInvalidAccountIdIsReceivedThenAnExceptionIsThrown() {

    expectedException.expect(InvalidPurchaseException.class);
    expectedException.expectMessage("Invalid account id");
    ticketService.purchaseTickets(null, new TicketTypeRequest(ADULT, 1));
  }

  @Test
  public void whenAZeroAccountIdIsReceivedThenAnExceptionIsThrown() {

    expectedException.expect(InvalidPurchaseException.class);
    expectedException.expectMessage("Invalid account id");
    ticketService.purchaseTickets(0L, new TicketTypeRequest(ADULT, 1));
  }

  /** Edge cases */
  @Test
  public void whenThereAreMoreInfantsRequestsThenAdultsThenAnExceptionIsThrown() {

    expectedException.expect(InvalidPurchaseException.class);
    expectedException.expectMessage("There are not enough adults for infants to seat");
    ticketService.purchaseTickets(1L, new TicketTypeRequest(ADULT, 1),
                                                new TicketTypeRequest(INFANT, 1),
                                                new TicketTypeRequest(INFANT, 1));
  }

  @Test
  public void whenThereAreMoreInfantsThenAdultsThenAnExceptionIsThrown() {

    expectedException.expect(InvalidPurchaseException.class);
    expectedException.expectMessage("There are not enough adults for infants to seat");
    ticketService.purchaseTickets(1L, new TicketTypeRequest(ADULT, 1),
                                                new TicketTypeRequest(INFANT, 2));
  }

}
