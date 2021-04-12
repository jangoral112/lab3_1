package pl.com.bottega.ecommerce.sales.domain.invoicing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.com.bottega.ecommerce.canonicalmodel.publishedlanguage.ClientData;
import pl.com.bottega.ecommerce.canonicalmodel.publishedlanguage.Id;
import pl.com.bottega.ecommerce.sales.domain.client.Client;
import pl.com.bottega.ecommerce.sales.domain.productscatalog.ProductData;
import pl.com.bottega.ecommerce.sales.domain.productscatalog.ProductDataBuilder;
import pl.com.bottega.ecommerce.sales.domain.productscatalog.ProductType;
import pl.com.bottega.ecommerce.sharedkernel.Money;

import java.awt.print.Book;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class BookKeeperTest {

    private static final Id SAMPLE_CLIENT_ID = Id.generate();

    private static final String SAMPLE_CLIENT_NAME = "Jan Kowalski";

    private static final ClientData SAMPLE_CLIENT_DATA = new ClientData(SAMPLE_CLIENT_ID, SAMPLE_CLIENT_NAME);

    private static final Tax SAMPLE_TAX = new Tax(Money.ZERO, "Sample tax name");

    private static final Id SAMPLE_INVOICE_ID = Id.generate();

    private BookKeeper keeper;

    @Mock
    private TaxPolicy taxPolicyMock;

    @Mock
    private InvoiceFactory invoiceFactoryMock;

    @BeforeEach
    void setUp() throws Exception {
        keeper = new BookKeeper(invoiceFactoryMock);
    }

    @Test
    public void shouldReturnInvoiceWithOneItemWhenRequestContainsOneItem() {
        // given
        InvoiceRequest requestWithOneItem = new InvoiceRequest(SAMPLE_CLIENT_DATA);

        ProductData productDataDummy = new ProductDataBuilder().withProductId(Id.generate())
                                                               .withPrice(Money.ZERO)
                                                               .withName("Sample product name")
                                                               .withProductType(ProductType.STANDARD)
                                                               .withSnapshotDate(null)
                                                               .build();

        RequestItem requestItemDummy = new RequestItem(productDataDummy, 1, Money.ZERO);
        requestWithOneItem.add(requestItemDummy);

        when(taxPolicyMock.calculateTax(any(ProductType.class), any(Money.class))).thenReturn(SAMPLE_TAX);

        Invoice sampleInvoice = new Invoice(SAMPLE_INVOICE_ID, SAMPLE_CLIENT_DATA);
        when(invoiceFactoryMock.create(SAMPLE_CLIENT_DATA)).thenReturn(sampleInvoice);

        int expectedItemCount = 1;

        // when
        Invoice invoice = keeper.issuance(requestWithOneItem, taxPolicyMock);

        // then
        assertEquals(expectedItemCount, invoice.getItems()
                                               .size());
    }

    @Test
    public void shouldInvokeCalculateTaxWhenRequestContainsTwoItemsWithCorrespondingParameters() {

        // given

        InvoiceRequest requestWithTwoItems = new InvoiceRequest(SAMPLE_CLIENT_DATA);

        ProductData firstProduct = new ProductDataBuilder().withProductId(Id.generate())
                                                           .withPrice(Money.ZERO)
                                                           .withName("Sample product name")
                                                           .withProductType(ProductType.STANDARD)
                                                           .withSnapshotDate(null)
                                                           .build();

        Money firstItemTotalCost = new Money(10, Money.DEFAULT_CURRENCY);
        RequestItem firstRequestItem = new RequestItem(firstProduct, 1, firstItemTotalCost);

        ProductData secondProduct = new ProductDataBuilder().withProductId(Id.generate())
                                                            .withPrice(Money.ZERO)
                                                            .withName("Sample product name")
                                                            .withProductType(ProductType.DRUG)
                                                            .withSnapshotDate(null)
                                                            .build();

        Money secondItemTotalCost = new Money(12, Money.DEFAULT_CURRENCY);
        RequestItem secondRequestItem = new RequestItem(secondProduct, 2, secondItemTotalCost);

        requestWithTwoItems.add(firstRequestItem);
        requestWithTwoItems.add(secondRequestItem);

        when(taxPolicyMock.calculateTax(any(ProductType.class), any(Money.class))).thenReturn(SAMPLE_TAX);

        Invoice sampleInvoice = new Invoice(SAMPLE_INVOICE_ID, SAMPLE_CLIENT_DATA);
        when(invoiceFactoryMock.create(SAMPLE_CLIENT_DATA)).thenReturn(sampleInvoice);

        ArgumentCaptor<ProductType> productTypeCaptor = ArgumentCaptor.forClass(ProductType.class);
        ArgumentCaptor<Money> moneyCaptor = ArgumentCaptor.forClass(Money.class);

        int expectedInvocationNumber = 2;

        // when
        keeper.issuance(requestWithTwoItems, taxPolicyMock);

        // then
        verify(taxPolicyMock, times(expectedInvocationNumber)).calculateTax(productTypeCaptor.capture(), moneyCaptor.capture());

        List<ProductType> capturedProductTypes = productTypeCaptor.getAllValues();
        List<Money> capturedMoney = moneyCaptor.getAllValues();

        assertEquals(firstProduct.getType(), capturedProductTypes.get(0));
        assertEquals(firstItemTotalCost, capturedMoney.get(0));

        assertEquals(secondProduct.getType(), capturedProductTypes.get(1));
        assertEquals(secondItemTotalCost, capturedMoney.get(1));
    }

    @Test
    public void shouldReturnInvoiceWithNoItemsWhenRequestDoesNotContainAnyItem() {
        // given
        InvoiceRequest requestWithNoItems = new InvoiceRequest(SAMPLE_CLIENT_DATA);

        Invoice sampleInvoice = new Invoice(SAMPLE_INVOICE_ID, SAMPLE_CLIENT_DATA);
        when(invoiceFactoryMock.create(SAMPLE_CLIENT_DATA)).thenReturn(sampleInvoice);

        int expectedItemCount = 0;

        // when
        Invoice invoice = keeper.issuance(requestWithNoItems, taxPolicyMock);

        // then
        assertEquals(expectedItemCount, invoice.getItems()
                                               .size());

    }

    @Test
    public void shouldReturnReturnInvoiceWithItemCorrespondingToRequestItem() {
        // given
        InvoiceRequest requestWithOneItem = new InvoiceRequest(SAMPLE_CLIENT_DATA);

        ProductData productData = new ProductDataBuilder().withProductId(Id.generate())
                                                          .withPrice(Money.ZERO)
                                                          .withName("Sample product name")
                                                          .withProductType(ProductType.STANDARD)
                                                          .withSnapshotDate(null)
                                                          .build();

        Money itemTotalCost = new Money(33, Money.DEFAULT_CURRENCY);
        int requestItemQuantity = 1;
        RequestItem requestItemDummy = new RequestItem(productData, requestItemQuantity, itemTotalCost);
        requestWithOneItem.add(requestItemDummy);

        when(taxPolicyMock.calculateTax(any(ProductType.class), any(Money.class))).thenReturn(SAMPLE_TAX);

        Invoice sampleInvoice = new Invoice(SAMPLE_INVOICE_ID, SAMPLE_CLIENT_DATA);
        when(invoiceFactoryMock.create(SAMPLE_CLIENT_DATA)).thenReturn(sampleInvoice);

        // when
        Invoice invoice = keeper.issuance(requestWithOneItem, taxPolicyMock);

        // then
        assertEquals(productData, invoice.getItems()
                                         .get(0)
                                         .getProduct());
        assertEquals(requestItemQuantity, invoice.getItems()
                                                 .get(0)
                                                 .getQuantity());
        assertEquals(itemTotalCost, invoice.getItems()
                                           .get(0)
                                           .getNet());
        assertEquals(SAMPLE_TAX, invoice.getItems()
                                        .get(0)
                                        .getTax());
    }

    @Test
    public void shouldInvokeInvoiceFactoryCreateWithClientDataFromInvoiceRequest() {
        // given
        InvoiceRequest requestWithClientData = new InvoiceRequest(SAMPLE_CLIENT_DATA);

        ArgumentCaptor<ClientData> clientDataCaptor = ArgumentCaptor.forClass(ClientData.class);

        // when
        keeper.issuance(requestWithClientData, taxPolicyMock);

        // then
        verify(invoiceFactoryMock, times(1)).create(clientDataCaptor.capture());

        assertEquals(SAMPLE_CLIENT_DATA, clientDataCaptor.getValue());
    }

    @Test
    public void shouldNotInteractWithTaxPolicyWhenNoItemsInInvoiceRequest() {
        // given
        InvoiceRequest requestWithNoItems = new InvoiceRequest(SAMPLE_CLIENT_DATA);

        Invoice sampleInvoice = new Invoice(SAMPLE_INVOICE_ID, SAMPLE_CLIENT_DATA);
        when(invoiceFactoryMock.create(SAMPLE_CLIENT_DATA)).thenReturn(sampleInvoice);

        // when
        keeper.issuance(requestWithNoItems, taxPolicyMock);

        // then
        verifyNoInteractions(taxPolicyMock);
    }
}
