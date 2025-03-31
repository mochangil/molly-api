package org.example.mollyapi.product.handler;

import com.github.f4b6a3.tsid.TsidCreator;
import java.util.concurrent.BlockingQueue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ExcelHandler extends DefaultHandler {

    // 병렬 처리 큐
    private final ReadOnlySharedStringsTable sharedStringsTable;
    private final BlockingQueue<List<String>> rowDataQueue;

    @Getter
    private final List<List<Long>> productItemIds = new ArrayList<>();

    // 현재 행의 데이터 저장 (각 셀의 문자열 값)
    private final List<String> currentRow = new ArrayList<>();
    private final StringBuilder cellValueBuilder = new StringBuilder();
    private boolean isSharedString = false;
    private boolean inValue = false;


    public ExcelHandler(
        BlockingQueue<List<String>> rowDataQueue,
        ReadOnlySharedStringsTable sharedStringsTable) {
        this.sharedStringsTable = sharedStringsTable;
        this.rowDataQueue = rowDataQueue;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        if ("row".equals(qName)) {
            currentRow.clear();
            currentRow.add(attributes.getValue("r"));
        }

        if ("c".equals(qName)) {
            inValue = true;
            cellValueBuilder.setLength(0);

            // "t" 속성을 확인하여 데이터 타입 판별
            String cellType = attributes.getValue("t");
            if ("s".equals(cellType)) {
                // "s"는 문자열(String) -> sharedStrings.xml을 참조해야 함
                isSharedString = true;
            } else {
                // 숫자형(Number) 또는 기본 값
                isSharedString = false;
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (inValue) {
            cellValueBuilder.append(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if ("c".equals(qName)) {
            inValue = false;
            String rawValue = cellValueBuilder.toString().trim();
            String cellValue;

            if (isSharedString) {
                int idx = Integer.parseInt(rawValue);
                cellValue = sharedStringsTable.getItemAt(idx).getString();
            } else {
                cellValue = rawValue;
            }

            currentRow.add(cellValue);
        }

        if ("row".equals(qName)) {
            int rowNumber = Integer.parseInt(currentRow.get(0));

            if (rowNumber == 1 || currentRow.size() < 9) {
                log.warn("유효하지 않은 row 감지 - rowNumber: {}, current_row_size: {}", rowNumber, currentRow.size());
                return;
            }

            try {
                Long tsid = TsidCreator.getTsid().toLong();
                currentRow.add(String.valueOf(tsid));

                productItemIds.add(List.of(tsid, (long) rowNumber));

                rowDataQueue.put(new ArrayList<>(currentRow)); // 큐에 행 데이터 추가
            } catch (InterruptedException e) {
                log.error("큐 삽입 중 인터럽트 발생 Error Message : {}", e.getMessage());
                Thread.currentThread().interrupt();
            }

            currentRow.clear();
        }
    }

    @Override
    public void startDocument(){
        try{
            log.info("Start : RowDataQueue_Size = {}", rowDataQueue.size());
        } catch (Exception e){
            log.error(" Excel Parsing Error : {} ", e.getMessage());
        }

    }

    @Override
    public void endDocument() {
        log.info("End : RowDataQueue_Size = {}", rowDataQueue.size());
    }

}