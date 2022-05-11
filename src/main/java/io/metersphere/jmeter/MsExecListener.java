package io.metersphere.jmeter;

import io.metersphere.dto.ResultDTO;
import org.apache.jmeter.samplers.SampleResult;

import java.util.List;
import java.util.Map;

public interface MsExecListener {

    public void setupTest();

    public void handleTeardownTest(List<SampleResult> sampleResults, ResultDTO dto, Map<String, Object> kafkaConfig);

    public void testEnded(ResultDTO dto, Map<String, Object> kafkaConfig);
}
