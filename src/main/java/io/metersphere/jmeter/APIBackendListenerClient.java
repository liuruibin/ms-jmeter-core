package io.metersphere.jmeter;

import com.alibaba.fastjson.JSON;
import io.metersphere.constants.BackendListenerConstants;
import io.metersphere.dto.RequestResult;
import io.metersphere.dto.ResultDTO;
import io.metersphere.utils.ClassLoaderUtil;
import io.metersphere.utils.ListenerUtil;
import io.metersphere.utils.LoggerUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.services.FileServer;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;

import java.io.Serializable;
import java.util.*;

/**
 * JMeter BackendListener扩展, jmx脚本中使用
 */
public class APIBackendListenerClient extends AbstractBackendListenerClient implements Serializable {
    private String runMode = BackendListenerConstants.RUN.name();
    private String listenerClazz;

    // KAFKA 配置信息
    private Map<String, Object> producerProps;
    private ResultDTO dto;

    @Override
    public void setupTest(BackendListenerContext context) throws Exception {
        this.setParam(context);
        LoggerUtil.info("TestStarted接收到参数：报告【" + JSON.toJSONString(dto) + " 】");
        LoggerUtil.info("TestStarted接收到参数：KAFKA【" + JSON.toJSONString(producerProps) + " 】");
        LoggerUtil.info("TestStarted接收到参数：处理类【" + listenerClazz + " 】");
        super.setupTest(context);
    }

    @Override
    public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context) {
        LoggerUtil.info("CORE接收到数据【" + sampleResults.size() + " 】, " + dto.getQueueId());
        try {
            List<RequestResult> requestResults = new LinkedList<>();
            List<String> environmentList = new ArrayList<>();
            sampleResults.forEach(result -> {
                ListenerUtil.setVars(result);
                RequestResult requestResult = JMeterBase.getRequestResult(result);
                if (StringUtils.equals(result.getSampleLabel(), ListenerUtil.RUNNING_DEBUG_SAMPLER_NAME)) {
                    String evnStr = result.getResponseDataAsString();
                    environmentList.add(evnStr);
                } else {
                    boolean resultNotFilterOut = ListenerUtil.checkResultIsNotFilterOut(requestResult);
                    if (resultNotFilterOut) {
                        if (StringUtils.isNotEmpty(requestResult.getName()) && requestResult.getName().startsWith("Transaction=")) {
                            requestResults.addAll(requestResult.getSubRequestResults());
                        } else {
                            requestResults.add(requestResult);
                        }
                    }
                }
            });
            dto.setRequestResults(requestResults);
            ListenerUtil.setEev(dto, environmentList);

            Class<?> clazz = ClassLoaderUtil.getClass(listenerClazz);
            Object instance = clazz.newInstance();
            clazz.getDeclaredMethod("handleTeardownTest", ResultDTO.class, Map.class).invoke(instance, dto, producerProps);
        } catch (Exception e) {
            LoggerUtil.error("JMETER-调用存储方法失败：" + e.getMessage());
        }
    }

    @Override
    public void teardownTest(BackendListenerContext context) throws Exception {
        try {
            if (FileServer.getFileServer() != null) {
                FileServer.getFileServer().closeCsv(dto.getReportId());
            }
            LoggerUtil.info("JMETER-测试报告【" + dto.getReportId() + "】资源【 " + dto.getTestId() + " 】执行结束");
            Class<?> clazz = ClassLoaderUtil.getClass(listenerClazz);
            Object instance = clazz.newInstance();
            clazz.getDeclaredMethod("testEnded", ResultDTO.class, Map.class).invoke(instance, dto, producerProps);
        } catch (Exception e) {
            LoggerUtil.error("JMETER-测试报告【" + dto.getReportId() + "】资源【 " + dto.getTestId() + " 】执行异常", e);
        }
        super.teardownTest(context);
    }

    private void setParam(BackendListenerContext context) {
        dto = new ResultDTO();
        dto.setTestId(context.getParameter(BackendListenerConstants.TEST_ID.name()));
        dto.setRunMode(context.getParameter(BackendListenerConstants.RUN_MODE.name()));
        dto.setReportId(context.getParameter(BackendListenerConstants.REPORT_ID.name()));
        dto.setReportType(context.getParameter(BackendListenerConstants.REPORT_TYPE.name()));
        dto.setTestPlanReportId(context.getParameter(BackendListenerConstants.MS_TEST_PLAN_REPORT_ID.name()));
        this.producerProps = new HashMap<>();
        if (StringUtils.isNotEmpty(context.getParameter(BackendListenerConstants.KAFKA_CONFIG.name()))) {
            this.producerProps = JSON.parseObject(context.getParameter(BackendListenerConstants.KAFKA_CONFIG.name()), Map.class);
        }
        this.listenerClazz = context.getParameter(BackendListenerConstants.CLASS_NAME.name());

        dto.setQueueId(context.getParameter(BackendListenerConstants.QUEUE_ID.name()));
        dto.setRunType(context.getParameter(BackendListenerConstants.RUN_TYPE.name()));

        String ept = context.getParameter(BackendListenerConstants.EPT.name());
        if (StringUtils.isNotEmpty(ept)) {
            dto.setExtendedParameters(JSON.parseObject(context.getParameter(BackendListenerConstants.EPT.name()), Map.class));
        }
    }
}
