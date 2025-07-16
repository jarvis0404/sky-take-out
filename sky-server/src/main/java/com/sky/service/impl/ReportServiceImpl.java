package com.sky.service.impl;


import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.LocaleEditor;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * hao: get turnover statistics
     * @param begin begin date
     * @param end end date
     * @return turnover report view object
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 1. get date list
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);

        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 2. get turnover list
        List<Double> turnoverList = new ArrayList<>();

        for (LocalDate date : dateList) {
            // get time range of curtain day
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            Map map  = new HashMap();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);
            turnover = turnover == null ? 0 : turnover;

            turnoverList.add(turnover);
        }


        // 3. return view object
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     *
     * @param begin begin date
     * @param end end date
     * @return user report view object
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 1. get date list
        List<LocalDate> dateList = new ArrayList<>();

        dateList.add(begin);
        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 2. get total user list and new user list
        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            Map map  = new HashMap();

            // 2.1 get total user list
            map.put("end", LocalDateTime.of(date, LocalTime.MAX));

            Integer cnt = userMapper.countByMap(map);
            totalUserList.add(cnt);

            // 2.2 get new user list
            map.put("begin", LocalDateTime.of(date, LocalTime.MIN));

            cnt = userMapper.countByMap(map);
            newUserList.add(cnt);
        }

        // return view object
        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 1. get date list
        List<LocalDate> dateList = new ArrayList<>();

        // log.info("order report service");

        dateList.add(begin);

        while (!begin.equals(end)) {
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        // 2. get daily order count list and valid order count list
        List<Integer> validOrderCountList = new ArrayList<>();
        List<Integer> orderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            orderCountList.add(getOrderCount(beginTime, endTime, null));
            validOrderCountList.add(getOrderCount(beginTime, endTime, Orders.COMPLETED));
        }

        // 3. get periodic order count & valid order count & completion rate
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        Double orderCompletionRate = totalOrderCount == 0 ? 0.0 : validOrderCount / totalOrderCount;

        // 4. return view object
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCount(validOrderCount)
                .totalOrderCount(totalOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    @Override
    public SalesTop10ReportVO getTop10Sales(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> top10Sales = orderMapper.getTop10Sales(beginTime, endTime);

        List<String> nameList = top10Sales.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numberList = top10Sales.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }

    @Override
    public void exportBusinessData(HttpServletResponse response) {
        // 1. prepare data to fill in
        LocalDate beginDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now().minusDays(1);
        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(beginDate, LocalTime.MIN), LocalDateTime.of(endDate, LocalTime.MAX));

        // 2. write data to Excel file through Apache POI
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            // get Excel workbook object
            XSSFWorkbook wb = new XSSFWorkbook(in);

            // get sheet object
            XSSFSheet sheet = wb.getSheetAt(0);

            // write data
            sheet.getRow(1).getCell(1).setCellValue("time: " + beginDate + "~" + endDate);

            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessData.getTurnover());
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());
            row.getCell(6).setCellValue(businessData.getNewUsers());

            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());
            row.getCell(4).setCellValue(businessData.getUnitPrice());

            for (int i = 0; i < 30; i++) {
                LocalDate date = beginDate.plusDays(i);

                BusinessDataVO dailyBusinessData = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(dailyBusinessData.getTurnover());
                row.getCell(3).setCellValue(dailyBusinessData.getValidOrderCount());
                row.getCell(4).setCellValue(dailyBusinessData.getOrderCompletionRate());
                row.getCell(5).setCellValue(dailyBusinessData.getUnitPrice());
                row.getCell(6).setCellValue(dailyBusinessData.getNewUsers());
            }

            // 3. write to http response output stream
            ServletOutputStream out = response.getOutputStream();
            wb.write(out);

            // close
            out.close();
            wb.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private Integer getOrderCount(LocalDateTime begin, LocalDateTime end, Integer status) {
        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end);
        map.put("status", status);

        return orderMapper.countByMap(map);
    }

}
