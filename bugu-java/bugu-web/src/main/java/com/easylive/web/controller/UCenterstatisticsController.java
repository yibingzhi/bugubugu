package com.easylive.web.controller;

import com.easylive.entity.dto.TokenUserInfoDto;
import com.easylive.entity.po.StatisticsInfo;
import com.easylive.entity.query.StatisticsInfoQuery;
import com.easylive.entity.vo.ResponseVO;
import com.easylive.service.StatisticsInfoService;
import com.easylive.utils.DateUtil;
import com.easylive.web.annotation.GlobalInterceptor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@Validated
@RequestMapping("/ucenter")
public class UCenterstatisticsController extends ABaseController {

    @Resource
    private StatisticsInfoService statisticsInfoService;

    /**
     * @Description: 获取实际时间统计信息
     * @return: com.easylive.entity.vo.ResponseVO - 返回包含前一天统计数据和总统计信息的响应对象
     */
    @RequestMapping("/getActualTimeStatisticsInfo")
    @GlobalInterceptor // 使用全局拦截器
    public ResponseVO getActualTimeStatisticsInfo() {
        // 获取前一天的日期
        String preDate = DateUtil.getBeforeDayDate(1);
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取当前用户信息

        // 构建统计信息查询参数
        StatisticsInfoQuery param = new StatisticsInfoQuery();
        param.setStatisticsDate(preDate); // 设置统计日期为前一天
        param.setUserId(tokenUserInfoDto.getUserId()); // 设置用户ID

        // 查询前一天的统计数据
        List<StatisticsInfo> preDayData = statisticsInfoService.findListByParam(param);
        // 将前一天的数据转换为Map，键为数据类型，值为统计计数
        Map<Integer, Integer> preDayDataMap = preDayData.stream()
                .collect(Collectors.toMap(StatisticsInfo::getDataType, StatisticsInfo::getStatisticsCount, (item1, item2) -> item2));

        // 获取当前用户的总统计信息
        Map<String, Integer> totalCountInfo = statisticsInfoService.getStatisticsInfoActualTime(tokenUserInfoDto.getUserId());

        // 构建结果对象
        Map<String, Object> result = new HashMap<>();
        result.put("preDayData", preDayDataMap); // 存储前一天的数据
        result.put("totalCountInfo", totalCountInfo); // 存储总统计信息

        return getSuccessResponseVO(result); // 返回成功响应
    }

    /**
     * @Description: 获取过去一周的统计信息
     * @param: [dataType] - 数据类型，用于过滤统计信息
     * @return: com.easylive.entity.vo.ResponseVO - 返回包含过去一周统计信息的响应对象
     */
    @RequestMapping("/getWeekStatisticsInfo")
    @GlobalInterceptor // 使用全局拦截器
    public ResponseVO getWeekStatisticsInfo(Integer dataType) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(); // 获取当前用户信息
        List<String> dateList = DateUtil.getBeforeDates(7); // 获取过去7天的日期列表

        // 构建统计信息查询参数
        StatisticsInfoQuery param = new StatisticsInfoQuery();
        param.setDataType(dataType); // 设置数据类型
        param.setUserId(tokenUserInfoDto.getUserId()); // 设置用户ID
        param.setStatisticsDateStart(dateList.get(0)); // 设置统计开始日期
        param.setStatisticsDateEnd(dateList.get(dateList.size() - 1)); // 设置统计结束日期
        param.setOrderBy("statistics_date asc"); // 设置排序方式为日期升序

        // 查询统计信息列表
        List<StatisticsInfo> statisticsInfoList = statisticsInfoService.findListByParam(param);
        // 将查询结果转换为Map，键为统计日期，值为StatisticsInfo对象
        Map<String, StatisticsInfo> dataMap = statisticsInfoList.stream()
                .collect(Collectors.toMap(item -> item.getStatisticsDate(), Function.identity(), (data1, data2) -> data2));

        List<StatisticsInfo> resultDataList = new ArrayList<>();
        for (String date : dateList) {
            StatisticsInfo dataItem = dataMap.get(date); // 获取对应日期的统计信息
            if (dataItem == null) {
                // 如果没有数据，则创建一个新的StatisticsInfo对象并设置统计计数为0
                dataItem = new StatisticsInfo();
                dataItem.setStatisticsCount(0);
                dataItem.setStatisticsDate(date); // 设置日期
            }
            resultDataList.add(dataItem); // 添加到结果列表
        }
        return getSuccessResponseVO(resultDataList); // 返回成功响应
    }


}
