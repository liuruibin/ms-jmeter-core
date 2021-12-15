package io.metersphere.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RunModeConfigDTO {
    private String mode;
    private String reportType;
    private String reportName;
    private String reportId;
    private String testId;
    private String amassReport;
    private boolean onSampleError;
    private String resourcePoolId;
    private BaseSystemConfigDTO baseInfo;
    private List<JvmInfoDTO> testResources;
    /**
     * 运行环境
     */
    private Map<String, String> envMap;
    private String environmentType;
    private String environmentGroupId;
}
