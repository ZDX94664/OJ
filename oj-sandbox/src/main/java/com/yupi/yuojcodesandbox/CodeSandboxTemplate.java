package com.yupi.yuojcodesandbox;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.yupi.yuojcodesandbox.model.ExecuteCodeRequest;
import com.yupi.yuojcodesandbox.model.ExecuteCodeResponse;
import com.yupi.yuojcodesandbox.model.ExecuteMessage;
import com.yupi.yuojcodesandbox.model.JudgeInfo;
import com.yupi.yuojcodesandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 支持Java、C、Python的代码沙箱
 */
@Slf4j
public abstract class CodeSandboxTemplate implements CodeSandbox {

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final long TIME_OUT = 5000L;

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage().toLowerCase();

        File userCodeFile = null;
        try {
            // 1. 保存代码文件
            userCodeFile = saveCodeToFile(code, language);

            // 2. 编译代码
            ExecuteMessage compileMessage = compileFile(userCodeFile, language);
            if (compileMessage.getExitValue() != 0) {
                // 编译错误
                return getCompileErrorResponse(compileMessage);
            }

            // 3. 执行代码
            List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList, language);

            // 4. 整理输出
            return getOutputResponse(executeMessageList);
        } catch (Exception e) {
            return getErrorResponse(e);
        } finally {
            // 5. 清理文件
            if (userCodeFile != null) {
                boolean b = deleteFile(userCodeFile);
                if (!b) {
                    log.error("deleteFile error, userCodeFilePath = {}", userCodeFile.getAbsolutePath());
                }
            }
        }
    }

    private File saveCodeToFile(String code, String language) {
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;
        if (!FileUtil.exist(globalCodePathName)) {
            FileUtil.mkdir(globalCodePathName);
        }

        String fileName;
        switch (language) {
            case "java":
                fileName = "Main.java";
                break;
            case "c":
                fileName = "main.c";
                break;
            case "python":
                fileName = "main.py";
                break;
            default:
                throw new RuntimeException("Unsupported language: " + language);
        }

        String userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + fileName;
        return FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
    }

    private ExecuteMessage compileFile(File userCodeFile, String language) {
        if ("python".equals(language)) {
            ExecuteMessage msg = new ExecuteMessage();
            msg.setExitValue(0);
            msg.setMessage("Python不需要编译");
            return msg;
        }

        String compileCmd;
        String filePath = userCodeFile.getAbsolutePath();
        String parentPath = userCodeFile.getParentFile().getAbsolutePath();

        switch (language) {
            case "java":
                compileCmd = String.format("javac -encoding utf-8 %s", filePath);
                break;
            case "c":
                compileCmd = String.format("gcc %s -o %s/main", filePath, parentPath);
                break;
            default:
                throw new RuntimeException("Unsupported language: " + language);
        }

        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            return ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
        } catch (Exception e) {
            throw new RuntimeException("编译失败: " + e.getMessage());
        }
    }

    private List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList, String language) {
        String parentPath = userCodeFile.getParentFile().getAbsolutePath();
        List<ExecuteMessage> executeMessages = new ArrayList<>();

        for (String input : inputList) {
            String runCmd;
            switch (language) {
                case "java":
                    runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", parentPath, input);
                    break;
                case "c":
                    runCmd = String.format("%s/main %s", parentPath, input);
                    break;
                case "python":
                    runCmd = String.format("python %s/main.py %s", parentPath, input);
                    break;
                default:
                    throw new RuntimeException("Unsupported language: " + language);
            }

            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                // 超时控制
                new Thread(() -> {
                    try {
                        Thread.sleep(TIME_OUT);
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                executeMessages.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException("执行错误: " + e.getMessage());
            }
        }
        return executeMessages;
    }

    private ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList) {
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        List<String> outputs = new ArrayList<>();
        long maxTime = 0;

        for (ExecuteMessage msg : executeMessageList) {
            if (StrUtil.isNotBlank(msg.getErrorMessage())) {
                response.setStatus(3);
                response.setMessage(msg.getErrorMessage());
                break;
            }
            outputs.add(msg.getMessage());
            maxTime = Math.max(maxTime, msg.getTime() != null ? msg.getTime() : 0);
        }

        if (outputs.size() == executeMessageList.size()) {
            response.setStatus(1);
        }

        response.setOutputList(outputs);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        response.setJudgeInfo(judgeInfo);
        return response;
    }

    private boolean deleteFile(File userCodeFile) {
        if (userCodeFile.getParentFile() != null) {
            System.out.println("删除代码文件位置："+userCodeFile.getParentFile());
            return FileUtil.del(userCodeFile.getParentFile());
        }
        return false;
    }

    private ExecuteCodeResponse getErrorResponse(Throwable e) {
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        response.setStatus(2);
        response.setMessage(e.getMessage());
        response.setOutputList(new ArrayList<>());
        response.setJudgeInfo(new JudgeInfo());
        return response;
    }

    private ExecuteCodeResponse getCompileErrorResponse(ExecuteMessage compileMessage) {
        ExecuteCodeResponse response = new ExecuteCodeResponse();
        response.setStatus(3);
        response.setMessage(compileMessage.getErrorMessage());
        response.setOutputList(new ArrayList<>());
        response.setJudgeInfo(new JudgeInfo());
        return response;
    }
}
