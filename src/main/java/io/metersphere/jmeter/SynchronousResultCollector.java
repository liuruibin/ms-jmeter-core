/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.metersphere.jmeter;

import com.alibaba.fastjson.JSON;
import io.metersphere.constants.BackendListenerConstants;
import io.metersphere.dto.RequestResult;
import io.metersphere.dto.ResultDTO;
import io.metersphere.utils.ClassLoaderUtil;
import io.metersphere.utils.JMeterVars;
import io.metersphere.utils.ListenerUtil;
import io.metersphere.utils.LoggerUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.engine.util.NoThreadClone;
import org.apache.jmeter.reporters.AbstractListenerElement;
import org.apache.jmeter.samplers.*;
import org.apache.jmeter.testelement.TestStateListener;
import org.apache.jmeter.threads.JMeterVariables;

import java.io.Serializable;
import java.util.*;

/**
 * 实时结果监听
 */
public class SynchronousResultCollector extends AbstractListenerElement implements SampleListener, Clearable, Serializable,
        TestStateListener, Remoteable, NoThreadClone {

    @Override
    public Object clone() {
        SynchronousResultCollector clone = (SynchronousResultCollector) super.clone();
        return clone;
    }

    public static boolean isSampleWanted(boolean success, boolean errorOnly,
                                         boolean successOnly) {
        return (!errorOnly && !successOnly) ||
                (success && successOnly) ||
                (!success && errorOnly);
    }

    @Override
    public void testEnded(String host) {
        try {
            LoggerUtil.info("JMETER-测试报告【" + this.reportId + "】资源【 " + this.testId + " 】执行结束");
            ResultDTO dto = new ResultDTO();
            dto.setTestId(this.testId);
            dto.setRunMode(this.runMode);
            dto.setReportId(this.reportId);
            dto.setReportType(this.reportType);
            dto.setQueueId(this.queueId);
            dto.setRunType(this.runType);
            dto.setExtendedParameters(this.extendedParameters);
            dto.setTestPlanReportId(this.testPlanReportId);
            if (StringUtils.isEmpty(this.listenerClazz)) {
                listenerClazz = MsExecListener.class.getCanonicalName();
            }
            Class<?> clazz = ClassLoaderUtil.getClass(listenerClazz);
            Object instance = clazz.newInstance();
            clazz.getDeclaredMethod("testEnded", ResultDTO.class, Map.class).invoke(instance, dto, producerProps);
        } catch (Exception e) {
            LoggerUtil.error("JMETER-测试报告【 " + this.reportId + "】资源【 " + this.testId + " 】结果处理失败：" + e.getMessage());
        }
    }

    @Override
    public void testStarted(String host) {
        LoggerUtil.info("TestStarted接收到参数：报告【 " + this.reportId + " 】" +
                "TestStarted接收到参数：资源【 " + this.testId + " 】" +
                "TestStarted接收到参数：测试计划报告【 " + this.testPlanReportId + " 】" +
                "TestStarted接收到参数：执行对象【 " + this.runMode + " 】" +
                "TestStarted接收到参数：报告类型【 " + this.reportType + " 】");
    }

    @Override
    public void testEnded() {
        testEnded(ListenerUtil.TEST_IS_LOCAL);
    }

    @Override
    public void testStarted() {
        testStarted(ListenerUtil.TEST_IS_LOCAL);
    }

    @Override
    public void sampleStarted(SampleEvent e) {
    }

    @Override
    public void sampleStopped(SampleEvent e) {
    }

    public boolean isErrorLogging() {
        return getPropertyAsBoolean(ListenerUtil.ERROR_LOGGING);
    }

    public boolean isSuccessOnlyLogging() {
        return getPropertyAsBoolean(ListenerUtil.SUCCESS_ONLY_LOGGING, false);
    }

    public boolean isSampleWanted(boolean success) {
        boolean errorOnly = isErrorLogging();
        boolean successOnly = isSuccessOnlyLogging();
        return isSampleWanted(success, errorOnly, successOnly);
    }

    @Override
    public void sampleOccurred(SampleEvent event) {
        SampleResult result = event.getResult();
        ListenerUtil.setVars(result);
        if (isSampleWanted(result.isSuccessful())) {
            List<RequestResult> requestResults = new LinkedList<>();
            List<String> environmentList = new ArrayList<>();
            ResultDTO dto = new ResultDTO();
            dto.setTestId(this.testId);
            dto.setRunMode(this.runMode);
            dto.setReportId(this.reportId);
            dto.setReportType(this.reportType);
            dto.setTestPlanReportId(this.testPlanReportId);
            dto.setExtendedParameters(this.extendedParameters);
            dto.setQueueId(this.queueId);
            dto.setRunType(this.runType);
            RequestResult requestResult = JMeterBase.getRequestResult(result);
            if (StringUtils.equals(result.getSampleLabel(), ListenerUtil.RUNNING_DEBUG_SAMPLER_NAME)) {
                String evnStr = result.getResponseDataAsString();
                environmentList.add(evnStr);
            } else {
                boolean resultNotFilterOut = ListenerUtil.checkResultIsNotFilterOut(requestResult);
                if (resultNotFilterOut) {
                    requestResults.add(requestResult);
                }
            }

            if (LoggerUtil.getLogger().isDebugEnabled()) {
                LoggerUtil.debug("JMETER-获取到单条执行结果【 " + JSON.toJSONString(dto) + " 】");
            }
            if (StringUtils.isNotEmpty(requestResult.getName()) && requestResult.getName().startsWith("Transaction=")) {
                LoggerUtil.debug("JMETER-获取到RunningDebugSampler 内容 【 " + requestResult + " 】");
                dto.setRequestResults(requestResult.getSubRequestResults());
            } else {
                dto.setRequestResults(requestResults);
            }
            try {
                ListenerUtil.setEev(dto, environmentList);
                if (StringUtils.isEmpty(this.listenerClazz)) {
                    listenerClazz = MsExecListener.class.getCanonicalName();
                }
                Class<?> clazz = ClassLoaderUtil.getClass(listenerClazz);
                Object instance = clazz.newInstance();
                clazz.getDeclaredMethod("handleTeardownTest", ResultDTO.class, Map.class).invoke(instance, dto, producerProps);
            } catch (Exception e) {
                LoggerUtil.error("JMETER-调用存储方法失败：" + e.getMessage());
            }
        }
    }

    @Override
    public void clearData() {
    }

    private String runMode = BackendListenerConstants.RUN.name();

    private String listenerClazz;

    // 测试ID
    private String testId;

    // 报告类型：independence= 独立报告/integrated=集成报告
    private String reportType;

    private String reportId;

    private String queueId;

    private String runType;

    private String testPlanReportId;

    // KAFKA 配置信息
    private Map<String, Object> producerProps;

    private Map<String, Object> extendedParameters;

    public void setRunType(String runType) {
        this.runType = runType;
    }

    public void setQueueId(String queueId) {
        this.queueId = queueId;
    }

    public void setTestId(String testId) {
        this.testId = testId;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public void setTestPlanReportId(String testPlanReportId) {
        this.testPlanReportId = testPlanReportId;
    }

    public void setProducerProps(Map<String, Object> producerProps) {
        this.producerProps = producerProps;
    }

    public void setRunMode(String runMode) {
        this.runMode = runMode;
    }

    public void setListenerClazz(String listenerClazz) {
        this.listenerClazz = listenerClazz;
    }

    public void setExtendedParameters(Map<String, Object> extendedParameters) {
        this.extendedParameters = extendedParameters;
    }
}
