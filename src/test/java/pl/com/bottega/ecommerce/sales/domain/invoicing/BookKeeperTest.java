package pl.com.bottega.ecommerce.sales.domain.invoicing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.com.bottega.ecommerce.canonicalmodel.publishedlanguage.ClientData;
import pl.com.bottega.ecommerce.canonicalmodel.publishedlanguage.Id;
import pl.com.bottega.ecommerce.sales.domain.productscatalog.ProductData;
import pl.com.bottega.ecommerce.sales.domain.productscatalog.ProductDataBuilder;
import pl.com.bottega.ecommerce.sales.domain.productscatalog.ProductType;
import pl.com.bottega.ecommerce.sharedkernel.Money;

import java.awt.print.Book;
import java.time.LocalDateTime;
import java.util.Date;

@ExtendWith(MockitoExtension.class) class BookKeeperTest {

    private static final Id SAMPLE_CLIENT_ID = Id.generate();

    private static final String SAMPLE_CLIENT_NAME = "Jan Kowalski";

    private static final ClientData SAMPLE_CLIENT_DATA = new ClientData(SAMPLE_CLIENT_ID, SAMPLE_CLIENT_NAME);

    private BookKeeper keeper;

    @Mock private TaxPolicy taxPolicyMock;

    @Mock private InvoiceFactory invoiceFactoryMock;

    @BeforeEach void setUp() throws Exception {
        keeper = new BookKeeper(invoiceFactoryMock);
    }

    @Test public void shouldReturnInvoiceWithOneItemWhenRequestContainsOneItem() {
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

        Tax tax = new Tax(Money.ZERO, "Sample tax name");
        when(taxPolicyMock.calculateTax(any(ProductType.class), any(Money.class))).thenReturn(tax);

        Id sampleId = Id.generate();
        when(invoiceFactoryMock.create(SAMPLE_CLIENT_DATA)).thenReturn(new Invoice(sampleId, SAMPLE_CLIENT_DATA));

        int expectedItemCount = 1;
        // when
        Invoice invoice = keeper.issuance(requestWithOneItem, taxPolicyMock);

        //
        assertEquals(expectedItemCount, invoice.getItems().size());
    }

}
