package io.metersphere.jmeter;

import com.alibaba.fastjson.JSON;
import io.metersphere.constants.BackendListenerConstants;
import io.metersphere.constants.HttpMethodConstants;
import io.metersphere.dto.JmeterRunRequestDTO;
import io.metersphere.dto.RequestResult;
import io.metersphere.dto.ResponseAssertionResult;
import io.metersphere.dto.ResponseResult;
import io.metersphere.utils.JMeterVars;
import io.metersphere.utils.LoggerUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jmeter.visualizers.backend.BackendListener;
import org.apache.jorphan.collections.HashTree;
import org.apache.jmeter.config.Arguments;

import java.lang.reflect.Field;
import java.util.Map;

public class JMeterBase {

    private final static String THREAD_SPLIT = " ";

    public static HashTree getHashTree(Object scriptWrapper) throws Exception {
        Field field = scriptWrapper.getClass().getDeclaredField("testPlan");
        field.setAccessible(true);
        return (HashTree) field.get(scriptWrapper);
    }

    public static void addBackendListener(JmeterRunRequestDTO request, HashTree hashTree) {
        LoggerUtil.debug("开始为报告【 " + request.getReportId() + "】，资源【" + request.getTestId() + "】添加BackendListener");

        BackendListener backendListener = new BackendListener();
        backendListener.setName(request.getReportId() + "_" + request.getTestId());
        Arguments arguments = new Arguments();
        arguments.addArgument(BackendListenerConstants.TEST_ID.name(), request.getTestId());
        arguments.addArgument(BackendListenerConstants.REPORT_ID.name(), request.getReportId());
        arguments.addArgument(BackendListenerConstants.RUN_MODE.name(), request.getRunMode());
        arguments.addArgument(BackendListenerConstants.REPORT_TYPE.name(), request.getReportType());
        arguments.addArgument(BackendListenerConstants.MS_TEST_PLAN_REPORT_ID.name(), request.getTestPlanReportId());
        if (request.getKafkaConfig() != null && request.getKafkaConfig().size() > 0) {
            arguments.addArgument(BackendListenerConstants.KAFKA_CONFIG.name(), JSON.toJSONString(request.getKafkaConfig()));
        }
        backendListener.setArguments(arguments);
        backendListener.setClassname(APIBackendListenerClient.class.getCanonicalName());
        if (hashTree != null) {
            hashTree.add(hashTree.getArray()[0], backendListener);
        }
        LoggerUtil.debug("开始为报告【 " + request.getReportId() + "】，资源【" + request.getTestId() + "】添加BackendListener 结束");
    }

    public static void addSyncListener(JmeterRunRequestDTO request, HashTree hashTree, String listenerClazz) {
        LoggerUtil.debug("开始为报告【 " + request.getReportId() + "】，资源【" + request.getTestId() + "】添加同步结果监听");

        SynchronousResultCollector backendListener = new SynchronousResultCollector();
        backendListener.setName(request.getReportId() + "_" + request.getTestId());
        backendListener.setReportId(request.getReportId());
        backendListener.setTestId(request.getTestId());
        backendListener.setRunMode(request.getRunMode());
        backendListener.setReportType(request.getReportType());
        backendListener.setTestPlanReportId(request.getTestPlanReportId());
        backendListener.setListenerClazz(listenerClazz);
        backendListener.setQueueId(request.getQueueId());
        backendListener.setRunType(request.getRunType());
        backendListener.setExtendedParameters(request.getExtendedParameters());

        if (request.getKafkaConfig() != null && request.getKafkaConfig().size() > 0) {
            backendListener.setProducerProps(request.getKafkaConfig());
        }
        if (hashTree != null) {
            hashTree.add(hashTree.getArray()[0], backendListener);
        }
        LoggerUtil.debug("开始为报告【 " + request.getReportId() + "】，资源【" + request.getTestId() + "】添加同步结果监听结束");
    }

    public static RequestResult getRequestResult(SampleResult result) {
        LoggerUtil.debug("开始处理结果资源【" + result.getSampleLabel() + "】");

        String threadName = StringUtils.substringBeforeLast(result.getThreadName(), THREAD_SPLIT);
        RequestResult requestResult = new RequestResult();
        requestResult.setThreadName(threadName);
        requestResult.setId(result.getSamplerId());
        requestResult.setResourceId(result.getResourceId());
        requestResult.setName(result.getSampleLabel());
        requestResult.setUrl(result.getUrlAsString());
        requestResult.setMethod(getMethod(result));
        requestResult.setBody(result.getSamplerData());
        requestResult.setHeaders(result.getRequestHeaders());
        requestResult.setRequestSize(result.getSentBytes());
        requestResult.setStartTime(result.getStartTime());
        requestResult.setEndTime(result.getEndTime());
        requestResult.setTotalAssertions(result.getAssertionResults().length);
        requestResult.setSuccess(result.isSuccessful());
        requestResult.setError(result.getErrorCount());
        requestResult.setScenario(result.getScenario());
        if (result instanceof HTTPSampleResult) {
            HTTPSampleResult res = (HTTPSampleResult) result;
            requestResult.setCookies(res.getCookies());
        }

        for (SampleResult subResult : result.getSubResults()) {
            requestResult.getSubRequestResults().add(getRequestResult(subResult));
        }
        ResponseResult responseResult = requestResult.getResponseResult();
        // 超过20M的文件不入库
        long size = 1024 * 1024 * 20;
        if (StringUtils.equals(ContentType.APPLICATION_OCTET_STREAM.getMimeType(), result.getContentType())
                && result.getResponseDataAsString().length() > size) {
            requestResult.setBody("");
        } else {
            responseResult.setBody(result.getResponseDataAsString());
        }
        responseResult.setHeaders(result.getResponseHeaders());
        responseResult.setLatency(result.getLatency());
        responseResult.setResponseCode(result.getResponseCode());
        responseResult.setResponseSize(result.getResponseData().length);
        responseResult.setResponseTime(result.getTime());
        responseResult.setResponseMessage(result.getResponseMessage());
        JMeterVariables variables = JMeterVars.get(result.getResourceId());
        if (StringUtils.isNotEmpty(result.getExtVars())) {
            responseResult.setVars(result.getExtVars());
            JMeterVars.remove(result.getResourceId());
        } else if (variables != null && CollectionUtils.isNotEmpty(variables.entrySet())) {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                builder.append(entry.getKey()).append("：").append(entry.getValue()).append("\n");
            }
            if (StringUtils.isNotEmpty(builder)) {
                responseResult.setVars(builder.toString());
            }
            JMeterVars.remove(result.getResourceId());
        }

        for (AssertionResult assertionResult : result.getAssertionResults()) {
            ResponseAssertionResult responseAssertionResult = getResponseAssertionResult(assertionResult);
            if (responseAssertionResult.isPass()) {
                requestResult.addPassAssertions();
            }
            //xpath 提取错误会添加断言错误
            if (StringUtils.isBlank(responseAssertionResult.getMessage()) ||
                    (StringUtils.isNotBlank(responseAssertionResult.getName()) && !responseAssertionResult.getName().endsWith("XPath2Extractor"))
                    || (StringUtils.isNotBlank(responseAssertionResult.getContent()) && !responseAssertionResult.getContent().endsWith("XPath2Extractor"))
            ) {
                responseResult.getAssertions().add(responseAssertionResult);
            }
        }

        LoggerUtil.debug("处理结果资源【" + result.getSampleLabel() + "】结束");
        return requestResult;
    }

    private static ResponseAssertionResult getResponseAssertionResult(AssertionResult assertionResult) {
        ResponseAssertionResult responseAssertionResult = new ResponseAssertionResult();
        responseAssertionResult.setName(assertionResult.getName());
        if (StringUtils.isNotEmpty(assertionResult.getName()) && assertionResult.getName().indexOf("split==") != -1) {
            if (assertionResult.getName().indexOf("JSR223") != -1) {
                String[] array = assertionResult.getName().split("split==", 3);
                if (array.length > 2 && "JSR223".equals(array[0])) {
                    responseAssertionResult.setName(array[1]);
                    if (array[2].indexOf("split&&") != -1) {
                        String[] content = array[2].split("split&&");
                        responseAssertionResult.setContent(content[0]);
                        if (content.length > 1) {
                            responseAssertionResult.setScript(content[1]);
                        }
                    } else {
                        responseAssertionResult.setContent(array[2]);
                    }
                }
            } else {
                String[] array = assertionResult.getName().split("split==");
                responseAssertionResult.setName(array[0]);
                StringBuffer content = new StringBuffer();
                for (int i = 1; i < array.length; i++) {
                    content.append(array[i]);
                }
                responseAssertionResult.setContent(content.toString());
            }
        }
        responseAssertionResult.setPass(!assertionResult.isFailure() && !assertionResult.isError());
        if (!responseAssertionResult.isPass()) {
            responseAssertionResult.setMessage(assertionResult.getFailureMessage());
        }
        return responseAssertionResult;
    }

    private static String getMethod(SampleResult result) {
        String body = result.getSamplerData();
        String start = "RPC Protocol: ";
        String end = "://";
        if (StringUtils.contains(body, start)) {
            String protocol = StringUtils.substringBetween(body, start, end);
            if (StringUtils.isNotEmpty(protocol)) {
                return protocol.toUpperCase();
            }
            return "DUBBO";
        } else if (StringUtils.contains(result.getResponseHeaders(), "url:jdbc")) {
            return "SQL";
        } else {
            String method = StringUtils.substringBefore(body, " ");
            for (HttpMethodConstants value : HttpMethodConstants.values()) {
                if (StringUtils.equals(method, value.name())) {
                    return method;
                }
            }
            return "Request";
        }
    }
}
