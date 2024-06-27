package Dto;

// import lombok.AllArgsConstructor;
import lombok.Data;
import java.sql.Timestamp;



@Data
// @AllArgsConstructor
public class Transaction
{
    private String transactionId;
    private String productId;
    private String productName;
    private String productCategory;
    private double productPrice;
    private int productQuantity;
    private String productBrand;
    private double totalCost;
    private String currency;
    private String customerId;
    private Timestamp transactionDate;
    private String paymentMethod;
}
