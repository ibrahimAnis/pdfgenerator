package com.project;

import com.project.dto.ShippingLabelDto;
import com.project.exceptions.DispatchDocumentException;
import com.project.util.ShippingLabelPdfGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws DispatchDocumentException {
        ShippingLabelPdfGenerator generator = new ShippingLabelPdfGenerator();
        List<ShippingLabelDto> dtos = new ArrayList<>();
        generator.generateShippingLabel(dtos,new File("sample.pdf"));
    }
}