package com.project.dto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShippingLabelDto {
    private String vertical;
    private Long sequence;
    private Boolean priority;
    private String priorityName;
    private String tripId;
    private String carrier;
    private String shipmentDisplayId;
    private String awb;
    private String shipmentType;
    private String packageName;

    private String itemName;
    private String userDetails;
    private String sellerName = "Dummy Seller";
    private String sellerAddress = "Dummy Seller Address";
    private String returnAddress;

    private String dispatchFcAddress;

    private Integer dispatchPincode;
    private Integer returnPincode;
    private String footer;

    public static ShippingLabelDto from() {
        ShippingLabelDto dto = new ShippingLabelDto();
        Long shipmentPackagesCount = 4l;
        dto.vertical = "SALE";
        dto.sequence = 1l;
        dto.priority = true;
        dto.tripId = "TRIP-123456";
        dto.carrier = "CARRIER";
        dto.shipmentDisplayId = "SHP-123456";
        dto.awb = "AWB123456";
        dto.shipmentType = "DELIVERY";
        dto.itemName ="Sample Item 1";
        dto.packageName = "Dummy name";
        dto.returnAddress = "Dummy return address";
        dto.returnPincode = 460876;
        dto.userDetails = "Dummy User Details";
        dto.dispatchFcAddress = "Dummy address";
        dto.dispatchPincode = 451001; // hardcoded pincode
        dto.priorityName = "PRIORITY";
        dto.footer =
                String.format(
                        "For Warehouse use only: SKU: %s (Box %s of %s)",
                        "Dummy package name", 1, shipmentPackagesCount);
        return dto;
    }
}
