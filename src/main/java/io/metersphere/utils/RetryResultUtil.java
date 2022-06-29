package io.metersphere.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.samplers.SampleResult;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 重试报告处理util
 */
public class RetryResultUtil {
    public final static String RETRY = "MsRetry_";
    public final static String RETRY_CN = "重试";
    public final static String RETRY_FIRST_CN = "首次";
    public final static String MS_CLEAR_LOOPS_VAR = "MS_CLEAR_LOOPS_VAR_";

    /**
     * 合并掉重试结果；保留最后十次重试结果
     *
     * @param results
     */
    public static void mergeRetryResults(List<SampleResult> results) {
        if (CollectionUtils.isNotEmpty(results)) {
            Map<String, List<SampleResult>> resultMap = results.stream().collect(Collectors.groupingBy(SampleResult::getResourceId));
            List<SampleResult> list = new LinkedList<>();
            resultMap.forEach((k, v) -> {
                if (CollectionUtils.isNotEmpty(v)) {
                    // 校验是否含重试结果
                    List<SampleResult> isRetryResults = v
                            .stream()
                            .filter(c -> StringUtils.isNotEmpty(c.getSampleLabel()) && c.getSampleLabel().startsWith(RETRY))
                            .collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(isRetryResults)) {
                        // 取最后执行的10 条
                        if (v.size() > 10) {
                            Collections.sort(v, Comparator.comparing(SampleResult::getResourceId));
                            SampleResult sampleResult = v.get(0);
                            List<SampleResult> topTens = v.subList(v.size() - 10, v.size());
                            topTens.set(0, sampleResult);
                            assembleName(topTens);
                            list.addAll(topTens);
                        } else {
                            assembleName(v);
                            list.addAll(v);
                        }
                    }
                }
            });
            results.clear();
            results.addAll(list);
        }
    }

    private static void assembleName(List<SampleResult> list) {
        // 名称排序处理
        for (int i = 0; i < list.size(); i++) {
            list.get(i).setSampleLabel(list.get(i).getSampleLabel().replace(RETRY, RETRY_CN + i + "_"));
            if (list.get(i).getSampleLabel().endsWith("_")) {
                list.get(i).setSampleLabel(list.get(i).getSampleLabel().substring(0, list.get(i).getSampleLabel().length() - 1));
            }
            if (i == 0) {
                list.get(i).setSampleLabel(StringUtils.isNotEmpty(list.get(i).getSampleLabel())
                        ? RETRY_FIRST_CN + "_" + list.get(i).getSampleLabel() : RETRY_FIRST_CN);
            }
        }
    }

    public static List<SampleResult> clearLoops(List<SampleResult> results) {
        if (CollectionUtils.isNotEmpty(results)) {
            return results.stream().filter(sampleResult ->
                    StringUtils.isNotEmpty(sampleResult.getSampleLabel())
                            && !sampleResult.getSampleLabel().startsWith(MS_CLEAR_LOOPS_VAR))
                    .collect(Collectors.toList());
        }
        return results;
    }
}
