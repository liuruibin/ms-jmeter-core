package io.metersphere.jmeter;

import com.alibaba.fastjson.JSON;
import io.metersphere.cache.JMeterEngineCache;
import io.metersphere.constants.BackendListenerConstants;
import io.metersphere.dto.RequestResult;
import io.metersphere.dto.ResultDTO;
import io.metersphere.utils.LoggerUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;

import java.io.Serializable;
import java.util.*;

/**
 * JMeter BackendListener扩展, jmx脚本中使用
 */
public class APIBackendListenerClient extends AbstractBackendListenerClient implements Serializable {
    public static final String RUNNING_DEBUG_SAMPLER_NAME = "RunningDebugSampler";

    public String runMode = BackendListenerConstants.RUN.name();

    private final List<SampleResult> queue = new ArrayList<>();
    // 测试ID
    private String testId;

    // 报告类型：independence= 独立报告/integrated=集成报告
    private String reportType;

    private String reportId;

    private String testPlanReportId;

    // KAFKA 配置信息
    private Map<String, Object> producerProps;

    @Override
    public void setupTest(BackendListenerContext context) throws Exception {
        setParam(context);
        super.setupTest(context);
    }

    @Override
    public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context) {
        queue.addAll(sampleResults);
    }

    @Override
    public void teardownTest(BackendListenerContext context) throws Exception {
        LoggerUtil.info("接收到报告：【" + this.reportId + "】,资源【" + this.testId + "】的执行结果 " + queue.size());
        ResultDTO dto = new ResultDTO();
        dto.setTestId(this.testId);
        dto.setRunMode(this.runMode);
        dto.setReportId(this.reportId);
        dto.setReportType(this.reportType);
        dto.setTestPlanReportId(this.testPlanReportId);
        try {
            List<RequestResult> requestResults = new LinkedList<>();
            List<String> environmentList = new ArrayList<>();
            queue.forEach(result -> {
                if (StringUtils.equals(result.getSampleLabel(), RUNNING_DEBUG_SAMPLER_NAME)) {
                    String evnStr = result.getResponseDataAsString();
                    environmentList.add(evnStr);
                } else {
                    requestResults.add(JMeterBase.getRequestResult(result));
                }
            });
            dto.setRequestResults(requestResults);
            dto.setArbitraryData(new HashMap<String, Object>() {{
                this.put("ENV", environmentList);
            }});

            JMeterEngineCache.runningEngine.remove(this.reportId);

            LoggerUtil.info("清理执行队列，剩余容量：" + JMeterEngineCache.runningEngine.size());

            Class<?> clazz = Class.forName("io.metersphere.api.jmeter.APIBackendListenerHandler");
            Object instance = clazz.newInstance();
            clazz.getDeclaredMethod("handleTeardownTest", ResultDTO.class, Map.class).invoke(instance, dto, producerProps);
        } catch (Exception e) {
            LoggerUtil.error("handleTeardownTest 调用失败", e.getMessage());
        }
        super.teardownTest(context);
    }

    private void setParam(BackendListenerContext context) {
        this.testId = context.getParameter(BackendListenerConstants.TEST_ID.name());
        this.runMode = context.getParameter(BackendListenerConstants.RUN_MODE.name());
        this.reportId = context.getParameter(BackendListenerConstants.REPORT_ID.name());
        this.reportType = context.getParameter(BackendListenerConstants.REPORT_TYPE.name());
        this.testPlanReportId = context.getParameter(BackendListenerConstants.MS_TEST_PLAN_REPORT_ID.name());
        if (StringUtils.isNotEmpty(context.getParameter(BackendListenerConstants.KAFKA_CONFIG.name()))) {
            this.producerProps = JSON.parseObject(context.getParameter(BackendListenerConstants.KAFKA_CONFIG.name()), Map.class);
        }
        if (StringUtils.isBlank(this.runMode)) {
            this.runMode = BackendListenerConstants.RUN.name();
        }
    }
}
