package com.project.util;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import com.project.dto.ShippingLabelDto;
import com.project.exceptions.DispatchDocumentException;
import lombok.extern.slf4j.Slf4j;
import org.krysalis.barcode4j.impl.code128.Code128Bean;
import org.krysalis.barcode4j.output.bitmap.BitmapCanvasProvider;
import org.krysalis.barcode4j.tools.UnitConv;

@Slf4j
public class ShippingLabelPdfGenerator {

    public File generateShippingLabel(List<ShippingLabelDto> dtos, File file) throws DispatchDocumentException {
        int startCount = 0;
        int endCount = 0;
        File page = new File("label_" + startCount + ".pdf");
        List<File> pages = new ArrayList<>();
        List<ShippingLabelDto> dtoPages = new ArrayList<>();
        try {
            FileOutputStream stream = new FileOutputStream(page);
            for (ShippingLabelDto dto : dtos) {
                if (endCount != 0 && endCount % 4 == 0) {
                    this.createPage(dtoPages, stream, startCount);
                    pages.add(page);
                    stream.flush();
                    stream.close();
                    page = new File("label_" + endCount + ".pdf");
                    stream = new FileOutputStream(page);
                    dtoPages = new ArrayList<>();
                    startCount = endCount;
                }
                dtoPages.add(dto);
                endCount++;
            }
            this.createPage(dtoPages, stream, startCount);
            pages.add(page);
            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.flush();
            outputStream.close();
            System.out.println("Shipping label is generated");
        } catch (IOException | DocumentException ex) {
            System.out.println("Something went wrong in shipping label generator....\n" + ex.getMessage());
            if (file.exists()) file.delete();
            throw new DispatchDocumentException(ex.getMessage());
        } finally {
            for (File tmp : pages) tmp.delete();
        }
        return file;
    }

    private void createPage(List<ShippingLabelDto> dtos, FileOutputStream stream, int startCount)
            throws IOException, DocumentException {
        Document document = new Document(PageSize.A4);

        PdfWriter writer = PdfWriter.getInstance(document, stream);
        document.open();
        PdfContentByte cb = writer.getDirectContent();

        for (ShippingLabelDto dto : dtos) {
            Integer labelId = startCount + 1;

            PdfPTable table = new PdfPTable(3);
            table.setTotalWidth((PageSize.A4.getWidth() / 2.0f) - 30.0f);
            table.setLockedWidth(true);

            // first row
            table.addCell(createPlaceholder(dto.getVertical().toString()));
            table.addCell(createPriority(dto.getPriority()));
            table.addCell(createTrip(dto));
            table.addCell(createSequence(cb, dto));
            table.addCell(createPackage(dto));
            table.addCell(createShippingAddress(dto));
            table.addCell(createVendorAddress(dto));
            table.addCell(createFooter(dto));

            float labelPageWidth = PageSize.A4.getWidth() / 2.0f;
            float labelWidth = table.getTotalWidth();
            float middleX = (labelPageWidth - labelWidth) / 2.0f;

            float labelPageHeight = PageSize.A4.getHeight() / 2.0f;
            float labelHeight = table.getTotalHeight();
            float middleY = (labelPageHeight - labelHeight) / 2.0f;

            switch (startCount % 4) {
                case 0:
                    table.writeSelectedRows(0, -1, middleX, PageSize.A4.getHeight() - middleY, cb);
                    break;
                case 1:
                    table.writeSelectedRows(
                            0,
                            -1,
                            (PageSize.A4.getWidth() / 2.0f + middleX),
                            PageSize.A4.getHeight() - middleY,
                            cb);
                    break;
                case 2:
                    table.writeSelectedRows(0, -1, middleX, table.getTotalHeight() + 30.0f, cb);
                    break;

                case 3:
                    table.writeSelectedRows(
                            0, -1, (PageSize.A4.getWidth() / 2.0f + middleX), table.getTotalHeight() + 30.0f, cb);
                    break;
            }
            startCount++;
        }
        document.close();
    }

    private static PdfPCell createPlaceholder(String vertical) {
        Font font = new Font(FontFamily.HELVETICA, 12, Font.ITALIC);
        PdfPCell cell = new PdfPCell(new Phrase(vertical, font));
        cell.setFixedHeight(20);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(BaseColor.BLACK);
        cell.setColspan(2);
        return cell;
    }

    public static BufferedImage generateCode128BarcodeImage(String barcodeText) {

        Code128Bean barcodeGenerator = new Code128Bean();
        barcodeGenerator.setBarHeight(240);
        // barcodeGenerator.setVerticalQuietZone(5);

        int dpi = 2;
        barcodeGenerator.doQuietZone(true);

        barcodeGenerator.setModuleWidth(UnitConv.in2mm(1.0f / dpi));
        BitmapCanvasProvider canvas =
                new BitmapCanvasProvider(dpi, BufferedImage.TYPE_BYTE_BINARY, false, 0);

        barcodeGenerator.setFontSize(0);
        barcodeGenerator.generateBarcode(canvas, barcodeText);
        BufferedImage bi = canvas.getBufferedImage();
        return bi;
    }

    private static PdfPCell createPriority(Boolean priority) {
        Font font = new Font(FontFamily.HELVETICA, 12, Font.BOLD);
        PdfPCell cell;
        if (priority) cell = new PdfPCell(new Phrase("PRIORITY", font));
        else cell = new PdfPCell(new Phrase("STANDARD", font));
        cell.setFixedHeight(20);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBorderColor(BaseColor.BLACK);
        return cell;
    }

    private static PdfPCell createTrip(ShippingLabelDto dto) {
        Font font = new Font(FontFamily.HELVETICA, 8);
        float[] widthPercentage = {30.0f, 70.0f};
        PdfPTable cellTable = new PdfPTable(widthPercentage);

        PdfPCell innerCell = new PdfPCell(new Phrase("Trip ID: ", font));
        innerCell.setBorder(Rectangle.NO_BORDER);
        innerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cellTable.addCell(innerCell);

        innerCell = new PdfPCell(new Phrase(dto.getTripId().toString(), font));
        innerCell.setBorder(Rectangle.NO_BORDER);
        innerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cellTable.addCell(innerCell);

        innerCell = new PdfPCell(new Phrase("Carrier: ", font));
        innerCell.setBorder(Rectangle.NO_BORDER);
        innerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cellTable.addCell(innerCell);

        innerCell = new PdfPCell(new Phrase(dto.getCarrier(), font));
        innerCell.setBorder(Rectangle.NO_BORDER);
        innerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cellTable.addCell(innerCell);

        PdfPCell cell = new PdfPCell(cellTable);
        cell.setPadding(0);
        innerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setFixedHeight(40);
        cell.setBorderColor(BaseColor.BLACK);
        cell.setColspan(2);
        return cell;
    }

    private static PdfPCell createAwb(PdfContentByte cb, ShippingLabelDto dto) throws DispatchDocumentException {
        File outputFile = new File("./saved.png");
        try {
            Font font = new Font(FontFamily.HELVETICA, 10);
            float[] widthPercentage = {20.0f, 40.0f, 40.0f};
            PdfPTable cellTable = new PdfPTable(widthPercentage);

            PdfPCell innerCell = new PdfPCell(new Phrase("AWB: ", font));
            innerCell.setBorder(Rectangle.NO_BORDER);
            innerCell.setVerticalAlignment(Element.ALIGN_TOP);
            innerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cellTable.addCell(innerCell);

            String awb = dto.getAwb();
            innerCell = new PdfPCell(new Phrase(awb, font));
            innerCell.setColspan(2);
            innerCell.setBorder(Rectangle.NO_BORDER);
            innerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            innerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cellTable.addCell(innerCell);

            BufferedImage image = generateCode128BarcodeImage(awb);
            ImageIO.write(image, "png", outputFile);

            Image img = Image.getInstance(outputFile.getPath());

            innerCell = new PdfPCell(img);
            innerCell.setBorder(Rectangle.NO_BORDER);
            innerCell.setColspan(3);
            innerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            innerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cellTable.addCell(innerCell);
            PdfPCell cell = new PdfPCell(cellTable);
            cell.setPadding(0);
            cell.setColspan(3);
            innerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setFixedHeight(50);
            cell.setBorderColor(BaseColor.BLACK);
            return cell;
        } catch (IOException | BadElementException e) {
            System.out.println(e.getMessage());
            throw new DispatchDocumentException(e.getMessage());
        } finally {
            if (outputFile.exists()) outputFile.delete();
        }
    }

    private static PdfPCell createSequence(PdfContentByte cb, ShippingLabelDto dto) {

        Font font = new Font(FontFamily.HELVETICA, 10);
        PdfPTable cellTable = new PdfPTable(2);
        PdfPCell seqCell = new PdfPCell();
        seqCell.setBorder(Rectangle.NO_BORDER);
        seqCell.setRowspan(2);
        Phrase p1 = new Phrase("       Seq:       ", font);
        seqCell.addElement(p1);
        seqCell.setVerticalAlignment(Element.ALIGN_TOP);
        seqCell.setHorizontalAlignment(Element.ALIGN_MIDDLE);
        cellTable.addCell(seqCell);

        Font seqfont = new Font(FontFamily.HELVETICA, 15, Font.BOLD);
        PdfPCell innerCell = new PdfPCell(new Phrase(dto.getSequence().toString(), seqfont));
        innerCell.setBorder(Rectangle.NO_BORDER);
        innerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        innerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cellTable.addCell(innerCell);

        PdfPCell cell = new PdfPCell(cellTable);
        cell.setPadding(0);
        innerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setCellEvent(
                (cell1, position, canvases) -> {
                    PdfContentByte cb1 = canvases[PdfPTable.LINECANVAS];
                    System.out.println("Chala");
                    double xLeft = position.getLeft();
                    double yLeft = position.getBottom();
                    double xRight = position.getRight();
                    double yRight = position.getTop();
                    double cellWidth = xRight - xLeft;
                    double cellHeight = yRight - yLeft;
                    float xCor = (float)(xLeft + ((cellWidth * 2) / 3.0f) + 10.0f);
                    float yCor = (float)(yLeft + (cellHeight / 2.0f) - 3.0f);

                    cb1.circle(xCor, yCor, 15.0f);
                    cb1.stroke();
                });
        cell.setFixedHeight(40);
        cell.setBorderColor(BaseColor.BLACK);
        return cell;
    }

    private static PdfPCell createPackage(ShippingLabelDto dto) {

        Font font = new Font(FontFamily.HELVETICA, 8);
        float[] widthPercentage = {70.0f, 30.0f};
        PdfPTable cellTable = new PdfPTable(widthPercentage);

        PdfPCell shipmentCell =
                new PdfPCell(new Phrase("Shipment ID: " + dto.getShipmentDisplayId(), font));
        shipmentCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        shipmentCell.setBorder(Rectangle.NO_BORDER);
        cellTable.addCell(shipmentCell);

        font = new Font(FontFamily.HELVETICA, 8, Font.BOLD);
        PdfPCell typeCell = new PdfPCell(new Phrase(dto.getShipmentType().toString(), font));
        typeCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        typeCell.setBorder(Rectangle.NO_BORDER);
        cellTable.addCell(typeCell);

        PdfPCell cell = new PdfPCell(new Phrase(dto.getItemName()));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setBorderColor(BaseColor.BLACK);
        cell.setColspan(2);
        cell.setNoWrap(false);
        cell.setBorder(Rectangle.NO_BORDER);
        cellTable.addCell(cell);

        PdfPCell innerCell = new PdfPCell(cellTable);
        innerCell.setColspan(3);
        return innerCell;
    }

    private static PdfPCell createShippingAddress(ShippingLabelDto dto) {

        Font font = new Font(FontFamily.HELVETICA, 8);

        PdfPTable cellTable = new PdfPTable(1);

        PdfPCell innerCell = new PdfPCell(new Phrase("Shipping Address: ", font));
        innerCell.setBorder(Rectangle.NO_BORDER);
        innerCell.setVerticalAlignment(Element.ALIGN_TOP);
        innerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cellTable.addCell(innerCell);

        innerCell = new PdfPCell(new Phrase(dto.getUserDetails(), font));
        innerCell.setBorder(Rectangle.NO_BORDER);
        innerCell.setVerticalAlignment(Element.ALIGN_TOP);
        innerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cellTable.addCell(innerCell);

        innerCell =
                new PdfPCell(
                        new Phrase(dto.getUserDetails(), font));
        innerCell.setRowspan(2);
        innerCell.setBorder(Rectangle.NO_BORDER);
        innerCell.setVerticalAlignment(Element.ALIGN_TOP);
        innerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cellTable.addCell(innerCell);

        PdfPTable contactTable = new PdfPTable(2);
        innerCell = new PdfPCell(new Phrase("Contact: " + dto.getUserDetails(), font));
        innerCell.setBorder(Rectangle.NO_BORDER);
        innerCell.setVerticalAlignment(Element.ALIGN_TOP);
        innerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        contactTable.addCell(innerCell);

        innerCell =
                new PdfPCell(
                        new Phrase(
                                "PIN:" + dto.getUserDetails(), font));
        innerCell.setBorder(Rectangle.NO_BORDER);
        innerCell.setVerticalAlignment(Element.ALIGN_TOP);
        innerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        contactTable.addCell(innerCell);

        innerCell = new PdfPCell(contactTable);
        innerCell.setBorder(Rectangle.NO_BORDER);
        cellTable.addCell(innerCell);

        PdfPCell cell = new PdfPCell(cellTable);
        cell.setColspan(3);
        return cell;
    }

    private static PdfPCell createVendorAddress(ShippingLabelDto dto) {

        Font font = new Font(FontFamily.HELVETICA, 8);

        PdfPTable sellerAddressTable = new PdfPTable(1);

        PdfPCell innerCell = new PdfPCell(new Phrase("Seller Name And Address: ", font));
        innerCell.setBorder(Rectangle.NO_BORDER);
        innerCell.setVerticalAlignment(Element.ALIGN_TOP);
        innerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        sellerAddressTable.addCell(innerCell);

        innerCell = new PdfPCell(new Phrase(dto.getSellerName() + " " + dto.getSellerAddress(), font));
        innerCell.setRowspan(2);
        innerCell.setBorder(Rectangle.NO_BORDER);
        innerCell.setVerticalAlignment(Element.ALIGN_TOP);
        innerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        sellerAddressTable.addCell(innerCell);

        PdfPTable shippedByTable = new PdfPTable(1);
        innerCell = new PdfPCell(new Phrase("Shipped By", font));
        innerCell.setBorder(Rectangle.NO_BORDER);
        innerCell.setVerticalAlignment(Element.ALIGN_TOP);
        innerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        shippedByTable.addCell(innerCell);

        innerCell = new PdfPCell(new Phrase(dto.getDispatchFcAddress(), font));
        innerCell.setBorder(Rectangle.NO_BORDER);
        innerCell.setRowspan(2);
        innerCell.setVerticalAlignment(Element.ALIGN_TOP);
        innerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        shippedByTable.addCell(innerCell);

        innerCell = new PdfPCell(new Phrase("PIN: " + dto.getDispatchPincode(), font));
        innerCell.setBorder(Rectangle.NO_BORDER);
        innerCell.setVerticalAlignment(Element.ALIGN_TOP);
        innerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        shippedByTable.addCell(innerCell);

        PdfPTable vendorTable = new PdfPTable(1);
        innerCell = new PdfPCell(new Phrase("Return Address", font));
        innerCell.setBorder(Rectangle.NO_BORDER);
        innerCell.setVerticalAlignment(Element.ALIGN_TOP);
        innerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        vendorTable.addCell(innerCell);

        innerCell = new PdfPCell(new Phrase(dto.getReturnAddress(), font));
        innerCell.setBorder(Rectangle.NO_BORDER);
        innerCell.setRowspan(2);
        innerCell.setVerticalAlignment(Element.ALIGN_TOP);
        innerCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        vendorTable.addCell(innerCell);

        innerCell = new PdfPCell(new Phrase("PIN: " + dto.getReturnPincode(), font));
        innerCell.setBorder(Rectangle.NO_BORDER);
        innerCell.setVerticalAlignment(Element.ALIGN_TOP);
        innerCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vendorTable.addCell(innerCell);

        PdfPCell sCell = new PdfPCell(sellerAddressTable);
        sCell.setBorder(Rectangle.NO_BORDER);

        PdfPCell vCell = new PdfPCell(vendorTable);
        vCell.setBorder(Rectangle.NO_BORDER);

        PdfPCell dCell = new PdfPCell(shippedByTable);
        dCell.setBorder(Rectangle.NO_BORDER);

        PdfPTable table = new PdfPTable(1);
        vCell.setBorder(Rectangle.NO_BORDER);
        table.addCell(sCell);
        table.addCell(dCell);
        table.addCell(vCell);

        innerCell = new PdfPCell(table);
        innerCell.setColspan(3);
        return innerCell;
    }

    public static PdfPCell createFooter(ShippingLabelDto dto) {

        Font font = new Font(FontFamily.HELVETICA, 8);
        PdfPCell cell =
                new PdfPCell(new Phrase("For Warehouse use only: \nSKU: " + dto.getPackageName(), font));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setFixedHeight(20);
        cell.setBorderColor(BaseColor.BLACK);
        cell.setColspan(3);
        return cell;
    }
}
