package com.arqaam.logframelab.service;

import static java.util.Objects.isNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.arqaam.logframelab.model.IndicatorResponse;
import com.arqaam.logframelab.model.persistence.CRSCode;
import com.arqaam.logframelab.model.persistence.Indicator;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import com.arqaam.logframelab.model.persistence.SDGCode;
import com.arqaam.logframelab.model.persistence.Source;
import org.apache.http.entity.ContentType;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class FirstIndicatorServiceTests extends BaseIndicatorServiceTest {

  @Test
  void extractIndicatorsFromWordFile() throws IOException {
    when(indicatorRepository.findAll()).thenReturn(mockIndicatorList());
    List<IndicatorResponse> expectedResult = getExpectedResult(false);
    MultipartFile file = new MockMultipartFile("test_doc.docx", "test_doc.docx",
        MediaType.APPLICATION_OCTET_STREAM.toString(),
        new ClassPathResource("test_doc.docx").getInputStream());
    List<IndicatorResponse> result = indicatorService.extractIndicatorsFromWordFile(file, null);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(expectedResult, result);
  }

  @Test
  void extractIndicatorsFromWordFile_doc() throws IOException {
    when(indicatorRepository.findAll()).thenReturn(mockIndicatorList());
    List<IndicatorResponse> expectedResult = getExpectedResult(false);
    MultipartFile file = new MockMultipartFile("test doc.doc", "test doc.doc",
        MediaType.APPLICATION_OCTET_STREAM.toString(),
        new ClassPathResource("test doc.doc").getInputStream());
    List<IndicatorResponse> result = indicatorService.extractIndicatorsFromWordFile(file, null);
    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals(expectedResult, result);
  }

  @Test
  void checkIndicators() {
    List<String> wordsToScan = Arrays.asList("food", "government", "policy", "retirement");
    List<Indicator> indicators = mockIndicatorList();
    // Test also indicators without keyword
    indicators.add(Indicator.builder().id(0L).name("Name").description("Description").build());
    Map<Long, Indicator> mapResult = new HashMap<>();
    indicatorService.checkIndicators(wordsToScan, indicators, mapResult);
    indicators = indicators.stream().sorted(Comparator.comparing(Indicator::getId))
        .collect(Collectors.toList());
    assertEquals(indicators.size() - 1, mapResult.size());
    for (int i = 0; i < mapResult.values().size(); i++) {
      assertEquals(indicators.get(i + 1), mapResult.values().toArray()[i]);
    }
  }

  @Test
  void checkIndicators_withIndicatorsWithSameId() {
    List<String> keywordsPolicyList = new ArrayList<>();
    keywordsPolicyList.add("policy");
    List<String> wordsToScan = Arrays.asList("food", "government", "policy", "retirement");
    List<Indicator> indicators = mockIndicatorList();
    indicators.add(Indicator.builder().id(73L).name(
        "Number of policies/strategies/laws/regulation developed/revised for digitalization with EU support")
        .description("Digitalisation").keywords("policy").keywordsList(keywordsPolicyList).build());
    Map<Long, Indicator> mapResult = new HashMap<>();
    indicatorService.checkIndicators(wordsToScan, indicators, mapResult);
    indicators = indicators.stream().sorted(Comparator.comparing(Indicator::getId))
        .collect(Collectors.toList());
    assertEquals(indicators.size() - 1, mapResult.size());
    for (int i = 0; i < indicators.size() - 1; i++) {
      assertEquals(indicators.get(i), mapResult.values().toArray()[i]);
    }
  }

  @Test
  void checkIndicators_withoutIndicators() {
    List<String> wordsToScan = Arrays.asList("food", "government", "policy", "retirement");
    List<Indicator> indicators = new ArrayList<>();
    Map<Long, Indicator> mapResult = new HashMap<>();
    indicatorService.checkIndicators(wordsToScan, indicators, mapResult);

    assertEquals(0, mapResult.size());
  }

  @Test
  void checkIndicators_withoutWordsToScan() {
    List<String> wordsToScan = new ArrayList<>();
    List<Indicator> indicators = new ArrayList<>();
    Map<Long, Indicator> mapResult = new HashMap<>();
    indicatorService.checkIndicators(wordsToScan, indicators, mapResult);

    assertTrue(mapResult.isEmpty());
  }

  @Test
  void exportIndicatorsInWordFile() throws IOException {
    List<Indicator> indicatorList = mockIndicatorList();
    List<Indicator> impactIndicators = indicatorList.stream()
            .filter(x -> x.getLevel().equals(mockLevels[3])).collect(Collectors.toList());
    List<Indicator> outcomeIndicators = indicatorList.stream()
            .filter(x -> x.getLevel().equals(mockLevels[1])).collect(Collectors.toList());
    List<Indicator> outputIndicators = indicatorList.stream()
            .filter(x -> x.getLevel().equals(mockLevels[0])).collect(Collectors.toList());

    List<IndicatorResponse> indicatorResponse = createListIndicatorResponse(indicatorList);
    ByteArrayOutputStream result = indicatorService.exportIndicatorsInWordFile(indicatorResponse);

    assertNotNull(result);
    XWPFDocument resultDoc = new XWPFDocument(new ByteArrayInputStream(result.toByteArray()));
    assertEquals(2, resultDoc.getTables().size());
    XWPFTable table = resultDoc.getTableArray(0);
    Integer rowIndex = 1;
    rowIndex = validateWordTemplateLevel(table, impactIndicators, indicatorResponse, rowIndex);
    rowIndex = validateWordTemplateLevel(table, outcomeIndicators, indicatorResponse, rowIndex);
    rowIndex = validateWordTemplateLevel(table, Collections.emptyList(), indicatorResponse, rowIndex);
    validateWordTemplateLevel(table, outputIndicators, indicatorResponse, rowIndex);
    resultDoc.close();
  }

  @Test
  void exportIndicatorsInWordFile_withoutValuesAndDate() throws IOException {
    List<Indicator> indicatorList = mockIndicatorList().stream().peek(x -> {x.setValue(null); x.setDate(null);}).collect(Collectors.toList());
    lenient().when(indicatorRepository.findAllById(any())).thenReturn(indicatorList);

    List<Indicator> impactIndicators = indicatorList.stream()
            .filter(x -> x.getLevel().equals(mockLevels[3])).collect(Collectors.toList());
    List<Indicator> outcomeIndicators = indicatorList.stream()
            .filter(x -> x.getLevel().equals(mockLevels[1])).collect(Collectors.toList());
    List<Indicator> outputIndicators = indicatorList.stream()
            .filter(x -> x.getLevel().equals(mockLevels[0])).collect(Collectors.toList());

    List<IndicatorResponse> indicatorResponse = createListIndicatorResponse(indicatorList);
    ByteArrayOutputStream result = indicatorService.exportIndicatorsInWordFile(indicatorResponse);

    assertNotNull(result);
    XWPFDocument resultDoc = new XWPFDocument(new ByteArrayInputStream(result.toByteArray()));
    assertEquals(2, resultDoc.getTables().size());
    XWPFTable table = resultDoc.getTableArray(0);
    Integer rowIndex = 1;
    rowIndex = validateWordTemplateLevel(table, impactIndicators, indicatorResponse, rowIndex);
    rowIndex = validateWordTemplateLevel(table, outcomeIndicators, indicatorResponse, rowIndex);
    rowIndex = validateWordTemplateLevel(table, Collections.emptyList(), indicatorResponse, rowIndex);
    validateWordTemplateLevel(table, outputIndicators, indicatorResponse, rowIndex);
    resultDoc.close();
  }

  @Test
  void importIndicators() {
    //TODO this test
//        indicatorService.importIndicators(new ClassPathResource("Indicator.xlsx").getPath());

//        indicatorService.importIndicators("/home/ari/Downloads/Indicator.xlsx");
//        indicatorService.importIndicators("/home/ari/Downloads/SDGs_changed.xlsx");

  }

  @Test
  void exportIndicatorsInWorksheet() throws IOException {
    List<Indicator> expectedResult = mockIndicatorList();

    when(indicatorRepository.findAllById(any())).thenReturn(expectedResult);
    List<IndicatorResponse> indicatorResponse = createListIndicatorResponse(expectedResult);
    ByteArrayOutputStream outputStream = indicatorService
        .exportIndicatorsInWorksheet(indicatorResponse);
    XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(outputStream.toByteArray()));
    XSSFSheet sheet = workbook.getSheetAt(0);
    IndicatorResponse response;
    for (int i = 0; i < expectedResult.size(); i++) {
      XSSFRow row = sheet.getRow(i+1);
      final Indicator indicator = expectedResult.get(i);
      response = indicatorResponse.stream().filter(x -> x.getId() == indicator.getId()).findFirst().get();
      assertEquals(indicator.getLevel().getName(), row.getCell(0).getStringCellValue());
      assertEquals(indicator.getThemes(), row.getCell(1).getStringCellValue());
      assertEquals(indicator.getName(), row.getCell(2).getStringCellValue());
      assertEquals(Optional.ofNullable(indicator.getDescription()).orElse(""), row.getCell(3).getStringCellValue());
      assertEquals(Optional.of(indicator.getSource().stream().map(Source::getName).collect(Collectors.joining(","))).orElse(null), row.getCell(4).getStringCellValue());
      assertEquals(indicator.getDisaggregation() ? "Yes" : "No", row.getCell(5).getStringCellValue());
      assertEquals(Optional.of(indicator.getCrsCode().stream().map(x->String.valueOf(x.getId())).collect(Collectors.joining(","))).orElse(null), row.getCell(6).getStringCellValue());
      assertEquals(Optional.of(indicator.getSdgCode().stream().map(x->String.valueOf(x.getId())).collect(Collectors.joining(","))).orElse(null), row.getCell(7).getStringCellValue());
      assertEquals(Optional.ofNullable(indicator.getSourceVerification()).orElse(""), row.getCell(8).getStringCellValue());
      assertEquals(Optional.ofNullable(indicator.getDataSource()).orElse(""), row.getCell(9).getStringCellValue());
      assertEquals(Optional.ofNullable(response.getValue()).orElse(""), row.getCell(10).getStringCellValue());
      assertEquals(Optional.ofNullable(response.getDate()).orElse(""), row.getCell(11).getStringCellValue());
    }

    // try(OutputStream fileOutputStream = new FileOutputStream("thefilename.xlsx")) {
    //     outputStream.writeTo(fileOutputStream);
    // } catch (IOException e) {
    //     e.printStackTrace();
    // }
  }


  List<Indicator> mockIndicatorList() {
    String keyword = "food insecurity,agriculture";
    List<Indicator> list = new ArrayList<>();

    List<String> keywordsFoodList = new ArrayList<>();
    keywordsFoodList.add("agriculture");
    keywordsFoodList.add("food");

    List<String> keywordsPolicyList = new ArrayList<>();
    keywordsPolicyList.add("policy");

    List<String> keywordsGovList = new ArrayList<>();
    keywordsGovList.add("government");

    List<String> keywordsGovPolicyList = new ArrayList<>();
    keywordsGovPolicyList.add("government policies");
    keywordsGovPolicyList.add("policy");

    list.add(Indicator.builder().id(4L).name(
        "Number of policies/strategies/laws/regulation developed/revised for digitalisation with EU support")
        .description("Digitalisation").level(mockLevels[0]).keywords("policy")
        .keywordsList(keywordsPolicyList)
        .disaggregation(false)
        .source(Collections.singleton(mockSources.get(0))).themes(mockThemes.get(0)).sdgCode(Collections.singleton(mockSdgCodes.get(0)))
        .dataSource("https://data.worldbank.org/indicator/NY.ADJ.DKAP.GN.ZS?view=chart")
        .date("2000")
        .value("100")
        .crsCode(Collections.singleton(mockCrsCodes.get(0))).build());
    list.add(Indicator.builder().id(73L).name(
        "Number of government policies developed or revised with civil society organisation participation through EU support")
        .description("Public Sector").level(mockLevels[1]).keywords("government policies, policy")
        .keywordsList(keywordsGovPolicyList)
        .disaggregation(true)
        .source(Collections.singleton(mockSources.get(1))).themes(mockThemes.get(1)).sdgCode(Collections.singleton(mockSdgCodes.get(1)))
        .dataSource("https://data.worldbank.org/indicator/SE.PRM.TENR.FE?view=chart")
        .date("2001")
        .value("100")
        .crsCode(Collections.singleton(mockCrsCodes.get(1))).build());
    list.add(Indicator.builder().id(5L).name("Revenue, excluding grants (% of GDP)")
        .description("Public Sector").level(mockLevels[3]).keywords("government")
        .keywordsList(keywordsGovList)
        .disaggregation(true)
        .source(Collections.singleton(mockSources.get(2))).themes(mockThemes.get(2)).sdgCode(Collections.singleton(mockSdgCodes.get(2)))
        .dataSource("https://data.worldbank.org/indicator/EG.ELC.ACCS.UR.ZS?view=chart")
        .date("1980")
        .value("50")
        .crsCode(Collections.singleton(mockCrsCodes.get(2))).build());
    list.add(
        Indicator.builder().id(1L).name("Number of food insecure people receiving EU assistance")
            .description("Food & Agriculture").level(mockLevels[1]).keywords(keyword)
            .keywordsList(keywordsFoodList)
            .disaggregation(false)
            .source(Collections.singleton(mockSources.get(3))).themes(mockThemes.get(3)).sdgCode(Collections.singleton(mockSdgCodes.get(3)))
            .dataSource("https://data.worldbank.org/indicator/EG.CFT.ACCS.ZS?view=chart")
            .date("2001")
            .value("100")
            .crsCode(Collections.singleton(mockCrsCodes.get(3))).build());

    return list;
  }


  private List<IndicatorResponse> createListIndicatorResponse(List<Indicator> indicators) {
    List<IndicatorResponse> list = new ArrayList<>();
    if(Optional.ofNullable(indicators).isPresent() && !indicators.isEmpty()) {
      int i = 0;
      for (Indicator indicator : indicators) {
        list.add(IndicatorResponse.builder()
                .name(indicator.getName())
                .id(indicator.getId())
                .color(indicator.getLevel().getColor())
                .description(indicator.getDescription())
                .level(indicator.getLevel().getName())
                .sdgCode(indicator.getSdgCode())
                .crsCode(indicator.getCrsCode())
                .source(indicator.getSource())
                .themes(indicator.getThemes())
                .disaggregation(indicator.getDisaggregation())
                .date(String.valueOf(2000+i))
                .value(String.valueOf(50+i))
                .build());
        i++;
      }
    } else {
      for (int i = 1; i < 6; i++) {
        list.add(IndicatorResponse.builder().id(i).level("IMPACT").color("color").name("Label " + i)
                .description("Description").build());
      }
    }
    return list;
  }

  @Test
  void getIndicators() {
    List<Indicator> expectedResult = mockIndicatorList().stream()
        .filter(
            x -> mockThemes.contains(x.getThemes()) && mockLevelsId.contains(x.getLevel().getId())
                && mockSources.containsAll(x.getSource())
                && mockSdgCodes.containsAll(x.getSdgCode()) && mockCrsCodes.containsAll(x.getCrsCode()))
        .collect(Collectors.toList());

    List<Indicator> result = indicatorService.getIndicators(Optional.of(mockThemes),
        Optional.of(mockSources.stream().map(Source::getId).collect(Collectors.toList())),
        Optional.of(mockLevelsId), Optional.of(mockSdgCodes.stream().map(SDGCode::getId).collect(Collectors.toList())),
        Optional.of(mockCrsCodes.stream().map(CRSCode::getId).collect(Collectors.toList())));
    verify(indicatorRepository).findAll(any(Specification.class));
    verify(indicatorRepository, times(0)).findAll();
    assertEquals(expectedResult, result);
  }

  @Test
  void getIndicators_someFilters() {
    when(indicatorRepository.findAll(any(Specification.class))).
        thenReturn(mockIndicatorList().stream()
            .filter(x -> mockThemes.contains(x.getThemes()) && mockLevelsId
                .contains(x.getLevel().getId()) && mockSources.containsAll(x.getSource())
            ).collect(Collectors.toList()));

    List<Indicator> expectedResult = mockIndicatorList().stream()
        .filter(
            x -> mockThemes.contains(x.getThemes()) && mockLevelsId.contains(x.getLevel().getId())
                && mockSources.containsAll(x.getSource())
        ).collect(Collectors.toList());

    List<Indicator> result = indicatorService.getIndicators(Optional.of(mockThemes),
        Optional.of(mockSources.stream().map(Source::getId).collect(Collectors.toList())), Optional.of(mockLevelsId), Optional.empty(), Optional.empty());
    verify(indicatorRepository).findAll(any(Specification.class));
    verify(indicatorRepository, times(0)).findAll();
    assertEquals(expectedResult, result);
  }

  @Test
  void getIndicators_noFilter() {
    List<Indicator> expectedResult = mockIndicatorList();
    List<Indicator> result = indicatorService
        .getIndicators(Optional.empty(), Optional.empty(), Optional.empty(),
            Optional.empty(), Optional.empty());
    verify(indicatorRepository, times(0)).findAll(any(Specification.class));
    verify(indicatorRepository).findAll();
    assertEquals(expectedResult, result);
  }

  Integer validateWordTemplateLevel(XWPFTable table, List<Indicator> indicators, List<IndicatorResponse> indicatorResponses, Integer rowIndex){
    if(indicators.size() > 0){
      // if its impact
      String assumptionsValue = indicators.get(0).getLevel().equals(mockLevels[3]) ? "\tNot applicable" : "";
      for (int i = 0; i < indicators.size(); i++) {
        XWPFTableRow row = table.getRow(i+rowIndex);
        assertEquals(8, row.getTableCells().size());
        assertEquals("", row.getCell(1).getTextRecursively());
        assertEquals(indicators.get(i).getName(), row.getCell(2).getTextRecursively());
        int finalI = i;
        indicatorResponses.stream().filter(x->x.getLevel().equals(mockLevels[3].getName()) && x.getId() == indicators.get(finalI).getId()).findFirst().ifPresent(response-> {
          String baselineValue = (isNull(response.getDate()) || isNull(response.getValue())) ? "" : response.getValue() + " (" +response.getDate() + ")";
          assertEquals(baselineValue, row.getCell(3).getTextRecursively());
        });
        assertEquals("", row.getCell(4).getTextRecursively());
        assertEquals("", row.getCell(5).getTextRecursively());
        assertEquals(Optional.ofNullable(indicators.get(i).getSourceVerification()).orElse(""), row.getCell(6).getTextRecursively());
        assertEquals(assumptionsValue,row.getCell(7).getTextRecursively());

        // validate merge cells
        assertTrue(row.getCell(0).getCTTc().getTcPr().isSetVMerge());
        assertTrue(row.getCell(1).getCTTc().getTcPr().isSetVMerge());
        assertTrue(row.getCell(7).getCTTc().getTcPr().isSetVMerge());
      }

      return rowIndex + indicators.size();
    }else {
      assertEquals(8, table.getRow(rowIndex).getTableCells().size());
      // First column has the level and last column is not empty for impact indicators
      for (int i = 1; i < table.getRow(rowIndex).getTableCells().size() - 1; i++) {
        assertEquals("", table.getRow(rowIndex).getCell(i).getTextRecursively());
      }
      return ++rowIndex;
    }
  }
}
