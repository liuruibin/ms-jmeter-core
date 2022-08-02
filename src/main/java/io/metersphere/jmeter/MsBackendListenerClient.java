package io.metersphere.jmeter;

import com.alibaba.fastjson.JSON;
import io.metersphere.cache.JMeterEngineCache;
import io.metersphere.constants.BackendListenerConstants;
import io.metersphere.dto.ResultDTO;
import io.metersphere.utils.LoggerUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.services.FileServer;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JMeter BackendListener扩展, jmx脚本中使用
 */
public class MsBackendListenerClient extends AbstractBackendListenerClient implements Serializable {
    // KAFKA 配置信息
    private Map<String, Object> producerProps;
    private ResultDTO dto;

    private MsExecListener execListener;

    @Override
    public void setupTest(BackendListenerContext context) throws Exception {
        this.setParam(context);
        LoggerUtil.info("TestStarted接收到参数：报告【" + JSON.toJSONString(dto) + " 】");
        super.setupTest(context);
    }

    @Override
    public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context) {
        LoggerUtil.info("接收到JMETER执行数据【" + sampleResults.size() + " 】", dto.getReportId());
        if (execListener != null) {
            execListener.handleTeardownTest(sampleResults, dto, producerProps);
        } else {
            LoggerUtil.info("找不到结果监听对象【" + dto.getReportId() + "】");
        }
    }

    @Override
    public void teardownTest(BackendListenerContext context) {
        try {
            super.teardownTest(context);
            if (FileServer.getFileServer() != null) {
                LoggerUtil.info("进入监听，开始关闭CSV", dto.getReportId());
                FileServer.getFileServer().closeCsv(dto.getReportId());
            }
            LoggerUtil.info("进入监听，开始调用存储方法", dto.getReportId());
            execListener.testEnded(dto, producerProps);
            LoggerUtil.info("JMETER-测试报告执行结束", dto.getReportId());
        } catch (Exception e) {
            LoggerUtil.error("JMETER执行机执行异常", dto.getReportId(), e);
        } finally {
            if (JMeterEngineCache.runningEngine.containsKey(dto.getReportId())) {
                JMeterEngineCache.runningEngine.remove(dto.getReportId());
            }
        }
    }

    /**
     * 初始化参数
     *
     * @param context
     */
    private void setParam(BackendListenerContext context) {
        dto = new ResultDTO();
        dto.setTestId(context.getParameter(BackendListenerConstants.TEST_ID.name()));
        dto.setRunMode(context.getParameter(BackendListenerConstants.RUN_MODE.name()));
        dto.setReportId(context.getParameter(BackendListenerConstants.REPORT_ID.name()));
        dto.setReportType(context.getParameter(BackendListenerConstants.REPORT_TYPE.name()));
        dto.setTestPlanReportId(context.getParameter(BackendListenerConstants.MS_TEST_PLAN_REPORT_ID.name()));
        if (context.getParameter(BackendListenerConstants.RETRY_ENABLE.name()) != null) {
            dto.setRetryEnable(Boolean.parseBoolean(context.getParameter(BackendListenerConstants.RETRY_ENABLE.name())));
        }
        this.producerProps = new HashMap<>();

        if (StringUtils.isNotEmpty(context.getParameter(BackendListenerConstants.KAFKA_CONFIG.name()))) {
            this.producerProps = JSON.parseObject(context.getParameter(BackendListenerConstants.KAFKA_CONFIG.name()), Map.class);
        }
        dto.setQueueId(context.getParameter(BackendListenerConstants.QUEUE_ID.name()));
        dto.setRunType(context.getParameter(BackendListenerConstants.RUN_TYPE.name()));

        String ept = context.getParameter(BackendListenerConstants.EPT.name());
        if (StringUtils.isNotEmpty(ept)) {
            dto.setExtendedParameters(JSON.parseObject(context.getParameter(BackendListenerConstants.EPT.name()), Map.class));
        }
        try {
            String listenerClazz = context.getParameter(BackendListenerConstants.CLASS_NAME.name());
            execListener = Class.forName(listenerClazz, true,
                    Thread.currentThread().getContextClassLoader())
                    .asSubclass(MsExecListener.class)
                    .getDeclaredConstructor().newInstance();
            // 初始化
            execListener.setupTest();
        } catch (Exception e) {
            LoggerUtil.error("初始化结果处理类失败", dto.getReportId(), e);
        }
    }
}
