package io.metersphere.jmeter;

import io.metersphere.dto.ResultDTO;
import io.metersphere.utils.LoggerUtil;

import java.util.Map;

public abstract class MsExecListener {

    public void handleTeardownTest(ResultDTO dto, Map<String, Object> kafkaConfig) {
        LoggerUtil.info("进入默认方法，处理单条执行结果报告【" + dto.getReportId() + " 】,资源【 " + dto.getTestId() + " 】");
    }

    public void testEnded(ResultDTO dto, Map<String, Object> kafkaConfig) {
        LoggerUtil.info("进入默认方法，线程组执行结束处理报告【" + dto.getReportId() + " 】,资源【 " + dto.getTestId() + " 】");
    }
}
