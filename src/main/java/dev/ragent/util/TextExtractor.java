package dev.ragent.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.apache.poi.hslf.usermodel.HSLFShape;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextShape;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Utility class for extracting text from various document formats
 */
public class TextExtractor {

    /**
     * Extract text from a DOCX file
     */
    public static String extractFromDocx(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
                XWPFDocument document = new XWPFDocument(fis);
                XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    /**
     * Extract text from a PPTX file
     */
    public static String extractFromPptx(File file) throws IOException {
        StringBuilder text = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(file);
                XMLSlideShow ppt = new XMLSlideShow(fis)) {

            int slideNumber = 1;
            for (XSLFSlide slide : ppt.getSlides()) {
                text.append("=== Slide ").append(slideNumber++).append(" ===\n");

                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) shape;
                        String shapeText = textShape.getText();
                        if (shapeText != null && !shapeText.trim().isEmpty()) {
                            text.append(shapeText).append("\n");
                        }
                    }
                }
                text.append("\n");
            }
        }

        return text.toString();
    }

    /**
     * Extract text from a PPT file (older binary format)
     */
    public static String extractFromPpt(File file) throws IOException {
        StringBuilder text = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(file);
                HSLFSlideShow ppt = new HSLFSlideShow(fis)) {

            int slideNumber = 1;
            for (HSLFSlide slide : ppt.getSlides()) {
                text.append("=== Slide ").append(slideNumber++).append(" ===\n");

                for (HSLFShape shape : slide.getShapes()) {
                    if (shape instanceof HSLFTextShape) {
                        HSLFTextShape textShape = (HSLFTextShape) shape;
                        String shapeText = textShape.getText();
                        if (shapeText != null && !shapeText.trim().isEmpty()) {
                            text.append(shapeText).append("\n");
                        }
                    }
                }
                text.append("\n");
            }
        }

        return text.toString();
    }

    /**
     * Extract text from a PDF file
     */
    public static String extractFromPdf(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Read plain text from a TXT file
     */
    public static String readTextFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    /**
     * Save text to a file
     */
    public static void saveTextToFile(String text, File file) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(text);
        }
    }

    /**
     * Extract text based on file extension
     */
    public static String extractText(File file) throws IOException {
        String fileName = file.getName().toLowerCase();

        if (fileName.endsWith(".docx")) {
            return extractFromDocx(file);
        } else if (fileName.endsWith(".pptx")) {
            return extractFromPptx(file);
        } else if (fileName.endsWith(".ppt")) {
            return extractFromPpt(file);
        } else if (fileName.endsWith(".pdf")) {
            return extractFromPdf(file);
        } else if (fileName.endsWith(".txt")) {
            return readTextFile(file);
        } else {
            throw new IllegalArgumentException("Unsupported file format: " + fileName);
        }
    }

    /**
     * Get the appropriate text file name for a source file
     */
    public static String getTextFileName(String originalFileName) {
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return originalFileName.substring(0, lastDotIndex) + ".txt";
        }
        return originalFileName + ".txt";
    }
}
