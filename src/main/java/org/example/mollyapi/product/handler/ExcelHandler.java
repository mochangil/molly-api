package org.example.mollyapi.product.handler;

import com.github.f4b6a3.tsid.TsidCreator;
import java.util.concurrent.BlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.model.StylesTable;
import org.example.mollyapi.product.dto.request.ProductBulkItemReqDto;
import org.example.mollyapi.product.dto.request.ProductBulkReqDto;
import org.example.mollyapi.product.mapper.ProductItemMapper;
import org.example.mollyapi.product.mapper.ProductMapper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.example.mollyapi.product.dto.request.ProductBulkItemReqDto.createBulkProductItemReqDto;

@Slf4j
public class ExcelHandler extends DefaultHandler {

    // 병렬 처리 큐
    private final ReadOnlySharedStringsTable sharedStringsTable;
    private final BlockingQueue<List<String>> blockQueue;

    private Long firstProductItemId;
    private Long lastProductItemId;
    private final List<List<Long>> productItemIds = new ArrayList<>();

    // 현재 행의 데이터 저장 (각 셀의 문자열 값)
    private final List<String> currentRow = new ArrayList<>();
    private final StringBuilder cellValueBuilder = new StringBuilder();
    private boolean isSharedString = false;
    private boolean inValue = false;


    public ExcelHandler(
        BlockingQueue<List<String>> blockQueue,
        ReadOnlySharedStringsTable sharedStringsTable) {
        this.sharedStringsTable = sharedStringsTable;
        this.blockQueue = blockQueue;
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
                return;
            }

            try {
                Long tsid = TsidCreator.getTsid().toLong();
                currentRow.add(String.valueOf(tsid));

                productItemIds.add(List.of(tsid, (long) rowNumber));

                blockQueue.put(new ArrayList<>(currentRow)); // 큐에 행 데이터 추가
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            currentRow.clear();
        }
    }

    @Override
    public void startDocument() throws SAXException {
        log.info("문서 파싱 시작했다요");
    }

    @Override
    public void endDocument() {
        log.info("문서 파싱 끝났다요");
    }

    public List<List<Long>> getProductItemIds(){
        return productItemIds;
    }
}