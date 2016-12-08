package com.feraijo.main;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Class for working with excel file
 * Created by Feraijo on 29.11.2016.
 */
public class ExcelCell {
    private Sheet sheet;
    private String file = "C:\\Users\\Feraijo\\Documents\\GitHub\\Excel reader\\2.xls";
    private CellType strType = CellType.STRING;
    private Workbook wb;
    private static ExcelCell exc;
    private FileOutputStream fileOut;
    public final int ARTICUL_C = 1;
    public final int NAME_C = 2;
    public final int DESCR_C = 4;
    public final int PRICE_C = 5;
    public final int PHOTO_C = 8;


    public static ExcelCell getInstance(){
        if (exc == null)
            exc = new ExcelCell();
        return exc;
    }

    private ExcelCell() {
        try {
            InputStream inp = new FileInputStream(file);
            wb = WorkbookFactory.create(inp);
            sheet = wb.getSheetAt(0);
        } catch (InvalidFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set string to defined cell
     * @param row_n
     * @param desc
     */
    public void setStringVal(int row_n, String desc, int column){
        Row row = sheet.getRow(row_n);

        Cell cell_desc = row.getCell(column);

        if (cell_desc == null)
            cell_desc = row.createCell(column);

        cell_desc.setCellType(getStrType());
        cell_desc.setCellValue(desc);

        writeToExcel();
    }

    public Cell getCell(int colN, int rowN) {
        Row row = sheet.getRow(rowN);
        return row.getCell(colN);
    }


    private CellType getStrType() {
        return strType;
    }

    private void writeToExcel(){
        try {
            fileOut = new FileOutputStream(file);
            wb.write(fileOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeExcel(){
        try {
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getMaxRows(){
        return sheet.getPhysicalNumberOfRows();
    }

    public int getMaxCols(){
        int cols = 0;
        int tmp;

        for(int i = 0; i < 10 || i < getMaxRows(); i++) {
            if(sheet.getRow(i) != null) {
                tmp = sheet.getRow(i).getPhysicalNumberOfCells();
                if(tmp > cols) cols = tmp;
            }
        }
        return cols;
    }

}
