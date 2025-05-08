package com.atguigu.maven;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Hello world!
 */
public class App {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    public static void main(String[] args) throws IOException, JSONException {

        App app = new App();
        List<Map<String, String>> list = new ArrayList();
        Map<String, String> map = new HashMap<>();
        map.put("imageDocCode", "6060451");
        map.put("imageCnt", "3");
        map.put("imageId", "xxxyyy.txt");
        list.add(map);
        System.out.println("****" + app.reqDocRead(list));
    }

    // 성공, 실패 집계 변수
    int SuccessCnt = 1;
    int Err400Cnt = 0;
    int Err500Cnt = 0;

    String lawDocCode = "6060031,6060137,6060451";
    String legalfeeDocCode = "6060450";
    String licenseDocCode = "6060453";
    String BankBookDocCode = "5657586,5657574,5560435,6060362";

    public String readRecResult() throws IOException {

        StringBuffer readBuffer = new StringBuffer();

        BufferedReader br = new BufferedReader(new FileReader("src/lominrecog.txt"));

        String line = null;

        while ((line = br.readLine()) != null) {
            // System.out.println(line);
            readBuffer.setLength(0);
            readBuffer.append(line);
        }

        br.close();
        return readBuffer.toString();
    }

    public Map<String, Object> reqDocRead(List<Map<String, String>> DocList) throws JSONException, JsonMappingException, JsonProcessingException {
        //JSONObject jsonObj = new JSONObject();

        LOGGER.info("########## reqDocRead start ##########");

        Map<String, Object> RecogList = new HashMap<String, Object>();

        Map<String, Object> OcrProcSts = new HashMap<String, Object>();
        //Map<String, Object> OcrProcData = new HashMap<String, Object>();
        List<Map<String, Object>> OcrProcData = new ArrayList<Map<String, Object>>();

        Map<String, Object> tmpMap = new HashMap<String, Object>();
        Map<String, Object> tmpMapRecog = new HashMap<String, Object>();


        // OCR 처리결과 상태 - 로민 ocr 시작시간 추가
        LocalDateTime fromDate = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        OcrProcSts.put("reqDttm", fromDate.format(formatter));

        String strRtn = "";

        for (Map<String, String> ReqDoc : DocList) {
            LOGGER.info("[reqDocRead] lominOcr 호출 파라미터 : " + ReqDoc);
            // strRtn += lominOcr(ReqDoc);
        }

        //ReqDoc.get("filePath") DocList.get(0).get("imageDocCode")
        //LOGGER.info("lominOcr 완료 rtn ===>" + strRtn.toString());

        // OCR 처리결과 상태 - 로민 ocr 완료시간 추가
        LocalDateTime toDate = LocalDateTime.now();
        OcrProcSts.put("resDttm", toDate.format(formatter));

        // OCR 처리결과 상태 - 로민 ocr 응답시간
        OcrProcSts.put("resTime", String.valueOf(Duration.between(fromDate, toDate).getSeconds()));

        // OCR 처리결과 상태 - DocRecog 시작시간 저장
        fromDate = LocalDateTime.now();
        OcrProcSts.put("docrecReqDttm", fromDate.format(formatter));

        String strFilePath = "";

        //int nHDPI = 0;
        //int nWDPI = 0;

        // 4. DocRecog 호출
        int nRegocCnt = 0;
        int nErrCnt = 0;

        try {
            // SuccessCnt가 0보다 큰 경우(ocr 인식요청 성공시 SuccessCnt++) 만 DocRecog 처리
            if (SuccessCnt > 0) {
                // 인식처리 성공상태 추가
                OcrProcSts.put("stsCd", "1");

                LOGGER.info("[reqDocRead] docRecognizer start");
                // 5. DocRecog 리턴값 편집 (분석설계 필요함)
                // OcrProcData 는 DocRecog 리턴값으로 생성

                /*
                 * DocRecognizer 인식처리
                 * 1. writeFile 로 인식값 파일저장
                 * 2. doReg 호출 (파라미터 : DocExec 풀패스, Setting.ini 풀패스, 인식값 파일 풀패스, libDocRecognizer.so 풀패스	 *
                 */
                // strFilePath = docRecogLominRecogPath + "_" + UUID.randomUUID().toString().replace("-", "") + ".txt";
                strFilePath = "" + "_" + UUID.randomUUID().toString().replace("-", "") + ".txt";
                // int nRtn = writeFile(strRtn, strFilePath);
                int nRtn = 0;
                //LOGGER.info("[reqDocRead] writeFile 완료");
                String rtnDocRecog = "";
                if (nRtn == 0) {

                    if (lawDocCode.contains(DocList.get(0).get("imageDocCode"))) {
                        rtnDocRecog = readRecResult();
                    }
                    LOGGER.info("[reqDocRead] DocRecog 인식 완료");

                    if ("dev".equals("dev")) {
                        LOGGER.info("[reqDocRead] DocRecog 인식 완료 rtnDocRecog ===> " + DocList.get(0).get("filePath") + ", rtn ===> " + rtnDocRecog);
                    }

                    // rtnDocRecog 리턴값에서 value 항목만 추출
                    JSONObject jsonobject = new JSONObject(rtnDocRecog);

                    int ncount = jsonobject.getInt("TotalPages");
                    LOGGER.info("[reqDocRead] TotalPages : " + ncount);
                    int nItemCnt = 0;
                    int nStartCnt = 0;

                    String strMaxRound = "1";

                    for (int i = 0; i < ncount; i++) {

                        // 인식값 page가 없으면 continue 처리
                        if (jsonobject.isNull("page" + Integer.toString(i))) {
                            continue;
                        }

                        Object objItem = jsonobject.get("page" + Integer.toString(i));

                        if (!objItem.equals("unknown")) {

                            // IMAGE_ID, IMAGE_PAGE 계산
                            String strImageId = "";
                            Integer nNowPage = 0;
                            Integer nImagePage = 0;

                            JSONObject jsonItem = jsonobject.getJSONObject("page" + Integer.toString(i));

                            int nvaluecnt = jsonItem.getInt("text_count");

                            if (nvaluecnt > 0) {
                                JSONObject jsonValue = jsonItem.getJSONObject("value");

                                // 판결문에서 이미 인식된 항목이 있으면 뒤에값은 무시. 오인식 케이스가 있음
                                boolean bSGI0054 = false;
                                if ("SGI0054".equals(jsonItem.getString("idcode"))) {
                                    String strdocType = "";
                                    for (Map<String, Object> tmpOcrProcData : OcrProcData) {
                                        strdocType = tmpOcrProcData.get("docType").toString();
                                        if ("5560441,6060368".contains(strdocType)) {
                                            bSGI0054 = true;
                                            break;
                                        }
                                    }
                                }
                                if (bSGI0054 == true) {
                                    continue;
                                }

                                // 변제예정액표 처리여부
                                boolean bSGI0006Recog = false;

                                // 수재보험료 처리여부
                                boolean bSGI0030Recog = false;

                                // 변제예정액표는 리턴값 형식이 특별한 형식이라 항목별로 List<Map<String, Object>>에 저장해서 for문 loop가 끝나면 최종적으로  OcrProcData에 값을 추가하도록 처리함.
                                List<Map<String, Object>> tmpOcrProcData0 = new ArrayList<Map<String, Object>>();    //목록번호
                                List<Map<String, Object>> tmpOcrProcData1 = new ArrayList<Map<String, Object>>();    //변제율
                                List<Map<String, Object>> tmpOcrProcData2 = new ArrayList<Map<String, Object>>();    //시작회차
                                List<Map<String, Object>> tmpOcrProcData3 = new ArrayList<Map<String, Object>>();    //종료회차
                                List<Map<String, Object>> tmpOcrProcData4 = new ArrayList<Map<String, Object>>();    //횟수
                                List<Map<String, Object>> tmpOcrProcData5 = new ArrayList<Map<String, Object>>();    //개인회생채권액
                                List<Map<String, Object>> tmpOcrProcData6 = new ArrayList<Map<String, Object>>();    //월변제예정(유보)액
                                List<Map<String, Object>> tmpOcrProcData7 = new ArrayList<Map<String, Object>>();    //총변제예정(유보)액
                                int nCnt = 0;
                                int nNowCnt = 0;
                                int nInsurIdx = 10;

                                // 종목인덱스
                                int nextcounttIdx = -1;
                                // 종목갯수
                                int nSectionCnt = 0;

                                int nSGI0002cnt = 0;

                                // 페이지별 항목 갯수 저장용
                                LinkedHashMap<String, Integer> tmpMapItem = new LinkedHashMap<String, Integer>();

                                for (int itemcnt = 1; itemcnt <= nvaluecnt; itemcnt++) {
                                    tmpMapRecog = new HashMap<String, Object>();

                                    //// 개인회생 채권자목록, 변제예정액표는 목록 중복건때문에 별도 처리 해야됨. 서식코드는 서울보증 서식은 분류가 되어있지 않아서 법원문서는 docreg에서 주는 값으로 일단 사용
                                    String stridCode = jsonItem.getString("idcode");
                                    if ("SGI0004".equals(stridCode)) {
                                        // 개인회생채권자목록 서식코드는 "SGI0004"로 변경
                                        // OCR_COL이 5인값은 다음 항목의 갯수값이라서 저장 안함
                                        // 채권현재액(원금), 채권현재액(이자) 순서로 리턴해야됨
                                        //int nCnt = 0;
                                        if (itemcnt == 5) {
                                            nCnt = Integer.parseInt(jsonValue.getString(Integer.toString(itemcnt)));
                                        } else if (itemcnt == 6) {
                                            tmpOcrProcData0 = new ArrayList<Map<String, Object>>();
                                            tmpOcrProcData1 = new ArrayList<Map<String, Object>>();
                                            tmpOcrProcData2 = new ArrayList<Map<String, Object>>();

                                            Map<String, Object> tmpMapRecog0 = new HashMap<String, Object>();
                                            Map<String, Object> tmpMapRecog1 = new HashMap<String, Object>();
                                            Map<String, Object> tmpMapRecog2 = new HashMap<String, Object>();

                                            int nCnt1 = 0;
                                            String tmpStr = jsonValue.getString(Integer.toString(itemcnt));
                                            List<Map<String, String>> tmpList = new ObjectMapper().readValue(tmpStr, new TypeReference<List<Map<String, String>>>() {
                                            });
                                            for (Map<String, String> tmpMap1 : tmpList) {
                                                nCnt1++;

                                                // 채권번호는 key="7", 채권현재액(원금)은 key="5", 채권현재액(이자)은 key="6" 고정

                                                // 이미지 id, 좌표값 분리
                                                String tmpOrgVal = tmpMap1.get("1");
                                                // 마지막 | 제거
                                                tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                //split
                                                String[] tmpOrgList = tmpOrgVal.split("[|]");
                                                String tmpVal = "";
                                                String tmpPos = "";
                                                if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                    nErrCnt++;
                                                    tmpVal = "";
                                                    tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                    strImageId = "";
                                                    nNowPage = -1;
                                                } else {
                                                    nRegocCnt++;
                                                    // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                    tmpVal = tmpOrgList[0];
                                                    tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                    nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                    for (Map<String, String> ReqDoc : DocList) {
                                                        strImageId = ReqDoc.get("imageId");
                                                        nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                        if (nNowPage <= nImagePage) {
                                                            // 파일 dpi 정보 확인
                                                            // // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                            // // tmpPos = tmpPos + "," + imageDPI;
                                                            break;
                                                        } else {
                                                            nNowPage = nNowPage - nImagePage;
                                                        }
                                                    }
                                                }

                                                tmpMapRecog0 = new HashMap<String, Object>();
                                                tmpMapRecog0.put("docType", stridCode);                                    // 서식코드
                                                tmpMapRecog0.put("imageId", strImageId);                                // IMAGE_ID
                                                tmpMapRecog0.put("imagePage", Integer.toString(nNowPage));                // IMAGE_PAGE
                                                tmpMapRecog0.put("ocrCol", "7");                                        // OCR컬럼(인식항목이름)
                                                tmpMapRecog0.put("ocrColIdx", Integer.toString(nCnt1));                    // OCR동일컬럼배열인덱스
                                                tmpMapRecog0.put("ocrColCnt", Integer.toString(nCnt));                    // OCR동일컬럼배열개수
                                                tmpMapRecog0.put("ocrOrgVal", tmpVal);                                    // OCR결과값
                                                tmpMapRecog0.put("ocrCoord", tmpPos);                                    // OCR좌표

                                                tmpOcrProcData0.add(tmpMapRecog0);
                                                //nRegocCnt++;

                                                // 이미지 id, 좌표값 분리
                                                tmpOrgVal = tmpMap1.get("2");
                                                // 마지막 | 제거
                                                tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                //split
                                                String[] tmpOrgList1 = tmpOrgVal.split("[|]");
                                                tmpVal = "";
                                                tmpPos = "";
                                                if ("".equals(tmpOrgList1[0]) || "NoValue".equals(tmpOrgList1[0])) {
                                                    nErrCnt++;
                                                    tmpVal = "";
                                                    tmpPos = "" + "," + "" + "," + "" + "," + "";
                                                    strImageId = "";
                                                    nNowPage = -1;
                                                } else {
                                                    nRegocCnt++;
                                                    // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                    tmpVal = tmpOrgList1[0];
                                                    tmpPos = tmpOrgList1[2] + "," + tmpOrgList1[3] + "," + tmpOrgList1[4] + "," + tmpOrgList1[5];

                                                    nNowPage = Integer.parseInt(tmpOrgList1[1]) + 1;

                                                    for (Map<String, String> ReqDoc : DocList) {
                                                        strImageId = ReqDoc.get("imageId");
                                                        nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                        if (nNowPage <= nImagePage) {
                                                            // 파일 dpi 정보 확인
                                                            // // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                            // // tmpPos = tmpPos + "," + imageDPI;
                                                            break;
                                                        } else {
                                                            nNowPage = nNowPage - nImagePage;
                                                        }
                                                    }
                                                }

                                                tmpMapRecog1 = new HashMap<String, Object>();
                                                tmpMapRecog1.put("docType", stridCode);                                    // 서식코드
                                                tmpMapRecog1.put("imageId", strImageId);                                // IMAGE_ID
                                                tmpMapRecog1.put("imagePage", Integer.toString(nNowPage));                // IMAGE_PAGE
                                                tmpMapRecog1.put("ocrCol", "5");                                        // OCR컬럼(인식항목이름)
                                                tmpMapRecog1.put("ocrColIdx", Integer.toString(nCnt1));                    // OCR동일컬럼배열인덱스
                                                tmpMapRecog1.put("ocrColCnt", Integer.toString(nCnt));                    // OCR동일컬럼배열개수
                                                tmpMapRecog1.put("ocrOrgVal", tmpVal);                                    // OCR결과값
                                                tmpMapRecog1.put("ocrCoord", tmpPos);                                    // OCR좌표

                                                tmpOcrProcData1.add(tmpMapRecog1);
                                                //nRegocCnt++;

                                                // 이미지 id, 좌표값 분리
                                                tmpOrgVal = tmpMap1.get("3");
                                                // 마지막 | 제거
                                                tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                //split
                                                String[] tmpOrgList2 = tmpOrgVal.split("[|]");
                                                tmpVal = "";
                                                tmpPos = "";
                                                if ("".equals(tmpOrgList2[0]) || "NoValue".equals(tmpOrgList2[0])) {
                                                    nErrCnt++;
                                                    tmpVal = "";
                                                    tmpPos = "" + "," + "" + "," + "" + "," + "";
                                                    strImageId = "";
                                                    nNowPage = -1;
                                                } else {
                                                    nRegocCnt++;
                                                    // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                    tmpVal = tmpOrgList2[0];
                                                    tmpPos = tmpOrgList2[2] + "," + tmpOrgList2[3] + "," + tmpOrgList2[4] + "," + tmpOrgList2[5];

                                                    nNowPage = Integer.parseInt(tmpOrgList2[1]) + 1;

                                                    for (Map<String, String> ReqDoc : DocList) {
                                                        strImageId = ReqDoc.get("imageId");
                                                        nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                        if (nNowPage <= nImagePage) {
                                                            // 파일 dpi 정보 확인
                                                            // // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                            // // tmpPos = tmpPos + "," + imageDPI;
                                                            break;
                                                        } else {
                                                            nNowPage = nNowPage - nImagePage;
                                                        }
                                                    }
                                                }

                                                tmpMapRecog2 = new HashMap<String, Object>();
                                                tmpMapRecog2.put("docType", stridCode);                                    // 서식코드
                                                tmpMapRecog2.put("imageId", strImageId);                                // IMAGE_ID
                                                tmpMapRecog2.put("imagePage", Integer.toString(nNowPage));                // IMAGE_PAGE
                                                tmpMapRecog2.put("ocrCol", "6");                                        // OCR컬럼(인식항목이름)
                                                tmpMapRecog2.put("ocrColIdx", Integer.toString(nCnt1));                    // OCR동일컬럼배열인덱스
                                                tmpMapRecog2.put("ocrColCnt", Integer.toString(nCnt));                    // OCR동일컬럼배열개수
                                                tmpMapRecog2.put("ocrOrgVal", tmpVal);                                    // OCR결과값
                                                tmpMapRecog2.put("ocrCoord", tmpPos);                                    // OCR좌표

                                                tmpOcrProcData2.add(tmpMapRecog2);
                                                //nRegocCnt++;
                                            }
                                            OcrProcData.addAll(tmpOcrProcData1);
                                            OcrProcData.addAll(tmpOcrProcData2);
                                            OcrProcData.addAll(tmpOcrProcData0);
                                        } else {

                                            // 이미지 id, 좌표값 분리
                                            String tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            String[] tmpOrgList = tmpOrgVal.split("[|]");
                                            String tmpVal = "";
                                            String tmpPos = "";
                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                nErrCnt++;
                                                tmpVal = "";
                                                tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                strImageId = "";
                                                nNowPage = -1;
                                            } else {
                                                nRegocCnt++;
                                                // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                tmpVal = tmpOrgList[0];
                                                tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                for (Map<String, String> ReqDoc : DocList) {
                                                    strImageId = ReqDoc.get("imageId");
                                                    nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                    if (nNowPage <= nImagePage) {
                                                        // 파일 dpi 정보 확인
                                                        // // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                        // // tmpPos = tmpPos + "," + imageDPI;
                                                        break;
                                                    } else {
                                                        nNowPage = nNowPage - nImagePage;
                                                    }
                                                }
                                            }

                                            tmpMapRecog.put("docType", stridCode);                                        // 서식코드
                                            tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                            tmpMapRecog.put("ocrCol", Integer.toString(itemcnt));                        // OCR컬럼(인식항목이름)
                                            tmpMapRecog.put("ocrColIdx", "1");                                            // OCR동일컬럼배열인덱스
                                            tmpMapRecog.put("ocrColCnt", "1");                                            // OCR동일컬럼배열개수
                                            tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                            tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표
                                            OcrProcData.add(tmpMapRecog);

                                            //nRegocCnt++;
                                        }
                                    } else if ("SGI0006".equals(stridCode) || "SGI0007".equals(stridCode)) {

                                        // 이미지 id, 좌표값 분리
                                        String tmpOrgVal = "";

                                        String tmpVal = "";
                                        String tmpPos = "";

										/*
										// Start 항목이 all인지 체크
										//tmpOrgVal = jsonValue.getString(Integer.toString(2));
										JSONObject tmpjsonVal = jsonValue.getJSONObject(Integer.toString(2));
										String tmpStartVal = tmpjsonVal.getString("Start").substring(0, tmpjsonVal.getString("Start").indexOf("|"));
										if("ALL".equals(tmpStartVal) && tmpOcrProcData2.size() >= 1){
											continue;
										}
										*/

                                        bSGI0006Recog = true;
                                        //변제예정액표 서식코드는 "SGI0006"로 변경
                                        //tmpMapRecog.put("DOC_TYPE", "SGI0006");				// 서식코드

                                        // 변제예정액표는 리턴값 형식이 특별한 형식이라 항목별로 List<Map<String, Object>>에 저장해서 for문 loop가 끝나면 최종적으로  OcrProcData에 값을 추가하도록 처리함.
										/*
										tmpOcrProcData1 = new ArrayList<Map<String, Object>>();	//변제율
										tmpOcrProcData2 = new ArrayList<Map<String, Object>>();	//시작회차
										tmpOcrProcData3 = new ArrayList<Map<String, Object>>();	//종료회차
										tmpOcrProcData4 = new ArrayList<Map<String, Object>>();	//횟수
										tmpOcrProcData5 = new ArrayList<Map<String, Object>>();	//개인회생채권액
										tmpOcrProcData6 = new ArrayList<Map<String, Object>>();	//월변제예정(유보)액
										tmpOcrProcData7 = new ArrayList<Map<String, Object>>();	//총변제예정(유보)액
										*/

                                        Map<String, Object> tmpMapRecog0 = new HashMap<String, Object>();    //목록번호
                                        Map<String, Object> tmpMapRecog1 = new HashMap<String, Object>();    //변제율
                                        Map<String, Object> tmpMapRecog2 = new HashMap<String, Object>();    //시작회차
                                        Map<String, Object> tmpMapRecog3 = new HashMap<String, Object>();    //종료회차
                                        Map<String, Object> tmpMapRecog4 = new HashMap<String, Object>();    //횟수
                                        Map<String, Object> tmpMapRecog5 = new HashMap<String, Object>();    //개인회생채권액
                                        Map<String, Object> tmpMapRecog6 = new HashMap<String, Object>();    //월변제예정(유보)액
                                        Map<String, Object> tmpMapRecog7 = new HashMap<String, Object>();    //총변제예정(유보)액


                                        // 이미지 id, 좌표값 분리
                                        if (itemcnt == 1) {

                                            // 이미지 id, 좌표값 분리
                                            tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            String[] tmpOrgList = tmpOrgVal.split("[|]");
                                            tmpVal = "";
                                            tmpPos = "";
                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                nErrCnt++;
                                                tmpVal = "";
                                                tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                strImageId = "";
                                                nNowPage = -1;
                                            } else {
                                                nRegocCnt++;
                                                // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                tmpVal = tmpOrgList[0];
                                                tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                if (nNowPage >= 1000) {
                                                    nErrCnt++;
                                                    tmpVal = "";
                                                    tmpPos = "";
                                                    strImageId = "";
                                                    nNowPage = -1;
                                                }

                                                for (Map<String, String> ReqDoc : DocList) {
                                                    strImageId = ReqDoc.get("imageId");
                                                    nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                    if (nNowPage <= nImagePage) {
                                                        // 파일 dpi 정보 확인
                                                        // // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                        // // tmpPos = tmpPos + "," + imageDPI;
                                                        break;
                                                    } else {
                                                        nNowPage = nNowPage - nImagePage;
                                                    }
                                                }
                                            }

                                            //nCnt = Integer.parseInt(jsonValue.getString(Integer.toString(itemcnt)));
                                            tmpMapRecog1.put("docType", "SGI0006");                                            // 서식코드
                                            tmpMapRecog1.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog1.put("imagePage", Integer.toString(nNowPage));                        // IMAGE_PAGE
                                            tmpMapRecog1.put("ocrCol", "1");                                                // OCR컬럼(인식항목이름)
                                            tmpMapRecog1.put("ocrColIdx", "1");                                                // OCR동일컬럼배열인덱스
                                            tmpMapRecog1.put("ocrColCnt", "1");                                                // OCR동일컬럼배열개수
                                            tmpMapRecog1.put("ocrOrgVal", tmpVal);                                            // OCR결과값
                                            tmpMapRecog1.put("ocrCoord", tmpPos);                                            // OCR좌표
                                            tmpOcrProcData1.add(tmpMapRecog1);
                                        } else {
                                            JSONObject jsonValue1 = jsonValue.getJSONObject(Integer.toString(itemcnt));
                                            // Start 값이 "ALL", "NoValue" 가 아닌 경우만 값을 사용함
                                            //jsonValue1.getString("Start").substring(0, jsonValue1.getString("Start").indexOf("[|]"))

                                            if ("NoValue".equals(jsonValue1.getString("Start").substring(0, jsonValue1.getString("Start").indexOf("|"))) ||
                                                    ("ALL".equals(jsonValue1.getString("Start").substring(0, jsonValue1.getString("Start").indexOf("|"))) && tmpOcrProcData2.size() >= 1)) {
                                                continue;
                                            }


                                            // 변재예정액표가 한장짜리인 경우가 있음. start, end 값이 ALL인 문서 한장이기 때문에 List size() 가 1보다 작으면 값을 저장해야됨.
                                            //
                                            //if(!"ALL".equals(jsonValue1.getString("Start").substring(0, jsonValue1.getString("Start").indexOf("|")))
                                            //		&& !"NoValue".equals(jsonValue1.getString("Start").substring(0, jsonValue1.getString("Start").indexOf("|")))) {

                                            nStartCnt++;

                                            //시작회차 처리
                                            // 이미지 id, 좌표값 분리
                                            tmpOrgVal = jsonValue1.getString("Start");
                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            String[] tmpOrgList = tmpOrgVal.split("[|]");
                                            tmpVal = "";
                                            tmpPos = "";
                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                nErrCnt++;
                                                tmpVal = "";
                                                tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                strImageId = "";
                                                nNowPage = -1;
                                            } else {
                                                nRegocCnt++;
                                                // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                tmpVal = tmpOrgList[0];

                                                // ALL인 경우 값 교체
                                                if ("ALL".equals(tmpVal)) {
                                                    tmpVal = "1";
                                                }

                                                tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                if (nNowPage >= 1000) {
                                                    nErrCnt++;
                                                    tmpVal = "";
                                                    tmpPos = "";
                                                    strImageId = "";
                                                    nNowPage = -1;
                                                }

                                                for (Map<String, String> ReqDoc : DocList) {
                                                    strImageId = ReqDoc.get("imageId");
                                                    nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                    if (nNowPage <= nImagePage) {
                                                        // 파일 dpi 정보 확인
                                                        // // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                        // // tmpPos = tmpPos + "," + imageDPI;
                                                        break;
                                                    } else {
                                                        nNowPage = nNowPage - nImagePage;
                                                    }
                                                }
                                            }

                                            tmpMapRecog2.put("docType", "SGI0006");                                            // 서식코드
                                            tmpMapRecog2.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog2.put("imagePage", Integer.toString(nNowPage));                        // IMAGE_PAGE
                                            tmpMapRecog2.put("ocrCol", "2");                                                // OCR컬럼(인식항목이름)
                                            tmpMapRecog2.put("ocrColIdx", Integer.toString(nStartCnt));                        // OCR동일컬럼배열인덱스
                                            tmpMapRecog2.put("ocrColCnt", "1");                                                // OCR동일컬럼배열개수
                                            tmpMapRecog2.put("ocrOrgVal", tmpVal);                                            // OCR결과값
                                            tmpMapRecog2.put("ocrCoord", tmpPos);                                            // OCR좌표
                                            tmpOcrProcData2.add(tmpMapRecog2);


                                            //종료회차 처리
                                            // 이미지 id, 좌표값 분리
                                            tmpOrgVal = jsonValue1.getString("End");
                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            tmpOrgList = tmpOrgVal.split("[|]");
                                            tmpVal = "";
                                            tmpPos = "";
                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                nErrCnt++;
                                                tmpVal = "";
                                                tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                strImageId = "";
                                                nNowPage = -1;
                                            } else {
                                                nRegocCnt++;
                                                // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                tmpVal = tmpOrgList[0];

                                                // ALL인 경우 값 교체
                                                if ("ALL".equals(tmpVal)) {
                                                    tmpVal = strMaxRound;
                                                }

                                                tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                if (nNowPage >= 1000) {
                                                    nErrCnt++;
                                                    tmpVal = "";
                                                    tmpPos = "";
                                                    strImageId = "";
                                                    nNowPage = -1;
                                                }

                                                for (Map<String, String> ReqDoc : DocList) {
                                                    strImageId = ReqDoc.get("imageId");
                                                    nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                    if (nNowPage <= nImagePage) {
                                                        // 파일 dpi 정보 확인
                                                        // // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                        // // tmpPos = tmpPos + "," + imageDPI;
                                                        break;
                                                    } else {
                                                        nNowPage = nNowPage - nImagePage;
                                                    }
                                                }
                                            }

                                            tmpMapRecog3.put("docType", "SGI0006");                                            // 서식코드
                                            tmpMapRecog3.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog3.put("imagePage", Integer.toString(nNowPage));                        // IMAGE_PAGE
                                            tmpMapRecog3.put("ocrCol", "3");                                                // OCR컬럼(인식항목이름)
                                            tmpMapRecog3.put("ocrColIdx", Integer.toString(nStartCnt));                        // OCR동일컬럼배열인덱스
                                            tmpMapRecog3.put("ocrColCnt", "1");                                                // OCR동일컬럼배열개수
                                            tmpMapRecog3.put("ocrOrgVal", tmpVal);                                            // OCR결과값
                                            tmpMapRecog3.put("ocrCoord", tmpPos);                                            // OCR좌표
                                            tmpOcrProcData3.add(tmpMapRecog3);


                                            // 개인회생채권액, 월변제예정(유보)액, 총변제예정(유보)액 처리
                                            Integer nTotalCnt = Integer.parseInt(jsonValue1.getString("Count"));


                                            String tmpStr = jsonValue1.getString("Value");
                                            List<Map<String, String>> tmpList = new ObjectMapper().readValue(tmpStr, new TypeReference<List<Map<String, String>>>() {
                                            });
                                            for (Map<String, String> tmpMap1 : tmpList) {

                                                tmpMapRecog0 = new HashMap<String, Object>();    //목록번호
                                                tmpMapRecog5 = new HashMap<String, Object>();    //개인회생채권액
                                                tmpMapRecog6 = new HashMap<String, Object>();    //월변제예정(유보)액
                                                tmpMapRecog7 = new HashMap<String, Object>();    //총변제예정(유보)액

                                                nItemCnt++;

                                                tmpOrgVal = tmpMap1.get("1");
                                                // 마지막 | 제거
                                                tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                //split
                                                tmpOrgList = tmpOrgVal.split("[|]");
                                                tmpVal = "";
                                                tmpPos = "";
                                                if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                    nErrCnt++;
                                                    tmpVal = "";
                                                    tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                    strImageId = "";
                                                    nNowPage = -1;
                                                } else {
                                                    nRegocCnt++;
                                                    // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                    tmpVal = tmpOrgList[0];
                                                    tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                    nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                    if (nNowPage >= 1000) {
                                                        nErrCnt++;
                                                        tmpVal = "";
                                                        tmpPos = "";
                                                        strImageId = "";
                                                        nNowPage = -1;
                                                    }

                                                    for (Map<String, String> ReqDoc : DocList) {
                                                        strImageId = ReqDoc.get("imageId");
                                                        nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                        if (nNowPage <= nImagePage) {
                                                            // 파일 dpi 정보 확인
                                                            // // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                            // // tmpPos = tmpPos + "," + imageDPI;
                                                            break;
                                                        } else {
                                                            nNowPage = nNowPage - nImagePage;
                                                        }
                                                    }
                                                }

                                                tmpMapRecog0.put("docType", "SGI0006");                                            // 서식코드
                                                tmpMapRecog0.put("imageId", strImageId);                                        // IMAGE_ID
                                                tmpMapRecog0.put("imagePage", Integer.toString(nNowPage));                        // IMAGE_PAGE
                                                tmpMapRecog0.put("ocrCol", "7");                                                // OCR컬럼(인식항목이름)
                                                tmpMapRecog0.put("ocrColIdx", Integer.toString(nStartCnt));                        // OCR동일컬럼배열인덱스
                                                tmpMapRecog0.put("ocrColCnt", Integer.toString(nTotalCnt));                        // OCR동일컬럼배열개수
                                                tmpMapRecog0.put("ocrOrgVal", tmpVal);                                            // OCR결과값
                                                tmpMapRecog0.put("ocrCoord", tmpPos);                                            // OCR좌표
                                                tmpOcrProcData0.add(tmpMapRecog0);

                                                // 개인회생채권액 : 2, 월변제예정(유보)액 : 3, 총변제예정(유보)액 : 4
                                                tmpOrgVal = tmpMap1.get("2");
                                                // 마지막 | 제거
                                                tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                //split
                                                tmpOrgList = tmpOrgVal.split("[|]");
                                                tmpVal = "";
                                                tmpPos = "";
                                                if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                    nErrCnt++;
                                                    tmpVal = "";
                                                    tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                    strImageId = "";
                                                    nNowPage = -1;
                                                } else {
                                                    nRegocCnt++;
                                                    // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                    tmpVal = tmpOrgList[0];
                                                    tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                    nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                    if (nNowPage >= 1000) {
                                                        nErrCnt++;
                                                        tmpVal = "";
                                                        tmpPos = "";
                                                        strImageId = "";
                                                        nNowPage = -1;
                                                    }

                                                    for (Map<String, String> ReqDoc : DocList) {
                                                        strImageId = ReqDoc.get("imageId");
                                                        nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                        if (nNowPage <= nImagePage) {
                                                            // 파일 dpi 정보 확인
                                                            // // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                            // // tmpPos = tmpPos + "," + imageDPI;
                                                            break;
                                                        } else {
                                                            nNowPage = nNowPage - nImagePage;
                                                        }
                                                    }
                                                }

                                                tmpMapRecog5.put("docType", "SGI0006");                                            // 서식코드
                                                tmpMapRecog5.put("imageId", strImageId);                                        // IMAGE_ID
                                                tmpMapRecog5.put("imagePage", Integer.toString(nNowPage));                        // IMAGE_PAGE
                                                tmpMapRecog5.put("ocrCol", "4");                                                // OCR컬럼(인식항목이름)
                                                tmpMapRecog5.put("ocrColIdx", Integer.toString(nStartCnt));                        // OCR동일컬럼배열인덱스
                                                tmpMapRecog5.put("ocrColCnt", Integer.toString(nTotalCnt));                        // OCR동일컬럼배열개수
                                                tmpMapRecog5.put("ocrOrgVal", tmpVal);                                            // OCR결과값
                                                tmpMapRecog5.put("ocrCoord", tmpPos);                                            // OCR좌표
                                                tmpOcrProcData5.add(tmpMapRecog5);


                                                tmpOrgVal = tmpMap1.get("3");
                                                // 마지막 | 제거
                                                tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                //split
                                                tmpOrgList = tmpOrgVal.split("[|]");
                                                tmpVal = "";
                                                tmpPos = "";
                                                if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                    nErrCnt++;
                                                    tmpVal = "";
                                                    tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                    strImageId = "";
                                                    nNowPage = -1;
                                                } else {
                                                    nRegocCnt++;
                                                    // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                    tmpVal = tmpOrgList[0];
                                                    tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                    nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                    if (nNowPage >= 1000) {
                                                        nErrCnt++;
                                                        tmpVal = "";
                                                        tmpPos = "";
                                                        strImageId = "";
                                                        nNowPage = -1;
                                                    }

                                                    for (Map<String, String> ReqDoc : DocList) {
                                                        strImageId = ReqDoc.get("imageId");
                                                        nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                        if (nNowPage <= nImagePage) {
                                                            // 파일 dpi 정보 확인
                                                            // // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                            // // tmpPos = tmpPos + "," + imageDPI;
                                                            break;
                                                        } else {
                                                            nNowPage = nNowPage - nImagePage;
                                                        }
                                                    }
                                                }

                                                tmpMapRecog6.put("docType", "SGI0006");                                            // 서식코드
                                                tmpMapRecog6.put("imageId", strImageId);                                        // IMAGE_ID
                                                tmpMapRecog6.put("imagePage", Integer.toString(nNowPage));                        // IMAGE_PAGE
                                                tmpMapRecog6.put("ocrCol", "5");                                                // OCR컬럼(인식항목이름)
                                                tmpMapRecog6.put("ocrColIdx", Integer.toString(nStartCnt));                        // OCR동일컬럼배열인덱스
                                                tmpMapRecog6.put("ocrColCnt", Integer.toString(nTotalCnt));                        // OCR동일컬럼배열개수
                                                tmpMapRecog6.put("ocrOrgVal", tmpVal);                                            // OCR결과값
                                                tmpMapRecog6.put("ocrCoord", tmpPos);                                            // OCR좌표
                                                tmpOcrProcData6.add(tmpMapRecog6);


                                                tmpOrgVal = tmpMap1.get("4");
                                                // 마지막 | 제거
                                                tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                //split
                                                tmpOrgList = tmpOrgVal.split("[|]");
                                                tmpVal = "";
                                                tmpPos = "";
                                                if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                    nErrCnt++;
                                                    tmpVal = "";
                                                    tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                    strImageId = "";
                                                    nNowPage = -1;
                                                } else {
                                                    nRegocCnt++;
                                                    // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                    tmpVal = tmpOrgList[0];
                                                    tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                    nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                    for (Map<String, String> ReqDoc : DocList) {
                                                        strImageId = ReqDoc.get("imageId");
                                                        nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                        if (nNowPage <= nImagePage) {
                                                            // 파일 dpi 정보 확인
                                                            // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                            // tmpPos = tmpPos + "," + imageDPI;
                                                            break;
                                                        } else {
                                                            nNowPage = nNowPage - nImagePage;
                                                        }
                                                    }
                                                }

                                                tmpMapRecog7.put("docType", "SGI0006");                                            // 서식코드
                                                tmpMapRecog7.put("imageId", strImageId);                                        // IMAGE_ID
                                                tmpMapRecog7.put("imagePage", Integer.toString(nNowPage));                        // IMAGE_PAGE
                                                tmpMapRecog7.put("ocrCol", "6");                                                // OCR컬럼(인식항목이름)
                                                tmpMapRecog7.put("ocrColIdx", Integer.toString(nStartCnt));                        // OCR동일컬럼배열인덱스
                                                tmpMapRecog7.put("ocrColCnt", Integer.toString(nTotalCnt));                        // OCR동일컬럼배열개수
                                                tmpMapRecog7.put("ocrOrgVal", tmpVal);                                            // OCR결과값
                                                tmpMapRecog7.put("ocrCoord", tmpPos);                                            // OCR좌표
                                                tmpOcrProcData7.add(tmpMapRecog7);


                                            }

                                            //}


                                        }
                                    } else if ("SGI0031".equals(stridCode) || "SGI0032".equals(stridCode) || "SGI0030".equals(stridCode)) {
                                        // 수재보험료_AON (SGI0031), 수재보험료_WillisHK (SGI0032) 처리
                                        /*
                                         * docrecog 리턴값 순서
                                         * 1: 거래사명
                                         * 2: Invoice연번
                                         * 3: 특약명
                                         * 4: 특약년도
                                         * 5: 인수연도
                                         * 6: 종목
                                         * 7: 수재계약번호
                                         * 8: 실적발생년도
                                         * 9: 재보험 계수 발생기준코드
                                         * 10: Currency
                                         * 11: 상세계정명 (동일컬럼배열개수 반복)
                                         * 12: 상세계정금액 (동일컬럼배열개수 반복)
                                         * 13: 차대변여부 (동일컬럼배열개수 반복)
                                         */

                                        // 수재보험료_WillisS (SGI0030) 추가
                                        /*
                                         * docrecog 리턴값 순서
                                         * 1: 거래사명
                                         * 2: Invoice연번
                                         * 3: 특약명
                                         * 4: 특약년도
                                         * 5: 인수연도
                                         * 6: 종목
                                         * 7: 수재계약번호 <---- 무조건 없음
                                         * 8: 실적발생년도
                                         * 9: 재보험 계수 발생기준코드
                                         * 10: Currency
                                         * 11: 상세계정명 (동일컬럼배열개수 반복)
                                         * 12: 상세계정금액 (동일컬럼배열개수 반복)
                                         */
                                        bSGI0030Recog = true;

                                        // 이미지 id, 좌표값 분리
                                        String tmpOrgVal = "";

                                        String tmpVal = "";
                                        String tmpPos = "";

                                        if (itemcnt == 10) {
                                            //nCnt = Integer.parseInt(jsonValue.getString(Integer.toString(itemcnt)));
                                            // 이미지 id, 좌표값 분리
                                            tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            String[] tmpOrgList = tmpOrgVal.split("[|]");

                                            // 금액항목이 없는 경우 예외처리
                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                itemcnt = itemcnt + 3;
                                            } else {
                                                nCnt = Integer.parseInt(tmpOrgList[0]);

                                                int nvalCnt = 0;

                                                // 차대변여부, 상세계정명, 금액 처리
                                                for (int j = 1; j <= nCnt; j++) {

                                                    itemcnt++;

                                                    // 금액이 0인경우 항목 저장 안함.
                                                    // 이미지 id, 좌표값 분리
                                                    tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt + nCnt));
                                                    // 마지막 | 제거
                                                    tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                    //split
                                                    tmpOrgList = tmpOrgVal.split("[|]");
                                                    if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0]) || "0.00".equals(tmpOrgList[0])) {
                                                        // for문 loop가 끝나면 itemcnt를 반복부 다음 값으로 변경
                                                        if (j == nCnt) {
                                                            itemcnt = itemcnt + nCnt + nCnt;
                                                        }
                                                        continue;
                                                    }

                                                    nvalCnt++;

                                                    //상세걔정명
                                                    tmpMapRecog = new HashMap<String, Object>();

                                                    tmpMapRecog.put("docType", stridCode);

                                                    // 이미지 id, 좌표값 분리
                                                    tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                                    // 마지막 | 제거
                                                    tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                    //split
                                                    tmpOrgList = tmpOrgVal.split("[|]");
                                                    tmpVal = "";
                                                    tmpPos = "";
                                                    if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                        nErrCnt++;
                                                        tmpVal = "";
                                                        tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                        strImageId = "";
                                                        nNowPage = -1;
                                                    } else {
                                                        nRegocCnt++;
                                                        // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                        tmpVal = tmpOrgList[0];
                                                        tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                        nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                        for (Map<String, String> ReqDoc : DocList) {
                                                            strImageId = ReqDoc.get("imageId");
                                                            nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                            if (nNowPage <= nImagePage) {
                                                                // 파일 dpi 정보 확인
                                                                // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                                // tmpPos = tmpPos + "," + imageDPI;
                                                                break;
                                                            } else {
                                                                nNowPage = nNowPage - nImagePage;
                                                            }
                                                        }
                                                    }
                                                    tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                                    tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                                    tmpMapRecog.put("ocrCol", Integer.toString(11));                        // OCR컬럼(인식항목이름)
                                                    tmpMapRecog.put("ocrColIdx", Integer.toString(nvalCnt));                    // OCR동일컬럼배열인덱스
                                                    tmpMapRecog.put("ocrColCnt", Integer.toString(nCnt));                        // OCR동일컬럼배열개수
                                                    tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                                    tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표
                                                    tmpOcrProcData1.add(tmpMapRecog);


                                                    //금액
                                                    tmpMapRecog = new HashMap<String, Object>();

                                                    tmpMapRecog.put("docType", stridCode);

                                                    // 이미지 id, 좌표값 분리
                                                    tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt + nCnt));
                                                    // 마지막 | 제거
                                                    tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                    //split
                                                    tmpOrgList = tmpOrgVal.split("[|]");
                                                    tmpVal = "";
                                                    tmpPos = "";
                                                    if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                        nErrCnt++;
                                                        tmpVal = "";
                                                        tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                        strImageId = "";
                                                        nNowPage = -1;
                                                    } else {
                                                        nRegocCnt++;
                                                        // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                        tmpVal = tmpOrgList[0];
                                                        tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                        nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                        for (Map<String, String> ReqDoc : DocList) {
                                                            strImageId = ReqDoc.get("imageId");
                                                            nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                            if (nNowPage <= nImagePage) {
                                                                // 파일 dpi 정보 확인
                                                                // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                                // tmpPos = tmpPos + "," + imageDPI;
                                                                break;
                                                            } else {
                                                                nNowPage = nNowPage - nImagePage;
                                                            }
                                                        }
                                                    }
                                                    tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                                    tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                                    tmpMapRecog.put("ocrCol", Integer.toString(12));                        // OCR컬럼(인식항목이름)
                                                    tmpMapRecog.put("ocrColIdx", Integer.toString(nvalCnt));                    // OCR동일컬럼배열인덱스
                                                    tmpMapRecog.put("ocrColCnt", Integer.toString(nCnt));                        // OCR동일컬럼배열개수
                                                    tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                                    tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표
                                                    tmpOcrProcData1.add(tmpMapRecog);

                                                    // willis 싱가폴은 차대변여부 없음
                                                    if (!"SGI0030".equals(stridCode)) {
                                                        //차대변여부
                                                        tmpMapRecog = new HashMap<String, Object>();

                                                        tmpMapRecog.put("docType", stridCode);

                                                        // 이미지 id, 좌표값 분리
                                                        tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt + nCnt + nCnt));
                                                        // 마지막 | 제거
                                                        tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);

                                                        //split
                                                        tmpOrgList = tmpOrgVal.split("[|]");
                                                        tmpVal = "";
                                                        tmpPos = "";
                                                        // 차대변여부는 NotAdvised 값까지 무시처리
                                                        if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0]) || "NotAdvised".equals(tmpOrgList[0])) {
                                                            nErrCnt++;
                                                            tmpVal = "";
                                                            tmpPos = "" + "," + "" + "," + "" + "," + "";
                                                            strImageId = "";
                                                            nNowPage = -1;
                                                        } else {
                                                            nRegocCnt++;
                                                            // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                            tmpVal = tmpOrgList[0];
                                                            tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                            nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                            for (Map<String, String> ReqDoc : DocList) {
                                                                strImageId = ReqDoc.get("imageId");
                                                                nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                                if (nNowPage <= nImagePage) {
                                                                    // 파일 dpi 정보 확인
                                                                    // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                                    // tmpPos = tmpPos + "," + imageDPI;
                                                                    break;
                                                                } else {
                                                                    nNowPage = nNowPage - nImagePage;
                                                                }
                                                            }
                                                        }
                                                        tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                                        tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                                        tmpMapRecog.put("ocrCol", Integer.toString(13));                        // OCR컬럼(인식항목이름)
                                                        tmpMapRecog.put("ocrColIdx", Integer.toString(nvalCnt));                    // OCR동일컬럼배열인덱스
                                                        tmpMapRecog.put("ocrColCnt", Integer.toString(nCnt));                        // OCR동일컬럼배열개수
                                                        tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                                        tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표
                                                        tmpOcrProcData1.add(tmpMapRecog);
                                                    }

                                                    // for문 loop가 끝나면 itemcnt를 반복부 다음 값으로 변경
                                                    if (j == nCnt) {
                                                        itemcnt = itemcnt + nCnt + nCnt;
                                                    }
                                                }

                                                // 기존항목 ocrColCnt 값 수정
                                                for (Map<String, Object> tmpmap : tmpOcrProcData1) {
                                                    tmpmap.put("ocrColCnt", Integer.toString(nvalCnt));
                                                }

                                            }

                                            // 수재계약번호처리
                                            // willis 싱가폴은 수재계약번호처리 없음 aon, willisHK와 항목 갯수 맞추기위해 빈값으로 집어넣음
                                            // willis 싱가폴 수재계약번호 추가됨. 차대변여부가 없어서 채다변여부 갯수만큼의 nCnt를 빼야됨. itemcnt- nCnt 처리


                                            if ("SGI0030".equals(stridCode)) {
                                                itemcnt = itemcnt - nCnt;
                                            }


                                            tmpMapRecog = new HashMap<String, Object>();

                                            tmpMapRecog.put("docType", stridCode);

                                            itemcnt++;

                                            // 이미지 id, 좌표값 분리
                                            tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);


                                            //split
                                            tmpOrgList = tmpOrgVal.split("[|]");
                                            tmpVal = "";
                                            tmpPos = "";
                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                nErrCnt++;
                                                tmpVal = "";
                                                tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                strImageId = "";
                                                nNowPage = -1;
                                            } else {
                                                nRegocCnt++;
                                                // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                tmpVal = tmpOrgList[0];
                                                tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                for (Map<String, String> ReqDoc : DocList) {
                                                    strImageId = ReqDoc.get("imageId");
                                                    nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                    if (nNowPage <= nImagePage) {
                                                        // 파일 dpi 정보 확인
                                                        // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                        // tmpPos = tmpPos + "," + imageDPI;
                                                        break;
                                                    } else {
                                                        nNowPage = nNowPage - nImagePage;
                                                    }
                                                }
                                            }

                                            tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                            tmpMapRecog.put("ocrCol", Integer.toString(7));                        // OCR컬럼(인식항목이름)
                                            tmpMapRecog.put("ocrColIdx", Integer.toString(1));                    // OCR동일컬럼배열인덱스
                                            tmpMapRecog.put("ocrColCnt", Integer.toString(1));                        // OCR동일컬럼배열개수
                                            tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                            tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표
                                            //tmpOcrProcData1.add(tmpMapRecog);
                                            OcrProcData.add(tmpMapRecog);

                                            OcrProcData.addAll(tmpOcrProcData1);
                                        } else {

                                            tmpMapRecog.put("docType", stridCode);            // 서식코드

                                            // 이미지 id, 좌표값 분리
                                            tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            String[] tmpOrgList = tmpOrgVal.split("[|]");
                                            tmpVal = "";
                                            tmpPos = "";
                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                nErrCnt++;
                                                tmpVal = "";
                                                tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                strImageId = "";
                                                nNowPage = -1;
                                            } else {
                                                nRegocCnt++;
                                                // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right


                                                tmpVal = tmpOrgList[0];
                                                tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                for (Map<String, String> ReqDoc : DocList) {
                                                    strImageId = ReqDoc.get("imageId");
                                                    nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                    if (nNowPage <= nImagePage) {
                                                        // 파일 dpi 정보 확인
                                                        // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                        // tmpPos = tmpPos + "," + imageDPI;
                                                        break;
                                                    } else {
                                                        nNowPage = nNowPage - nImagePage;
                                                    }
                                                }
                                            }

                                            //재보험 계수 발생기준코드 예외처리 (1-Oct) 형식으로 들어오는 케이스. 1-Jan, 4-Apr, 7-Jul, 10-Oct 월 케이스가 있을것으로 예상
                                            if (itemcnt == 8) {
                                                if (tmpVal.contains("Jan")) {
                                                    tmpVal = "1Q";
                                                } else if (tmpVal.contains("Apr")) {
                                                    tmpVal = "2Q";
                                                } else if (tmpVal.contains("Jul")) {
                                                    tmpVal = "3Q";
                                                } else if (tmpVal.contains("Oct")) {
                                                    tmpVal = "4Q";
                                                }
                                            }

                                            nInsurIdx = 0;
                                            if (itemcnt == 7 || itemcnt == 8 || itemcnt == 9) {
                                                nInsurIdx = itemcnt + 1;
                                            } else {
                                                nInsurIdx = itemcnt;
                                            }
                                            tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                            tmpMapRecog.put("ocrCol", Integer.toString(nInsurIdx));                        // OCR컬럼(인식항목이름)
                                            tmpMapRecog.put("ocrColIdx", "1");                                            // OCR동일컬럼배열인덱스
                                            tmpMapRecog.put("ocrColCnt", "1");                                            // OCR동일컬럼배열개수
                                            tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                            tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표

                                            //LocalDateTime regDate = LocalDateTime.now();
                                            //tmpMapRecog.put("REG_DTTM", regDate.format(formatter));							// 등록시간
                                            OcrProcData.add(tmpMapRecog);

                                            //nRegocCnt++;
                                        }
                                    } else if ("SGI0033".equals(stridCode)) {

                                        // 수재보험료_GuySunshine -> 차대변여부 없음
                                        /*
                                         * docrecog 리턴값 순서
                                         * 1: 거래사명
                                         * 2: Invoice연번
                                         * 3: 특약명
                                         * 4: 특약년도
                                         * 5: 인수연도
                                         * 6: 종목
                                         * 7: 수재계약번호
                                         * 8: 실적발생년도
                                         * 9: 재보험 계수 발생기준코드
                                         * 10: Currency
                                         * 11: 상세계정명 (동일컬럼배열개수 반복)
                                         * 12: 상세계정금액 (동일컬럼배열개수 반복)
                                         */

                                        bSGI0030Recog = true;

                                        // 이미지 id, 좌표값 분리
                                        String tmpOrgVal = "";

                                        String tmpVal = "";
                                        String tmpPos = "";

                                        // 상세계정갯수
                                        int nDescCnt = 0;
                                        int nDescIdx = 0;

                                        if (itemcnt == 6) {
                                            //nCnt = Integer.parseInt(jsonValue.getString(Integer.toString(itemcnt)));
                                            // 이미지 id, 좌표값 분리
                                            tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            String[] tmpOrgList = tmpOrgVal.split("[|]");

                                            // 항목 갯수 없으면 break;
                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                break;
                                            }

                                            nCnt = Integer.parseInt(tmpOrgList[0]);
                                            nSectionCnt = nCnt;
                                            // 항목 갯수 idx자리 계산
                                            nextcounttIdx = itemcnt + (nCnt * 2) + 3 + 1;

                                            String tmpItemPage = "";
                                            int nPageCnt = 0;

                                            // 종목명, 계약번호 처리
                                            for (int j = 1; j <= nCnt; j++) {

                                                itemcnt++;

                                                //종목명
                                                tmpMapRecog = new HashMap<String, Object>();

                                                tmpMapRecog.put("docType", stridCode);

                                                // 이미지 id, 좌표값 분리
                                                tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                                // 마지막 | 제거
                                                tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                //split
                                                tmpOrgList = tmpOrgVal.split("[|]");
                                                tmpVal = "";
                                                tmpPos = "";
                                                if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                    nErrCnt++;
                                                    tmpVal = "";
                                                    tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                    strImageId = "";
                                                    nNowPage = -1;
                                                } else {
                                                    nRegocCnt++;
                                                    // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                    tmpVal = tmpOrgList[0];
                                                    tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                    nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                    for (Map<String, String> ReqDoc : DocList) {
                                                        strImageId = ReqDoc.get("imageId");
                                                        nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                        if (nNowPage <= nImagePage) {
                                                            // 파일 dpi 정보 확인
                                                            // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                            // tmpPos = tmpPos + "," + imageDPI;
                                                            break;
                                                        } else {
                                                            nNowPage = nNowPage - nImagePage;
                                                        }
                                                    }
                                                }

                                                // Levies@ -> Levies, 로 변경
                                                // @ -> , 으로 수정
                                                //tmpVal=tmpVal.replace("Levies@", "Levies,");
                                                tmpVal = tmpVal.replace("@", ",");

                                                tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                                tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                                tmpMapRecog.put("ocrCol", Integer.toString(6));                        // OCR컬럼(인식항목이름)
                                                tmpMapRecog.put("ocrColIdx", Integer.toString(j));                    // OCR동일컬럼배열인덱스
                                                tmpMapRecog.put("ocrColCnt", Integer.toString(nCnt));                        // OCR동일컬럼배열개수
                                                tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                                tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표
                                                tmpOcrProcData1.add(tmpMapRecog);

                                                if (j == 1) {
                                                    tmpItemPage = Integer.toString(nNowPage);
                                                }
                                                if (tmpItemPage.equals(Integer.toString(nNowPage))) {
                                                    nDescIdx++;
                                                } else {
                                                    tmpMapItem.put(tmpItemPage, nDescIdx);
                                                    tmpItemPage = Integer.toString(nNowPage);
                                                    nDescIdx = 1;
                                                }

                                                //계약번호
                                                tmpMapRecog = new HashMap<String, Object>();

                                                tmpMapRecog.put("docType", stridCode);

                                                // 이미지 id, 좌표값 분리
                                                tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt + nCnt));
                                                // 마지막 | 제거
                                                tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                //split
                                                tmpOrgList = tmpOrgVal.split("[|]");
                                                tmpVal = "";
                                                tmpPos = "";
                                                if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                    nErrCnt++;
                                                    tmpVal = "";
                                                    tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                    strImageId = "";
                                                    nNowPage = -1;
                                                } else {
                                                    nRegocCnt++;
                                                    // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                    tmpVal = tmpOrgList[0];
                                                    tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                    nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                    for (Map<String, String> ReqDoc : DocList) {
                                                        strImageId = ReqDoc.get("imageId");
                                                        nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                        if (nNowPage <= nImagePage) {
                                                            // 파일 dpi 정보 확인
                                                            // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                            // tmpPos = tmpPos + "," + imageDPI;
                                                            break;
                                                        } else {
                                                            nNowPage = nNowPage - nImagePage;
                                                        }
                                                    }
                                                }
                                                tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                                tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                                tmpMapRecog.put("ocrCol", Integer.toString(7));                        // OCR컬럼(인식항목이름)
                                                tmpMapRecog.put("ocrColIdx", Integer.toString(j));                    // OCR동일컬럼배열인덱스
                                                tmpMapRecog.put("ocrColCnt", Integer.toString(nCnt));                        // OCR동일컬럼배열개수
                                                tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                                tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표
                                                tmpOcrProcData1.add(tmpMapRecog);


                                                // for문 loop가 끝나면 itemcnt를 반복부 다음 값으로 변경
                                                if (j == nCnt) {
                                                    itemcnt = itemcnt + nCnt;
                                                    tmpMapItem.put(tmpItemPage, nDescIdx);
                                                }
                                            }

                                            OcrProcData.addAll(tmpOcrProcData1);
                                            tmpOcrProcData1 = new ArrayList<Map<String, Object>>();

                                            // 실적발생년도 처리
                                            itemcnt++;

                                            tmpMapRecog = new HashMap<String, Object>();

                                            tmpMapRecog.put("docType", stridCode);

                                            // 이미지 id, 좌표값 분리
                                            tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            tmpOrgList = tmpOrgVal.split("[|]");
                                            tmpVal = "";
                                            tmpPos = "";
                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                nErrCnt++;
                                                tmpVal = "";
                                                tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                strImageId = "";
                                                nNowPage = -1;
                                            } else {
                                                nRegocCnt++;
                                                // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                tmpVal = tmpOrgList[0];
                                                tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                for (Map<String, String> ReqDoc : DocList) {
                                                    strImageId = ReqDoc.get("imageId");
                                                    nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                    if (nNowPage <= nImagePage) {
                                                        // 파일 dpi 정보 확인
                                                        // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                        // tmpPos = tmpPos + "," + imageDPI;
                                                        break;
                                                    } else {
                                                        nNowPage = nNowPage - nImagePage;
                                                    }
                                                }
                                            }
                                            tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                            tmpMapRecog.put("ocrCol", Integer.toString(8));                        // OCR컬럼(인식항목이름)
                                            tmpMapRecog.put("ocrColIdx", Integer.toString(1));                    // OCR동일컬럼배열인덱스
                                            tmpMapRecog.put("ocrColCnt", Integer.toString(1));                        // OCR동일컬럼배열개수
                                            tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                            tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표
                                            //tmpOcrProcData1.add(tmpMapRecog);
                                            OcrProcData.add(tmpMapRecog);

                                            // 재보험 계수 발생기준코드 처리
                                            itemcnt++;

                                            tmpMapRecog = new HashMap<String, Object>();

                                            tmpMapRecog.put("docType", stridCode);

                                            // 이미지 id, 좌표값 분리
                                            tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            tmpOrgList = tmpOrgVal.split("[|]");
                                            tmpVal = "";
                                            tmpPos = "";
                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                nErrCnt++;
                                                tmpVal = "";
                                                tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                strImageId = "";
                                                nNowPage = -1;
                                            } else {
                                                nRegocCnt++;
                                                // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                tmpVal = tmpOrgList[0];
                                                tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                for (Map<String, String> ReqDoc : DocList) {
                                                    strImageId = ReqDoc.get("imageId");
                                                    nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                    if (nNowPage <= nImagePage) {
                                                        // 파일 dpi 정보 확인
                                                        // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                        // tmpPos = tmpPos + "," + imageDPI;
                                                        break;
                                                    } else {
                                                        nNowPage = nNowPage - nImagePage;
                                                    }
                                                }
                                            }
                                            tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                            tmpMapRecog.put("ocrCol", Integer.toString(9));                        // OCR컬럼(인식항목이름)
                                            tmpMapRecog.put("ocrColIdx", Integer.toString(1));                    // OCR동일컬럼배열인덱스
                                            tmpMapRecog.put("ocrColCnt", Integer.toString(1));                        // OCR동일컬럼배열개수
                                            tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                            tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표
                                            //tmpOcrProcData1.add(tmpMapRecog);
                                            OcrProcData.add(tmpMapRecog);

                                            // Currency 처리
                                            itemcnt++;

                                            tmpMapRecog = new HashMap<String, Object>();

                                            tmpMapRecog.put("docType", stridCode);

                                            // 이미지 id, 좌표값 분리
                                            tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            tmpOrgList = tmpOrgVal.split("[|]");
                                            tmpVal = "";
                                            tmpPos = "";
                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                nErrCnt++;
                                                tmpVal = "";
                                                tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                strImageId = "";
                                                nNowPage = -1;
                                            } else {
                                                nRegocCnt++;
                                                // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                tmpVal = tmpOrgList[0];
                                                tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                for (Map<String, String> ReqDoc : DocList) {
                                                    strImageId = ReqDoc.get("imageId");
                                                    nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                    if (nNowPage <= nImagePage) {
                                                        // 파일 dpi 정보 확인
                                                        // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                        // tmpPos = tmpPos + "," + imageDPI;
                                                        break;
                                                    } else {
                                                        nNowPage = nNowPage - nImagePage;
                                                    }
                                                }
                                            }
                                            tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                            tmpMapRecog.put("ocrCol", Integer.toString(10));                        // OCR컬럼(인식항목이름)
                                            tmpMapRecog.put("ocrColIdx", Integer.toString(1));                    // OCR동일컬럼배열인덱스
                                            tmpMapRecog.put("ocrColCnt", Integer.toString(1));                        // OCR동일컬럼배열개수
                                            tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                            tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표
                                            //tmpOcrProcData1.add(tmpMapRecog);
                                            OcrProcData.add(tmpMapRecog);


                                            //itemcnt++;

                                        } else if (itemcnt == nextcounttIdx) {
                                            //nCnt = Integer.parseInt(jsonValue.getString(Integer.toString(itemcnt)));
                                            // 이미지 id, 좌표값 분리
                                            tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            String[] tmpOrgList = tmpOrgVal.split("[|]");
                                            nCnt = Integer.parseInt(tmpOrgList[0]);

                                            String strBeforeDescName = "";


                                            // 상세계정명, 금액 처리 (GuySunshine은 차대변여부 없음)
                                            // 상세계정명을 먼저 처리 해야됨. 중복되는 항목을 제거 후 갯수를 집계, 종목명갯수 * 상세계정명갯수로 금액 처리
                                            for (int j = 1; j <= nCnt; j++) {

                                                itemcnt++;

                                                //상세걔정명
                                                tmpMapRecog = new HashMap<String, Object>();

                                                tmpMapRecog.put("docType", stridCode);

                                                // 이미지 id, 좌표값 분리
                                                tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                                // 마지막 | 제거
                                                tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                //split
                                                tmpOrgList = tmpOrgVal.split("[|]");
                                                tmpVal = "";
                                                tmpPos = "";
                                                if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                    nErrCnt++;
                                                    tmpVal = "";
                                                    tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                    strImageId = "";
                                                    nNowPage = -1;
                                                } else {
                                                    nRegocCnt++;
                                                    // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                    tmpVal = tmpOrgList[0];
                                                    tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                    nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                    for (Map<String, String> ReqDoc : DocList) {
                                                        strImageId = ReqDoc.get("imageId");
                                                        nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                        if (nNowPage <= nImagePage) {
                                                            // 파일 dpi 정보 확인
                                                            // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                            // tmpPos = tmpPos + "," + imageDPI;
                                                            break;
                                                        } else {
                                                            nNowPage = nNowPage - nImagePage;
                                                        }
                                                    }
                                                }
                                                // Levies@ -> Levies, 로 변경
                                                // @ -> , 으로 수정
                                                //tmpVal=tmpVal.replace("Levies@", "Levies,");
                                                tmpVal = tmpVal.replace("@", ",");

                                                // 상세계정명이 이전값과 같으면 continue처리
                                                if (strBeforeDescName.contains(tmpVal)) {
                                                    continue;
                                                }

                                                strBeforeDescName = strBeforeDescName + tmpVal;

                                                nDescIdx++;
                                                nDescCnt++;

                                                tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                                tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                                tmpMapRecog.put("ocrCol", Integer.toString(11));                        // OCR컬럼(인식항목이름)
                                                tmpMapRecog.put("ocrColIdx", Integer.toString(nDescIdx));                    // OCR동일컬럼배열인덱스
                                                tmpMapRecog.put("ocrColCnt", Integer.toString(nDescIdx));                        // OCR동일컬럼배열개수
                                                tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                                tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표
                                                tmpOcrProcData1.add(tmpMapRecog);

                                            }

                                            // 동일컬럼배열개수 갱신 후 항목명 저장
                                            for (Map<String, Object> tmpmap : tmpOcrProcData1) {
                                                tmpmap.put("ocrColCnt", Integer.toString(nDescCnt));
                                            }
                                            OcrProcData.addAll(tmpOcrProcData1);
                                            tmpOcrProcData1 = new ArrayList<Map<String, Object>>();


                                            // 금액처리
                                            nDescIdx = 0;
                                            int nocrCol = 12;
                                            int nValItemCnt = 0;
                                            for (int j = 0; j < nDescCnt; j++) {

                                                int nValueSum = 0;
                                                int nValCnt = 0;
                                                for (String strKey : tmpMapItem.keySet()) {

                                                    int nValue = tmpMapItem.get(strKey);
                                                    for (int k = 0; k < nValue; k++) {
                                                        nValItemCnt++;
                                                        nValCnt++;
                                                        //int nTmpItemCnt = itemcnt+(j*(nValue))+2*nValueSum+nValCnt;
                                                        int nTmpItemCnt = itemcnt + (j * (nValue)) + (nValueSum * (nDescCnt)) + k + 1;
                                                        //금액
                                                        tmpMapRecog = new HashMap<String, Object>();

                                                        tmpMapRecog.put("docType", stridCode);

                                                        // 이미지 id, 좌표값 분리
                                                        tmpOrgVal = jsonValue.getString(Integer.toString(nTmpItemCnt));
                                                        // 마지막 | 제거
                                                        tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                        //split
                                                        tmpOrgList = tmpOrgVal.split("[|]");
                                                        tmpVal = "";
                                                        tmpPos = "";
                                                        if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                            nErrCnt++;
                                                            tmpVal = "";
                                                            tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                            strImageId = "";
                                                            nNowPage = -1;
                                                        } else {
                                                            nRegocCnt++;
                                                            // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                            tmpVal = tmpOrgList[0];
                                                            tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                            nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                            for (Map<String, String> ReqDoc : DocList) {
                                                                strImageId = ReqDoc.get("imageId");
                                                                nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                                if (nNowPage <= nImagePage) {
                                                                    // 파일 dpi 정보 확인
                                                                    // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                                    // tmpPos = tmpPos + "," + imageDPI;
                                                                    break;
                                                                } else {
                                                                    nNowPage = nNowPage - nImagePage;
                                                                }
                                                            }

                                                        }

                                                        tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                                        tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                                        tmpMapRecog.put("ocrCol", Integer.toString(nocrCol));                        // OCR컬럼(인식항목이름)
                                                        tmpMapRecog.put("ocrColIdx", Integer.toString(nValCnt));                    // OCR동일컬럼배열인덱스
                                                        tmpMapRecog.put("ocrColCnt", Integer.toString(nSectionCnt));                // OCR동일컬럼배열개수
                                                        tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                                        tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표
                                                        tmpOcrProcData1.add(tmpMapRecog);


                                                    }
                                                    nValueSum = nValueSum + nValue;
                                                }

                                                nocrCol++;
                                            }

                                            // 금액 계산 끝나면 itemcnt값 반영
                                            itemcnt = itemcnt + nValItemCnt;

                                            List<Map<String, Object>> testTemp = new ArrayList<Map<String, Object>>();
                                            //List<Map<String, Object>> testTemp2 = new ArrayList<Map<String, Object>>();

                                            int countPerItem = nSectionCnt; // 항목당 개수
                                            int itemCount = nDescCnt; // 항목 수
                                            int countPerPage = 4; // 한페이지 당 개수
                                            int outterIterCount = (countPerItem + countPerPage - 1) / countPerPage; // (10 + (4 - 1)) / 4

                                            for (int ii = 0; ii < countPerItem; ii++) {
                                                int RCount = ii;
                                                for (int j = 0; j < itemCount; j++) {
                                                    if (j != 0) RCount = RCount + nSectionCnt;
                                                    testTemp.add(tmpOcrProcData1.get(RCount));
                                                }
                                            }

                                            OcrProcData.addAll(testTemp);
                                            testTemp.clear();
                                            tmpOcrProcData1 = new ArrayList<Map<String, Object>>();
                                        } else {

                                            tmpMapRecog.put("docType", stridCode);            // 서식코드

                                            // 이미지 id, 좌표값 분리
                                            tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            String[] tmpOrgList = tmpOrgVal.split("[|]");
                                            tmpVal = "";
                                            tmpPos = "";
                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                nErrCnt++;
                                                tmpVal = "";
                                                tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                strImageId = "";
                                                nNowPage = -1;
                                            } else {
                                                nRegocCnt++;
                                                // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right


                                                tmpVal = tmpOrgList[0];
                                                tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                for (Map<String, String> ReqDoc : DocList) {
                                                    strImageId = ReqDoc.get("imageId");
                                                    nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                    if (nNowPage <= nImagePage) {
                                                        // 파일 dpi 정보 확인
                                                        // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                        // tmpPos = tmpPos + "," + imageDPI;
                                                        break;
                                                    } else {
                                                        nNowPage = nNowPage - nImagePage;
                                                    }
                                                }
                                            }

                                            nInsurIdx = 0;
                                            if (itemcnt == 7 || itemcnt == 8 || itemcnt == 9) {
                                                nInsurIdx = itemcnt + 1;
                                            } else {
                                                nInsurIdx = itemcnt;
                                            }
                                            tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                            tmpMapRecog.put("ocrCol", Integer.toString(nInsurIdx));                        // OCR컬럼(인식항목이름)
                                            tmpMapRecog.put("ocrColIdx", "1");                                            // OCR동일컬럼배열인덱스
                                            tmpMapRecog.put("ocrColCnt", "1");                                            // OCR동일컬럼배열개수
                                            tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                            tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표

                                            //LocalDateTime regDate = LocalDateTime.now();
                                            //tmpMapRecog.put("REG_DTTM", regDate.format(formatter));							// 등록시간
                                            OcrProcData.add(tmpMapRecog);

                                            //nRegocCnt++;
                                        }
                                    } else if ("SGI0034".equals(stridCode)) {
                                        // 수재보험료_Hyundai
                                        /*
                                         * docrecog 리턴값 순서
                                         * 1: 거래사명
                                         * 2: Invoice연번
                                         * 3: 특약명
                                         * 4: 특약년도
                                         * 5: 인수연도
                                         * 6: 종목
                                         * 7: 수재계약번호
                                         * 8: Amount(필요없음)
                                         * 9: Amount
                                         * 10: Currency
                                         * 11: 상세계정명 (동일컬럼배열개수 반복)
                                         * 12: 상세계정금액 (동일컬럼배열개수 반복)
                                         * 13: 차대변여부 (동일컬럼배열개수 반복)
                                         */

                                        bSGI0030Recog = true;

                                        // 이미지 id, 좌표값 분리
                                        String tmpOrgVal = "";

                                        String tmpVal = "";
                                        String tmpPos = "";

                                        if (itemcnt == 10) {
                                            //nCnt = Integer.parseInt(jsonValue.getString(Integer.toString(itemcnt)));
                                            // 이미지 id, 좌표값 분리
                                            tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            String[] tmpOrgList = tmpOrgVal.split("[|]");

                                            int nvalCnt = 0;

                                            // 금액항목이 없는 경우 예외처리
                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                itemcnt = itemcnt + 3;
                                            } else {
                                                nCnt = Integer.parseInt(tmpOrgList[0]);

                                                // 차대변여부, 상세계정명, 금액 처리
                                                for (int j = 1; j <= nCnt; j++) {

                                                    itemcnt++;

                                                    // 금액이 0인경우 항목 저장 안함.
                                                    // 이미지 id, 좌표값 분리
                                                    tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt + nCnt));
                                                    // 마지막 | 제거
                                                    tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                    //split
                                                    tmpOrgList = tmpOrgVal.split("[|]");
                                                    if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0]) || "0.00".equals(tmpOrgList[0])) {
                                                        // for문 loop가 끝나면 itemcnt를 반복부 다음 값으로 변경
                                                        if (j == nCnt) {
                                                            itemcnt = itemcnt + nCnt + nCnt;
                                                        }
                                                        continue;
                                                    }

                                                    nvalCnt++;

                                                    //상세걔정명
                                                    tmpMapRecog = new HashMap<String, Object>();

                                                    tmpMapRecog.put("docType", stridCode);

                                                    // 이미지 id, 좌표값 분리
                                                    tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                                    // 마지막 | 제거
                                                    tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                    //split
                                                    tmpOrgList = tmpOrgVal.split("[|]");
                                                    tmpVal = "";
                                                    tmpPos = "";
                                                    if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                        nErrCnt++;
                                                        tmpVal = "";
                                                        tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                        strImageId = "";
                                                        nNowPage = -1;
                                                    } else {
                                                        nRegocCnt++;
                                                        // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                        tmpVal = tmpOrgList[0];
                                                        tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                        nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                        for (Map<String, String> ReqDoc : DocList) {
                                                            strImageId = ReqDoc.get("imageId");
                                                            nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                            if (nNowPage <= nImagePage) {
                                                                // 파일 dpi 정보 확인
                                                                // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                                // tmpPos = tmpPos + "," + imageDPI;
                                                                break;
                                                            } else {
                                                                nNowPage = nNowPage - nImagePage;
                                                            }
                                                        }
                                                    }
                                                    tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                                    tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                                    tmpMapRecog.put("ocrCol", Integer.toString(11));                        // OCR컬럼(인식항목이름)
                                                    tmpMapRecog.put("ocrColIdx", Integer.toString(nvalCnt));                    // OCR동일컬럼배열인덱스
                                                    tmpMapRecog.put("ocrColCnt", Integer.toString(nCnt));                        // OCR동일컬럼배열개수
                                                    tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                                    tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표
                                                    tmpOcrProcData1.add(tmpMapRecog);


                                                    //금액
                                                    tmpMapRecog = new HashMap<String, Object>();

                                                    tmpMapRecog.put("docType", stridCode);

                                                    // 이미지 id, 좌표값 분리
                                                    tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt + nCnt));
                                                    // 마지막 | 제거
                                                    tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                    //split
                                                    tmpOrgList = tmpOrgVal.split("[|]");
                                                    tmpVal = "";
                                                    tmpPos = "";
                                                    if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                        nErrCnt++;
                                                        tmpVal = "";
                                                        tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                        strImageId = "";
                                                        nNowPage = -1;
                                                    } else {
                                                        nRegocCnt++;
                                                        // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                        tmpVal = tmpOrgList[0];
                                                        tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                        nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                        for (Map<String, String> ReqDoc : DocList) {
                                                            strImageId = ReqDoc.get("imageId");
                                                            nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                            if (nNowPage <= nImagePage) {
                                                                // 파일 dpi 정보 확인
                                                                // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                                // tmpPos = tmpPos + "," + imageDPI;
                                                                break;
                                                            } else {
                                                                nNowPage = nNowPage - nImagePage;
                                                            }
                                                        }
                                                    }
                                                    tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                                    tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                                    tmpMapRecog.put("ocrCol", Integer.toString(12));                        // OCR컬럼(인식항목이름)
                                                    tmpMapRecog.put("ocrColIdx", Integer.toString(nvalCnt));                    // OCR동일컬럼배열인덱스
                                                    tmpMapRecog.put("ocrColCnt", Integer.toString(nCnt));                        // OCR동일컬럼배열개수
                                                    tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                                    tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표
                                                    tmpOcrProcData1.add(tmpMapRecog);


                                                    //차대변여부
                                                    tmpMapRecog = new HashMap<String, Object>();

                                                    tmpMapRecog.put("docType", stridCode);

                                                    // 이미지 id, 좌표값 분리
                                                    tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt + nCnt + nCnt));
                                                    // 마지막 | 제거
                                                    tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                    //split
                                                    tmpOrgList = tmpOrgVal.split("[|]");
                                                    tmpVal = "";
                                                    tmpPos = "";
                                                    // 차대변여부는 NotAdvised 값까지 무시처리
                                                    if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0]) || "NotAdvised".equals(tmpOrgList[0])) {
                                                        nErrCnt++;
                                                        tmpVal = "";
                                                        tmpPos = "" + "," + "" + "," + "" + "," + "";
                                                        strImageId = "";
                                                        nNowPage = -1;
                                                    } else {
                                                        nRegocCnt++;
                                                        // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                        tmpVal = tmpOrgList[0];
                                                        tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                        nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                        for (Map<String, String> ReqDoc : DocList) {
                                                            strImageId = ReqDoc.get("imageId");
                                                            nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                            if (nNowPage <= nImagePage) {
                                                                // 파일 dpi 정보 확인
                                                                // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                                // tmpPos = tmpPos + "," + imageDPI;
                                                                break;
                                                            } else {
                                                                nNowPage = nNowPage - nImagePage;
                                                            }
                                                        }
                                                    }
                                                    tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                                    tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                                    tmpMapRecog.put("ocrCol", Integer.toString(13));                        // OCR컬럼(인식항목이름)
                                                    tmpMapRecog.put("ocrColIdx", Integer.toString(nvalCnt));                    // OCR동일컬럼배열인덱스
                                                    tmpMapRecog.put("ocrColCnt", Integer.toString(nCnt));                        // OCR동일컬럼배열개수
                                                    tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                                    tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표
                                                    tmpOcrProcData1.add(tmpMapRecog);

                                                    // for문 loop가 끝나면 itemcnt를 반복부 다음 값으로 변경
                                                    if (j == nCnt) {
                                                        itemcnt = itemcnt + nCnt + nCnt;
                                                    }
                                                }
                                            }
                                            // 수재계약번호처리
                                            itemcnt++;
                                            tmpMapRecog = new HashMap<String, Object>();

                                            tmpMapRecog.put("docType", stridCode);

                                            // 이미지 id, 좌표값 분리
                                            tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            tmpOrgList = tmpOrgVal.split("[|]");
                                            tmpVal = "";
                                            tmpPos = "";
                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                nErrCnt++;
                                                tmpVal = "";
                                                tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                strImageId = "";
                                                nNowPage = -1;
                                            } else {
                                                nRegocCnt++;
                                                // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                tmpVal = tmpOrgList[0];
                                                tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                for (Map<String, String> ReqDoc : DocList) {
                                                    strImageId = ReqDoc.get("imageId");
                                                    nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                    if (nNowPage <= nImagePage) {
                                                        // 파일 dpi 정보 확인
                                                        // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                        // tmpPos = tmpPos + "," + imageDPI;
                                                        break;
                                                    } else {
                                                        nNowPage = nNowPage - nImagePage;
                                                    }
                                                }
                                            }
                                            tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                            tmpMapRecog.put("ocrCol", Integer.toString(7));                        // OCR컬럼(인식항목이름)
                                            tmpMapRecog.put("ocrColIdx", Integer.toString(1));                    // OCR동일컬럼배열인덱스
                                            tmpMapRecog.put("ocrColCnt", Integer.toString(1));                        // OCR동일컬럼배열개수
                                            tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                            tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표
                                            //tmpOcrProcData1.add(tmpMapRecog);
                                            OcrProcData.add(tmpMapRecog);

                                            // Outstanding Loss Reserve 처리
                                            // 제일 마지막항목에 금액이 있으면 Outstanding Loss Reserve 항목으로 추가
                                            itemcnt = itemcnt + 2;
                                            tmpMapRecog = new HashMap<String, Object>();

                                            tmpMapRecog.put("docType", stridCode);

                                            // 이미지 id, 좌표값 분리
                                            tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            tmpOrgList = tmpOrgVal.split("[|]");
                                            tmpVal = "";
                                            tmpPos = "";
                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                // 값이 없으면 무시
                                                OcrProcData.addAll(tmpOcrProcData1);
                                                continue;
                                            } else {
                                                // 기존항목 ocrColCnt 값 수정
                                                for (Map<String, Object> tmpmap : tmpOcrProcData1) {
                                                    tmpmap.put("ocrColCnt", Integer.toString(nvalCnt + 1));
                                                }

                                                nRegocCnt++;
                                                // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                tmpVal = tmpOrgList[0];
                                                tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                for (Map<String, String> ReqDoc : DocList) {
                                                    strImageId = ReqDoc.get("imageId");
                                                    nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                    if (nNowPage <= nImagePage) {
                                                        // 파일 dpi 정보 확인
                                                        // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                        // tmpPos = tmpPos + "," + imageDPI;
                                                        break;
                                                    } else {
                                                        nNowPage = nNowPage - nImagePage;
                                                    }
                                                }
                                            }
                                            tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                            tmpMapRecog.put("ocrCol", Integer.toString(11));                        // OCR컬럼(인식항목이름)
                                            tmpMapRecog.put("ocrColIdx", Integer.toString(nvalCnt + 1));                    // OCR동일컬럼배열인덱스
                                            tmpMapRecog.put("ocrColCnt", Integer.toString(nvalCnt + 1));                        // OCR동일컬럼배열개수
                                            tmpMapRecog.put("ocrOrgVal", "Outstanding Loss Reserve");                    // OCR결과값
                                            tmpMapRecog.put("ocrCoord", ",,,");                                        // OCR좌표
                                            tmpOcrProcData1.add(tmpMapRecog);
                                            //OcrProcData.addAll(tmpOcrProcData1);


                                            tmpMapRecog = new HashMap<String, Object>();

                                            tmpMapRecog.put("docType", stridCode);
                                            tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                            tmpMapRecog.put("ocrCol", Integer.toString(12));                        // OCR컬럼(인식항목이름)
                                            tmpMapRecog.put("ocrColIdx", Integer.toString(nvalCnt + 1));                    // OCR동일컬럼배열인덱스
                                            tmpMapRecog.put("ocrColCnt", Integer.toString(nvalCnt + 1));                        // OCR동일컬럼배열개수
                                            tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                            tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표
                                            tmpOcrProcData1.add(tmpMapRecog);
                                            //OcrProcData.addAll(tmpOcrProcData1);

                                            //Outstanding Loss Reserve 차대변여부 없음
                                            tmpMapRecog = new HashMap<String, Object>();

                                            tmpMapRecog.put("docType", stridCode);
                                            tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog.put("imagePage", Integer.toString(-1));                    // IMAGE_PAGE
                                            tmpMapRecog.put("ocrCol", Integer.toString(13));                        // OCR컬럼(인식항목이름)
                                            tmpMapRecog.put("ocrColIdx", Integer.toString(nvalCnt + 1));                    // OCR동일컬럼배열인덱스
                                            tmpMapRecog.put("ocrColCnt", Integer.toString(nvalCnt + 1));                        // OCR동일컬럼배열개수
                                            tmpMapRecog.put("ocrOrgVal", "");                                        // OCR결과값
                                            tmpMapRecog.put("ocrCoord", ",,,");                                        // OCR좌표
                                            tmpOcrProcData1.add(tmpMapRecog);

                                            OcrProcData.addAll(tmpOcrProcData1);


                                        } else {

                                            tmpMapRecog.put("docType", stridCode);            // 서식코드

                                            // 이미지 id, 좌표값 분리
                                            tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            String[] tmpOrgList = tmpOrgVal.split("[|]");
                                            tmpVal = "";
                                            tmpPos = "";
                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                nErrCnt++;
                                                tmpVal = "";
                                                tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                strImageId = "";
                                                nNowPage = -1;
                                            } else {
                                                nRegocCnt++;
                                                // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right


                                                tmpVal = tmpOrgList[0];
                                                tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                for (Map<String, String> ReqDoc : DocList) {
                                                    strImageId = ReqDoc.get("imageId");
                                                    nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                    if (nNowPage <= nImagePage) {
                                                        // 파일 dpi 정보 확인
                                                        // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                        // tmpPos = tmpPos + "," + imageDPI;
                                                        break;
                                                    } else {
                                                        nNowPage = nNowPage - nImagePage;
                                                    }
                                                }
                                            }

                                            nInsurIdx = 0;
                                            if (itemcnt == 7 || itemcnt == 8 || itemcnt == 9) {
                                                nInsurIdx = itemcnt + 1;
                                            } else {
                                                nInsurIdx = itemcnt;
                                            }
                                            tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                            tmpMapRecog.put("ocrCol", Integer.toString(nInsurIdx));                        // OCR컬럼(인식항목이름)
                                            tmpMapRecog.put("ocrColIdx", "1");                                            // OCR동일컬럼배열인덱스
                                            tmpMapRecog.put("ocrColCnt", "1");                                            // OCR동일컬럼배열개수
                                            tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                            tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표

                                            //LocalDateTime regDate = LocalDateTime.now();
                                            //tmpMapRecog.put("REG_DTTM", regDate.format(formatter));							// 등록시간
                                            OcrProcData.add(tmpMapRecog);

                                            //nRegocCnt++;
                                        }
                                    }
                                    // 임대사업자등록증 추가
                                    else if ("SGI0002".equals(stridCode)) {
                                        /*
                                         * docrecog 리턴값 순서
                                         * 1: 최초등록일
                                         * 2: 성명(법인명)
                                         * 3: 등록번호
                                         * 4: 건물주소(다수)
                                         * 5: 호/실/층(다수)
                                         * 6: 주택구분(다수)
                                         * 7: 주택유형(다수)
                                         */

                                        String tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));

                                        if (itemcnt == 4 || itemcnt == 5 || itemcnt == 6 || itemcnt == 7) {
                                            // 인식항목이 없을때 continue
											/*
											if(tmpOrgVal.contains("NoValue")) {
												continue;
											}
											*/

                                            JSONObject tmpjsonVal = jsonValue.getJSONObject(Integer.toString(itemcnt));

                                            if (itemcnt == 4) {
                                                nSGI0002cnt = Integer.parseInt(tmpjsonVal.getString("Count"));
                                            }

                                            for (int j = 1; j <= nSGI0002cnt; j++) {

                                                tmpMapRecog = new HashMap<String, Object>();

                                                tmpMapRecog.put("docType", DocList.get(0).get("imageDocCode"));            // 서식코드

                                                tmpOrgVal = tmpjsonVal.getString(Integer.toString(j));

                                                // 마지막 | 제거
                                                tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                //split
                                                String[] tmpOrgList = tmpOrgVal.split("[|]");
                                                String tmpVal = "";
                                                String tmpPos = "";

                                                if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                    nErrCnt++;
                                                    tmpVal = "";
                                                    tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                    strImageId = "";
                                                    nNowPage = -1;
                                                } else {
                                                    nRegocCnt++;
                                                    // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                    tmpVal = tmpOrgList[0];

                                                    // itemcnt == 4(건물주소) 일때 합계 항목이 나오면 nCount - 1 처리후 break
                                                    // 합계 ~~~~ 형식으로 뒤에 가비지 문구가 붙는 경우가 있어서 같이 제외
                                                    if (itemcnt == 4 && ("합계".equals(tmpVal) || tmpVal.length() <= 3 || (tmpVal.contains("합계 ") && tmpVal.length() > 3))) {
                                                        nSGI0002cnt = nSGI0002cnt - 1;

                                                        for (Map<String, Object> tmpOcrProcData : OcrProcData) {
                                                            if ("4".equals(tmpOcrProcData.get("ocrCol"))) {
                                                                // 최종 ocrColCnt 반영 nStartCnt
                                                                tmpOcrProcData.put("ocrColCnt", Integer.toString(nSGI0002cnt));
                                                            }
                                                        }

                                                        break;
                                                    }

                                                    //tmpVal=tmpOrgList[0];
                                                    tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                    nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                    for (Map<String, String> ReqDoc : DocList) {
                                                        strImageId = ReqDoc.get("imageId");
                                                        nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                        if (nNowPage <= nImagePage) {
                                                            // 파일 dpi 정보 확인
                                                            // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                            // tmpPos = tmpPos + "," + imageDPI;
                                                            break;
                                                        } else {
                                                            nNowPage = nNowPage - nImagePage;
                                                        }
                                                    }
                                                }

                                                tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                                tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                                tmpMapRecog.put("ocrCol", Integer.toString(itemcnt));                        // OCR컬럼(인식항목이름)
                                                tmpMapRecog.put("ocrColIdx", Integer.toString(j));                            // OCR동일컬럼배열인덱스
                                                tmpMapRecog.put("ocrColCnt", Integer.toString(nSGI0002cnt));                        // OCR동일컬럼배열개수
                                                tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                                tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표

                                                //LocalDateTime regDate = LocalDateTime.now();
                                                //tmpMapRecog.put("REG_DTTM", regDate.format(formatter));							// 등록시간
                                                OcrProcData.add(tmpMapRecog);

                                            }
                                        } else {

                                            tmpMapRecog.put("docType", DocList.get(0).get("imageDocCode"));            // 서식코드

                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            String[] tmpOrgList = tmpOrgVal.split("[|]");
                                            String tmpVal = "";
                                            String tmpPos = "";

                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                nErrCnt++;
                                                tmpVal = "";
                                                tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                strImageId = "";
                                                nNowPage = -1;
                                            } else {
                                                nRegocCnt++;
                                                // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                tmpVal = tmpOrgList[0];
                                                // 12번항목 쓰래기값 제거
                                                if (itemcnt == 12) {
                                                    tmpVal = tmpVal.replace("청구취지 및 항소취지", "");
                                                    tmpVal = tmpVal.replace("청구취지", "");
                                                }
                                                tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                for (Map<String, String> ReqDoc : DocList) {
                                                    strImageId = ReqDoc.get("imageId");
                                                    nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                    if (nNowPage <= nImagePage) {
                                                        // 파일 dpi 정보 확인
                                                        // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                        // tmpPos = tmpPos + "," + imageDPI;
                                                        break;
                                                    } else {
                                                        nNowPage = nNowPage - nImagePage;
                                                    }
                                                }
                                            }

                                            tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                            tmpMapRecog.put("ocrCol", Integer.toString(itemcnt));                        // OCR컬럼(인식항목이름)
                                            tmpMapRecog.put("ocrColIdx", "1");                                            // OCR동일컬럼배열인덱스
                                            tmpMapRecog.put("ocrColCnt", "1");                                            // OCR동일컬럼배열개수
                                            tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                            tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표

                                            //LocalDateTime regDate = LocalDateTime.now();
                                            //tmpMapRecog.put("REG_DTTM", regDate.format(formatter));							// 등록시간
                                            OcrProcData.add(tmpMapRecog);
                                        }
                                    }
                                    // 소장 추가
                                    else if ("SGI0053".equals(stridCode)) {
                                        /*
                                         * docrecog 리턴값 순서
                                         * 1: 법원명
                                         * 2: 지원명
                                         * 3: 사건번호
                                         * 4: 소송명
                                         * 5: 원고명(다수)
                                         * 6: 피고명(다수)
                                         * 7: 원고소송대리인(다수)
                                         * 8: 피고소송대리인(다수)
                                         * 9: 청구취지(소가)(다수)
                                         * 10: 청구취지(전체)
                                         * 11: 소장내용(전체)
                                         */

                                        String tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));

                                        if (itemcnt == 5 || itemcnt == 6 || itemcnt == 7 || itemcnt == 8 || itemcnt == 9) {
                                            // 인식항목이 없을때 continue
                                            if (tmpOrgVal.contains("NoValue")) {
                                                continue;
                                            }

                                            JSONObject tmpjsonVal = jsonValue.getJSONObject(Integer.toString(itemcnt));

                                            int nCount = Integer.parseInt(tmpjsonVal.getString("Count"));

                                            for (int j = 1; j <= nCount; j++) {

                                                tmpMapRecog = new HashMap<String, Object>();

                                                tmpMapRecog.put("docType", DocList.get(0).get("imageDocCode"));            // 서식코드

                                                tmpOrgVal = tmpjsonVal.getString(Integer.toString(j));

                                                // 마지막 | 제거
                                                tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                //split
                                                String[] tmpOrgList = tmpOrgVal.split("[|]");
                                                String tmpVal = "";
                                                String tmpPos = "";

                                                if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                    nErrCnt++;
                                                    tmpVal = "";
                                                    tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                    strImageId = "";
                                                    nNowPage = -1;
                                                } else {
                                                    nRegocCnt++;
                                                    // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                    tmpVal = tmpOrgList[0];
                                                    tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                    nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                    for (Map<String, String> ReqDoc : DocList) {
                                                        strImageId = ReqDoc.get("imageId");
                                                        nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                        if (nNowPage <= nImagePage) {
                                                            // 파일 dpi 정보 확인
                                                            // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                            // tmpPos = tmpPos + "," + imageDPI;
                                                            break;
                                                        } else {
                                                            nNowPage = nNowPage - nImagePage;
                                                        }
                                                    }
                                                }

                                                tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                                tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                                tmpMapRecog.put("ocrCol", Integer.toString(itemcnt));                        // OCR컬럼(인식항목이름)
                                                tmpMapRecog.put("ocrColIdx", Integer.toString(j));                            // OCR동일컬럼배열인덱스
                                                tmpMapRecog.put("ocrColCnt", Integer.toString(nCount));                        // OCR동일컬럼배열개수
                                                tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                                tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표

                                                //LocalDateTime regDate = LocalDateTime.now();
                                                //tmpMapRecog.put("REG_DTTM", regDate.format(formatter));							// 등록시간
                                                OcrProcData.add(tmpMapRecog);

                                            }
                                        } else {

                                            tmpMapRecog.put("docType", DocList.get(0).get("imageDocCode"));            // 서식코드

                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            String[] tmpOrgList = tmpOrgVal.split("[|]");
                                            String tmpVal = "";
                                            String tmpPos = "";

                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                nErrCnt++;
                                                tmpVal = "";
                                                tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                strImageId = "";
                                                nNowPage = -1;
                                            } else {
                                                nRegocCnt++;
                                                // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                tmpVal = tmpOrgList[0];
                                                if (itemcnt == 10) {
                                                    int indexOfGoal = 0;
                                                    indexOfGoal = tmpVal.indexOf("청구원인");
                                                    if (indexOfGoal != -1)
                                                        tmpVal = tmpVal.substring(0, indexOfGoal);
                                                }

                                                if (itemcnt == 11) {
                                                    if (tmpVal.length() > 25) {
                                                        String tempValue = tmpVal.substring(0, 25);
                                                        Boolean isKor = false;

                                                        int indexOfGoal = 0;
                                                        indexOfGoal = tempValue.indexOf("1.");
                                                        if (indexOfGoal != -1) {
                                                            tempValue = tempValue.substring(0, indexOfGoal);
                                                            for (int check = 0; check < tempValue.length(); check++) {
                                                                if (Character.getType(tempValue.charAt(check)) == 5) {
                                                                    isKor = true;
                                                                    break;
                                                                }
                                                            }

                                                            if (isKor == false) {
                                                                tmpVal = tmpVal.substring(indexOfGoal, tmpVal.length());
                                                            }
                                                        }
                                                    }
                                                }

                                                tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                for (Map<String, String> ReqDoc : DocList) {
                                                    strImageId = ReqDoc.get("imageId");
                                                    nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                    if (nNowPage <= nImagePage) {
                                                        // 파일 dpi 정보 확인
                                                        // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                        // tmpPos = tmpPos + "," + imageDPI;
                                                        break;
                                                    } else {
                                                        nNowPage = nNowPage - nImagePage;
                                                    }
                                                }
                                            }

                                            tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                            tmpMapRecog.put("ocrCol", Integer.toString(itemcnt));                        // OCR컬럼(인식항목이름)
                                            tmpMapRecog.put("ocrColIdx", "1");                                            // OCR동일컬럼배열인덱스
                                            tmpMapRecog.put("ocrColCnt", "1");                                            // OCR동일컬럼배열개수
                                            tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                            tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표

                                            //LocalDateTime regDate = LocalDateTime.now();
                                            //tmpMapRecog.put("REG_DTTM", regDate.format(formatter));							// 등록시간
                                            OcrProcData.add(tmpMapRecog);
                                        }
                                    }
                                    // 판결문 추가
                                    else if ("SGI0054".equals(stridCode)) {
                                        /*
                                         * docrecog 리턴값 순서
                                         * 1: 법원명
                                         * 2: 담당재판부
                                         * 3: 사건번호
                                         * 4: 사건명
                                         * 5: 원고명(다수)
                                         * 6: 피고명(다수)
                                         * 7: 원고소송대리인(다수)
                                         * 8: 피고소송대리인(다수)
                                         * 9: 판결선고일
                                         * 10: 원고보조참가인(다수)
                                         * 11: 피고보조참가인(다수)
                                         * 12: 주문(전체) - 좌표없음
                                         * 13: 이유(전체) - 좌표없음
                                         */

                                        String tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));

                                        if (itemcnt == 5 || itemcnt == 6 || itemcnt == 7 || itemcnt == 8 || itemcnt == 10 || itemcnt == 11) {
                                            // 인식항목이 없을때 continue
                                            if (tmpOrgVal.contains("NoValue")) {
                                                continue;
                                            }

                                            JSONObject tmpjsonVal = jsonValue.getJSONObject(Integer.toString(itemcnt));

                                            int nCount = Integer.parseInt(tmpjsonVal.getString("Count"));

                                            for (int j = 1; j <= nCount; j++) {

                                                tmpMapRecog = new HashMap<String, Object>();

                                                tmpMapRecog.put("docType", DocList.get(0).get("imageDocCode"));            // 서식코드

                                                tmpOrgVal = tmpjsonVal.getString(Integer.toString(j));

                                                // 마지막 | 제거
                                                tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                //split
                                                String[] tmpOrgList = tmpOrgVal.split("[|]");
                                                String tmpVal = "";
                                                String tmpPos = "";

                                                if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                    nErrCnt++;
                                                    tmpVal = "";
                                                    tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                    strImageId = "";
                                                    nNowPage = -1;
                                                } else {
                                                    nRegocCnt++;
                                                    // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                    tmpVal = tmpOrgList[0];
                                                    tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                    nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                    for (Map<String, String> ReqDoc : DocList) {
                                                        strImageId = ReqDoc.get("imageId");
                                                        nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                        if (nNowPage <= nImagePage) {
                                                            // 파일 dpi 정보 확인
                                                            // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                            // tmpPos = tmpPos + "," + imageDPI;
                                                            break;
                                                        } else {
                                                            nNowPage = nNowPage - nImagePage;
                                                        }
                                                    }
                                                }

                                                tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                                tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                                tmpMapRecog.put("ocrCol", Integer.toString(itemcnt));                        // OCR컬럼(인식항목이름)
                                                tmpMapRecog.put("ocrColIdx", Integer.toString(j));                            // OCR동일컬럼배열인덱스
                                                tmpMapRecog.put("ocrColCnt", Integer.toString(nCount));                        // OCR동일컬럼배열개수
                                                tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                                tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표

                                                //LocalDateTime regDate = LocalDateTime.now();
                                                //tmpMapRecog.put("REG_DTTM", regDate.format(formatter));							// 등록시간
                                                OcrProcData.add(tmpMapRecog);

                                            }
                                        } else {
                                            tmpMapRecog.put("docType", DocList.get(0).get("imageDocCode"));            // 서식코드

                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            String[] tmpOrgList = tmpOrgVal.split("[|]");
                                            String tmpVal = "";
                                            String tmpPos = "";

                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                nErrCnt++;
                                                tmpVal = "";
                                                tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                strImageId = "";
                                                nNowPage = -1;
                                            } else {
                                                nRegocCnt++;
                                                // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                tmpVal = tmpOrgList[0];

                                                if (itemcnt == 12) {
                                                    //tmpVal = tmpVal.replace("청구취지 및 항소취지", "");
                                                    //tmpVal = tmpVal.replace("청구취지", "");
                                                    int indexOfGoal = 0;
                                                    indexOfGoal = tmpVal.indexOf("청구취지");
                                                    //System.out.println(+indexOfGoal);
                                                    if (indexOfGoal != -1)
                                                        tmpVal = tmpVal.substring(0, indexOfGoal);

                                                    //System.out.println("청구취지 = "+tmpVal);
                                                }

                                                tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                for (Map<String, String> ReqDoc : DocList) {
                                                    strImageId = ReqDoc.get("imageId");
                                                    nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                    if (nNowPage <= nImagePage) {
                                                        // 파일 dpi 정보 확인
                                                        // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                        // tmpPos = tmpPos + "," + imageDPI;
                                                        break;
                                                    } else {
                                                        nNowPage = nNowPage - nImagePage;
                                                    }
                                                }
                                            }

                                            tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                            tmpMapRecog.put("ocrCol", Integer.toString(itemcnt));                        // OCR컬럼(인식항목이름)
                                            tmpMapRecog.put("ocrColIdx", "1");                                            // OCR동일컬럼배열인덱스
                                            tmpMapRecog.put("ocrColCnt", "1");                                            // OCR동일컬럼배열개수
                                            tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                            tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표

                                            //LocalDateTime regDate = LocalDateTime.now();
                                            //tmpMapRecog.put("REG_DTTM", regDate.format(formatter));							// 등록시간
                                            OcrProcData.add(tmpMapRecog);
                                        }
                                    }
                                    // 담보제공명령 첫페이지 추가
                                    else if ("SGI0052".equals(stridCode)) {

                                        String tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));

                                        if (itemcnt == 6 || itemcnt == 7 || itemcnt == 8 || itemcnt == 9 || itemcnt == 11 || itemcnt == 12) { //khh
                                            // 인식항목이 없을때 continue
                                            if (tmpOrgVal.contains("NoValue")) {
                                                continue;
                                            }


                                            JSONObject tmpjsonVal = jsonValue.getJSONObject(Integer.toString(itemcnt));

                                            int nCount = Integer.parseInt(tmpjsonVal.getString("Count"));

                                            for (int j = 1; j <= nCount; j++) {

                                                tmpMapRecog = new HashMap<String, Object>();

                                                // 담보제공명령 뒷페이지(공탁금계좌납입안내문) 와 서식 분리를 위해 docrecog 서식코드 반환
                                                tmpMapRecog.put("docType", stridCode);                                    // 서식코드

                                                tmpOrgVal = tmpjsonVal.getString(Integer.toString(j));

                                                // 마지막 | 제거
                                                tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                                //split
                                                String[] tmpOrgList = tmpOrgVal.split("[|]");
                                                String tmpVal = "";
                                                String tmpPos = "";

                                                if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                    nErrCnt++;
                                                    tmpVal = "";
                                                    tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                    strImageId = "";
                                                    nNowPage = -1;
                                                } else {
                                                    nRegocCnt++;
                                                    // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                    tmpVal = tmpOrgList[0];
                                                    tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                    nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                    for (Map<String, String> ReqDoc : DocList) {
                                                        strImageId = ReqDoc.get("imageId");
                                                        nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                        if (nNowPage <= nImagePage) {
                                                            // 파일 dpi 정보 확인
                                                            // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                            // tmpPos = tmpPos + "," + imageDPI;
                                                            break;
                                                        } else {
                                                            nNowPage = nNowPage - nImagePage;
                                                        }
                                                    }
                                                }

                                                tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                                tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                                tmpMapRecog.put("ocrCol", Integer.toString(itemcnt));                        // OCR컬럼(인식항목이름)
                                                tmpMapRecog.put("ocrColIdx", Integer.toString(j));                            // OCR동일컬럼배열인덱스
                                                tmpMapRecog.put("ocrColCnt", Integer.toString(nCount));                        // OCR동일컬럼배열개수
                                                tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                                tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표

                                                //LocalDateTime regDate = LocalDateTime.now();
                                                //tmpMapRecog.put("REG_DTTM", regDate.format(formatter));							// 등록시간
                                                OcrProcData.add(tmpMapRecog);

                                            }
                                        } else {

                                            // 담보제공명령 뒷페이지(공탁금계좌납입안내문) 와 서식 분리를 위해 docrecog 서식코드 반환
                                            tmpMapRecog.put("docType", stridCode);                                    // 서식코드

                                            // 마지막 | 제거
                                            tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                            //split
                                            String[] tmpOrgList = tmpOrgVal.split("[|]");
                                            String tmpVal = "";
                                            String tmpPos = "";

                                            if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                                nErrCnt++;
                                                tmpVal = "";
                                                tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                                strImageId = "";
                                                nNowPage = -1;
                                            } else {
                                                nRegocCnt++;
                                                // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right
                                                tmpVal = tmpOrgList[0];
                                                tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                                for (Map<String, String> ReqDoc : DocList) {
                                                    strImageId = ReqDoc.get("imageId");
                                                    nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                    if (nNowPage <= nImagePage) {
                                                        // 파일 dpi 정보 확인
                                                        // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                        // tmpPos = tmpPos + "," + imageDPI;
                                                        break;
                                                    } else {
                                                        nNowPage = nNowPage - nImagePage;
                                                    }
                                                }
                                            }

                                            tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                            tmpMapRecog.put("ocrCol", Integer.toString(itemcnt));                        // OCR컬럼(인식항목이름)
                                            tmpMapRecog.put("ocrColIdx", "1");                                            // OCR동일컬럼배열인덱스
                                            tmpMapRecog.put("ocrColCnt", "1");                                            // OCR동일컬럼배열개수
                                            tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                            tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표

                                            //LocalDateTime regDate = LocalDateTime.now();
                                            //tmpMapRecog.put("REG_DTTM", regDate.format(formatter));							// 등록시간
                                            OcrProcData.add(tmpMapRecog);
                                        }
                                    } else {
                                        // 법원문서는 docrecog에서 여러개의 서식으로 분류되기 때문에 OCR_COL이 중복됨. 법원문서만 docrecog에서 리턴하는 서식코드를 사용할것
                                        // 법무비용입증자료도 서식 분리를 위해 docrecog 서식코드 반환
                                        // 자격증도 docrecog 서식코드 반환
                                        // 공탁금계좌납입안내문도 docrecog 서식코드 반환 (law2 서식이 다른 서식과 섞여있어서 "SGI0055".equals(stridCode) 로 구분)
                                        if (lawDocCode.contains(DocList.get(0).get("imageDocCode")) || legalfeeDocCode.contains(DocList.get(0).get("imageDocCode"))
                                                || licenseDocCode.contains(DocList.get(0).get("imageDocCode")) || "SGI0055".equals(stridCode)) {
                                            tmpMapRecog.put("docType", stridCode);                                    // 서식코드
                                        } else {
                                            tmpMapRecog.put("docType", DocList.get(0).get("imageDocCode"));            // 서식코드
                                        }

                                        // 법원문서 첫페이지(SGI0003) 2~7번이 인식값이 없는 오인식 저장 안하도록 처리
                                        if ("SGI0003".equals(stridCode)) {
                                            if (jsonValue.getString(Integer.toString(2)).contains("NoValue")
                                                    && jsonValue.getString(Integer.toString(3)).contains("NoValue")
                                                    && jsonValue.getString(Integer.toString(4)).contains("NoValue")
                                                    && jsonValue.getString(Integer.toString(5)).contains("NoValue")
                                                    && jsonValue.getString(Integer.toString(6)).contains("NoValue")
                                                    && jsonValue.getString(Integer.toString(7)).contains("NoValue")
                                                    && jsonValue.getString(Integer.toString(8)).contains("NoValue")) {
                                                continue;
                                            }
                                        }

                                        // 변제계획(안) 오인식 저장 안하도록 처리
                                        if ("SGI0005".equals(stridCode)) {
                                            if (jsonValue.getString(Integer.toString(8)).contains("NoValue")
                                                    && jsonValue.getString(Integer.toString(9)).contains("NoValue")
                                                    && jsonValue.getString(Integer.toString(10)).contains("NoValue")
                                                    && jsonValue.getString(Integer.toString(11)).contains("NoValue")
                                                    && jsonValue.getString(Integer.toString(12)).contains("NoValue")) {
                                                continue;
                                            }
                                        }

                                        // 이미지 id, 좌표값 분리
                                        String tmpOrgVal = jsonValue.getString(Integer.toString(itemcnt));
                                        // 마지막 | 제거
                                        tmpOrgVal = tmpOrgVal.substring(0, tmpOrgVal.length() - 1);
                                        //split
                                        String[] tmpOrgList = tmpOrgVal.split("[|]");
                                        String tmpVal = "";
                                        String tmpPos = "";
                                        if ("".equals(tmpOrgList[0]) || "NoValue".equals(tmpOrgList[0])) {
                                            // 개인회생문서 첫페이지 (SGI0003) 7번째 항목 지원, 지법 처리
                                            // 담보제공명령(SGI0052) 14번째항목  지원, 지법 처리 추가 ->  -> 담보제공명령은 제외하고 인식기능 분리됨
                                            if (!("SGI0003".equals(stridCode) && (itemcnt == 7 || itemcnt == 8))) {
                                                nErrCnt++;
                                            }
                                            tmpVal = "";
                                            tmpPos = "" + "," + "" + "," + "" + "," + "" + "," + "" + "," + "";
                                            strImageId = "";
                                            nNowPage = -1;
                                        } else {
                                            // 개인회생문서 첫페이지 (SGI0003) 7번째 항목 지원, 지법 처리
                                            // 담보제공명령(SGI0052) 14번째항목  지원, 지법 처리 추가 ->  -> 담보제공명령은 제외하고 인식기능 분리됨
                                            if (!("SGI0003".equals(stridCode) && (itemcnt == 7 || itemcnt == 8))) {
                                                nRegocCnt++;
                                            }

                                            // 0:인식값, 1:이미지page, 2:top, 3:left, 4:bottom, 5:right

                                            // 통장사본 계좌번호일때 숫자, - 이외는 제거
                                            if (BankBookDocCode.contains(DocList.get(0).get("imageDocCode")) && itemcnt == 2) {
                                                //LOGGER.info("계좌번호 : "+ tmpOrgList[0] + ", 문자제거 후 : " + tmpOrgList[0].replaceAll("[^0-9-]", ""));
                                                tmpVal = tmpOrgList[0].replaceAll("[^0-9-]", "");
                                            } else {
                                                tmpVal = tmpOrgList[0];
                                            }

                                            //tmpVal=tmpOrgList[0];
                                            tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                            nNowPage = Integer.parseInt(tmpOrgList[1]) + 1;

                                            for (Map<String, String> ReqDoc : DocList) {
                                                strImageId = ReqDoc.get("imageId");
                                                nImagePage = Integer.parseInt(ReqDoc.get("imageCnt"));
                                                if (nNowPage <= nImagePage) {
                                                    // 파일 dpi 정보 확인
                                                    // String imageDPI = getImageDPI(ReqDoc.get("filePath"));
                                                    tmpPos = tmpPos + "," + "200,200";
                                                    break;
                                                } else {
                                                    nNowPage = nNowPage - nImagePage;
                                                }
                                            }
                                        }

                                        // 개인회생문서 첫페이지 (SGI0003) 7번째 항목 지원, 지법 처리
                                        // 담보제공명령(SGI0052) 14번째항목  지원, 지법 처리 추가 -> 담보제공명령은 제외하고 인식기능 분리됨
                                        if (("SGI0003".equals(stridCode) && (itemcnt == 7 || itemcnt == 8))) {
                                            if ("".equals(tmpVal)) {
                                                continue;
                                            }

                                            for (Map<String, Object> tmpmap : OcrProcData) {
                                                if ("1".equals(tmpmap.get("ocrCol"))) {
                                                    tmpVal = tmpmap.get("ocrOrgVal").toString() + " " + tmpVal;

                                                    tmpPos = tmpOrgList[2] + "," + tmpOrgList[3] + "," + tmpOrgList[4] + "," + tmpOrgList[5];

                                                    tmpmap.put("imagePage", Integer.toString(nNowPage));
                                                    tmpmap.put("ocrOrgVal", tmpVal);
                                                    tmpmap.put("ocrCoord", tmpPos);

                                                    break;
                                                } else {
                                                    continue;
                                                }
                                            }
                                        } else {

                                            // 변제예정액표가 한장만 오는 경우가 있어서 변제계획안의 회차정보값을 저장해야됨.
                                            if (("SGI0005".equals(stridCode) && itemcnt == 4)) {
                                                strMaxRound = tmpVal;
                                            }

                                            // 개인회생 첫페이지 사건번호 개희 -> 개회 보정
                                            if (("SGI0003".equals(stridCode) && itemcnt == 2)) {
                                                tmpVal = tmpVal.replace("개희", "개회");
                                            }
                                            // 개인회생문서(SGI0003), 담보제공명령(SGI0052) 첫페이지 사건번호항목의 사건번호 이외값 삭제
                                            if (("SGI0003".equals(stridCode) && itemcnt == 2) || ("SGI0052".equals(stridCode) && itemcnt == 2)) {
                                                String reStr = tmpVal.replaceAll("\\D", "");
                                                if (reStr.length() > 0) {
                                                    reStr = reStr.substring(reStr.length() - 1);
                                                    int index = tmpVal.lastIndexOf(reStr);
                                                    if (index != -1)
                                                        tmpVal = tmpVal.substring(0, index + 1);
                                                }
                                            }

                                            // 괄호 없애기
                                            if ("SGI0003".equals(stridCode) && itemcnt == 4) {
                                                if (tmpVal != null) {
                                                    if (tmpVal.startsWith("(")) {
                                                        tmpVal = tmpVal.substring(1);
                                                    }

                                                    if (tmpVal.endsWith(")")) {
                                                        tmpVal = tmpVal.substring(0, tmpVal.length() - 1);
                                                    }
                                                }
                                            }

                                            tmpMapRecog.put("imageId", strImageId);                                        // IMAGE_ID
                                            tmpMapRecog.put("imagePage", Integer.toString(nNowPage));                    // IMAGE_PAGE
                                            tmpMapRecog.put("ocrCol", Integer.toString(itemcnt));                        // OCR컬럼(인식항목이름)
                                            tmpMapRecog.put("ocrColIdx", "1");                                            // OCR동일컬럼배열인덱스
                                            tmpMapRecog.put("ocrColCnt", "1");                                            // OCR동일컬럼배열개수
                                            tmpMapRecog.put("ocrOrgVal", tmpVal);                                        // OCR결과값
                                            tmpMapRecog.put("ocrCoord", tmpPos);                                        // OCR좌표

                                            //LocalDateTime regDate = LocalDateTime.now();
                                            //tmpMapRecog.put("REG_DTTM", regDate.format(formatter));							// 등록시간
                                            OcrProcData.add(tmpMapRecog);
                                        }
                                        //nRegocCnt++;
                                    }
                                }

                                // 페이지별 인식값 처리 for문 loop end
                                // 번제예정액표는 여기서 값을 저장
                                if (bSGI0006Recog) {
                                    //nStartCnt 값으로 tmpOcrProcData2, tmpOcrProcData3의 각 항목 OCR동일컬럼배열개수 값 수정
                                    for (Map<String, Object> tmpmap : tmpOcrProcData2) {
                                        tmpmap.put("ocrColCnt", Integer.toString(nStartCnt));
                                    }
                                    for (Map<String, Object> tmpmap : tmpOcrProcData3) {
                                        tmpmap.put("ocrColCnt", Integer.toString(nStartCnt));
                                    }

                                    //nItemCnt 값으로 tmpOcrProcData5, tmpOcrProcData6, tmpOcrProcData7 의 각 항목 OCR동일컬럼배열개수 값 수정
                                    for (Map<String, Object> tmpmap : tmpOcrProcData0) {
                                        tmpmap.put("ocrColCnt", Integer.toString(nStartCnt));
                                    }
                                    for (Map<String, Object> tmpmap : tmpOcrProcData5) {
                                        tmpmap.put("ocrColCnt", Integer.toString(nStartCnt));
                                    }
                                    for (Map<String, Object> tmpmap : tmpOcrProcData6) {
                                        tmpmap.put("ocrColCnt", Integer.toString(nStartCnt));
                                    }
                                    for (Map<String, Object> tmpmap : tmpOcrProcData7) {
                                        tmpmap.put("ocrColCnt", Integer.toString(nStartCnt));
                                    }

                                    boolean bItemChk = false;

                                    for (Map<String, Object> tmpOcrProcData : OcrProcData) {
                                        if ("SGI0006".equals(tmpOcrProcData.get("docType")) && "1".equals(tmpOcrProcData.get("ocrCol"))) {
                                            bItemChk = true;
                                            // 기존값이 "" 이고 신규항목 값이 있으면 기존값 삭제후 신규항목 추가, 아니면 신규항목 무시
                                            if ("".equals(tmpOcrProcData.get("ocrOrgVal"))) {
                                                bItemChk = false;
                                                OcrProcData.remove(tmpOcrProcData);
                                                break;
                                                //OcrProcData.addAll(tmpOcrProcData1);
                                            }
                                        }
                                    }

                                    for (Map<String, Object> tmpOcrProcData : OcrProcData) {
                                        if ("SGI0006".equals(tmpOcrProcData.get("docType")) && !"1".equals(tmpOcrProcData.get("ocrCol"))) {
                                            // 최종 ocrColCnt 반영 nStartCnt
                                            tmpOcrProcData.put("ocrColCnt", Integer.toString(nStartCnt));
                                        }
                                    }
                                    // 기존값이 없으면 신규항목 추가
                                    if (bItemChk == false) {
                                        OcrProcData.addAll(tmpOcrProcData1);
                                    }

                                    //OcrProcData.addAll(tmpOcrProcData1);
                                    OcrProcData.addAll(tmpOcrProcData2);
                                    OcrProcData.addAll(tmpOcrProcData3);
                                    OcrProcData.addAll(tmpOcrProcData5);
                                    OcrProcData.addAll(tmpOcrProcData6);
                                    OcrProcData.addAll(tmpOcrProcData7);
                                    OcrProcData.addAll(tmpOcrProcData0);
                                }
                            }
                        }
                    }
                    LOGGER.info("########## docregog 인식값처리 loop end ##########");
                }
            } else {
                // 인식처리 실패상태 추가
                OcrProcSts.put("stsCd", "99");
            }

            // OCR 처리결과 상태 - DocRecog 완료시간 저장
            toDate = LocalDateTime.now();
            OcrProcSts.put("docrecResDttm", toDate.format(formatter));

            // OCR 처리결과 상태 - DocRecog 응답시간
            OcrProcSts.put("docrecResTime", String.valueOf(Duration.between(fromDate, toDate).getSeconds()));

            //nErrCnt++;
            // OCR 처리결과 상태 - 집계항목 추가
            OcrProcSts.put("colCnt", nRegocCnt + nErrCnt);
            OcrProcSts.put("succCnt", nRegocCnt);
            OcrProcSts.put("err400Cnt", nErrCnt);
            OcrProcSts.put("err500Cnt", 0);

            //OcrProcSts.add(tmpMap);

            // 리턴값 생성
            RecogList.put("statusData", OcrProcSts);
            RecogList.put("ocrData", OcrProcData);

            LOGGER.info("########## reqDocRead end ##########");


        }
        //catch(RuntimeException e) {
        //	LOGGER.warn("reqDocRead : "+ e.toString());
        //	throw e;
        //}
        finally {
            // 로민 인식결과 삭제
            File file = new File(strFilePath);
            if (file.exists() && !"dev".equals("dev")) {
                if (file.delete()) {
                    LOGGER.info("########## file.delete() end ##########");
                }
            }
            return RecogList;
        }
    }
}
