package com.oj.ojbackendquestionservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.oj.ojbackendcommon.common.ErrorCode;
import com.oj.ojbackendcommon.constant.CommonConstant;
import com.oj.ojbackendcommon.exception.BusinessException;
import com.oj.ojbackendcommon.utils.SqlUtils;
import com.oj.ojbackendmodel.model.dto.questionsubmit.QuestionSubmitAddRequest;
import com.oj.ojbackendmodel.model.dto.questionsubmit.QuestionSubmitQueryRequest;
import com.oj.ojbackendmodel.model.entity.Question;
import com.oj.ojbackendmodel.model.entity.QuestionSubmit;
import com.oj.ojbackendmodel.model.entity.User;
import com.oj.ojbackendmodel.model.enums.QuestionSubmitLanguageEnum;
import com.oj.ojbackendmodel.model.enums.QuestionSubmitStatusEnum;
import com.oj.ojbackendmodel.model.vo.QuestionSubmitVO;
import com.oj.ojbackendquestionservice.mapper.QuestionSubmitMapper;
import com.oj.ojbackendquestionservice.rabbitmq.MyMessageProducer;
import com.oj.ojbackendquestionservice.service.QuestionService;
import com.oj.ojbackendquestionservice.service.QuestionSubmitService;
import com.oj.ojbackendserviceclient.service.JudgeFeignClient;
import com.oj.ojbackendserviceclient.service.UserFeignClient;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* @author 李鱼皮
* @description 针对表【question_submit(题目提交)】的数据库操作Service实现
* @createDate 2023-08-07 20:58:53
*/
@Service
public class QuestionSubmitServiceImpl extends ServiceImpl<QuestionSubmitMapper, QuestionSubmit>
    implements QuestionSubmitService {
    
    @Resource
    private QuestionService questionService;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    @Lazy
    private JudgeFeignClient judgeFeignClient;

    @Resource
    private MyMessageProducer myMessageProducer;

    /**
     * 提交题目
     *
     * @param questionSubmitAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public long doQuestionSubmit(QuestionSubmitAddRequest questionSubmitAddRequest, User loginUser) {
        // 校验编程语言是否合法
        String language = questionSubmitAddRequest.getLanguage();
        QuestionSubmitLanguageEnum languageEnum = QuestionSubmitLanguageEnum.getEnumByValue(language);
        if (languageEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "编程语言错误");
        }
        long questionId = questionSubmitAddRequest.getQuestionId();
        // 判断实体是否存在，根据类别获取实体
        Question question = questionService.getById(questionId);
        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 是否已提交题目
        long userId = loginUser.getId();
        // 每个用户串行提交题目
        QuestionSubmit questionSubmit = new QuestionSubmit();
        questionSubmit.setUserId(userId);
        questionSubmit.setQuestionId(questionId);
        questionSubmit.setCode(questionSubmitAddRequest.getCode());
        questionSubmit.setLanguage(language);
        // 设置初始状态
        questionSubmit.setStatus(QuestionSubmitStatusEnum.WAITING.getValue());
        questionSubmit.setJudgeInfo("{}");
        boolean save = this.save(questionSubmit);
        if (!save){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "数据插入失败");
        }
        Long questionSubmitId = questionSubmit.getId();
        // 发送消息
        myMessageProducer.sendMessage("code_exchange", "my_routingKey", String.valueOf(questionSubmitId));
        // 执行判题服务
//        CompletableFuture.runAsync(() -> {
//            judgeFeignClient.doJudge(questionSubmitId);
//        });
        return questionSubmitId;
    }


    /**
     * 获取查询包装类（用户根据哪些字段查询，根据前端传来的请求对象，得到 mybatis 框架支持的查询 QueryWrapper 类）
     *
     * @param questionSubmitQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<QuestionSubmit> getQueryWrapper(QuestionSubmitQueryRequest questionSubmitQueryRequest) {
        QueryWrapper<QuestionSubmit> queryWrapper = new QueryWrapper<>();
        if (questionSubmitQueryRequest == null) {
            return queryWrapper;
        }
        String language = questionSubmitQueryRequest.getLanguage();
        Integer status = questionSubmitQueryRequest.getStatus();
        Long questionId = questionSubmitQueryRequest.getQuestionId();
        Long userId = questionSubmitQueryRequest.getUserId();
        String sortField = questionSubmitQueryRequest.getSortField();
        String sortOrder = questionSubmitQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq(StringUtils.isNotBlank(language), "language", language);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(questionId), "questionId", questionId);
        queryWrapper.eq(QuestionSubmitStatusEnum.getEnumByValue(status) != null, "status", status);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public QuestionSubmitVO getQuestionSubmitVO(QuestionSubmit questionSubmit, User loginUser) {
        QuestionSubmitVO questionSubmitVO = QuestionSubmitVO.objToVo(questionSubmit);
        // 脱敏：仅本人和管理员能看见自己（提交 userId 和登录用户 id 不同）提交的代码
        long userId = loginUser.getId();
        // 处理脱敏
        if (userId != questionSubmit.getUserId() && !userFeignClient.isAdmin(loginUser)) {
            questionSubmitVO.setCode(null);
        }
        return questionSubmitVO;
    }

    @Override
    public Page<QuestionSubmitVO> getQuestionSubmitVOPage(Page<QuestionSubmit> questionSubmitPage, User loginUser) {
        List<QuestionSubmit> questionSubmitList = questionSubmitPage.getRecords();
        Page<QuestionSubmitVO> questionSubmitVOPage = new Page<>(questionSubmitPage.getCurrent(), questionSubmitPage.getSize(), questionSubmitPage.getTotal());
        if (CollectionUtils.isEmpty(questionSubmitList)) {
            return questionSubmitVOPage;
        }
        List<QuestionSubmitVO> questionSubmitVOList = questionSubmitList.stream()
                .map(questionSubmit -> getQuestionSubmitVO(questionSubmit, loginUser))
                .collect(Collectors.toList());
        questionSubmitVOPage.setRecords(questionSubmitVOList);
        return questionSubmitVOPage;
    }
    @Override
    public Map<String, Object> getUserStatsUsingGet(Long userId) {
        Map<String, Object> stats = new HashMap<>();

        // 1. Total Questions
        QueryWrapper<Question> totalQuestionsQuery = new QueryWrapper<>();
        totalQuestionsQuery.eq("isDelete", 0);
        long totalQuestions = questionService.count(totalQuestionsQuery);
        stats.put("totalQuestions", totalQuestions);

        // 2. Completed Questions
        QueryWrapper<QuestionSubmit> completedQuestionsQuery = new QueryWrapper<>();
        completedQuestionsQuery.eq("userId", userId).eq("isDelete", 0);
        long completedQuestions = this.count(completedQuestionsQuery);
        stats.put("completedQuestions", completedQuestions);

        // 3. Submission Stats
        Map<String, Long> submissionStats = new HashMap<>();
        QueryWrapper<QuestionSubmit> submissionSuccessQuery = new QueryWrapper<>();
        submissionSuccessQuery .eq("userId", userId)
                .eq("isDelete", 0)
                .apply("JSON_EXTRACT(judgeInfo, '$.message') = 'Correct answer'");

        long successSubmissions = this.count(submissionSuccessQuery );
        submissionStats.put("success", successSubmissions);

        QueryWrapper<QuestionSubmit>submissionFailedQuery=new QueryWrapper<>();
        submissionSuccessQuery .eq("userId", userId)
                .eq("isDelete", 0)
                .apply("JSON_EXTRACT(judgeInfo, '$.message') = 'Wrong Answer'");
        long failedSubmissions = this.count(submissionFailedQuery);
        submissionStats.put("failed", failedSubmissions);

//        submissionQuery.eq("status", 1);
//        long pendingSubmissions = this.count(submissionQuery);
//        submissionStats.put("pending", pendingSubmissions);

        stats.put("submissionStats", submissionStats);

        // 4. Language Stats
        Map<String, Long> languageStats = new HashMap<>();
        QueryWrapper<QuestionSubmit> languageQuery = new QueryWrapper<>();
        languageQuery.eq("userId", userId).eq("isDelete", 0);
        languageQuery.eq("language", "java");
        long javaSubmissions = this.count(languageQuery);
        languageStats.put("java", javaSubmissions);

        QueryWrapper<QuestionSubmit> pyLanguageQuery = new QueryWrapper<>();
        pyLanguageQuery.eq("language", "python").eq("isDelete", 0);
        long pythonSubmissions = this.count(pyLanguageQuery);
        languageStats.put("python", pythonSubmissions);

        QueryWrapper<QuestionSubmit> cLanguageQuery = new QueryWrapper<>();
        cLanguageQuery.eq("language", "c").eq("isDelete", 0);
        long cppSubmissions = this.count(cLanguageQuery);
        languageStats.put("c", cppSubmissions);

        stats.put("languageStats", languageStats);

        // 5. Activity Stats
        Map<String, Long> activityStats = new HashMap<>();
        QueryWrapper<QuestionSubmit> activityQuery = new QueryWrapper<>();
        activityQuery.eq("userId", userId).eq("isDelete", 0);
        activityQuery.select("DATE(createTime) as date, count(*) as count");
        activityQuery.groupBy("DATE(createTime)");

        List<Map<String, Object>> activityResults = this.listMaps(activityQuery);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (Map<String, Object> result : activityResults) {
            Date date = (Date) result.get("date");
            String dates = dateFormat.format(date); // 将 Date 转换为 String
            Long counts = (Long) result.get("count");
            activityStats.put(dates, counts);
        }

        stats.put("activityStats", activityStats);
        System.out.println(stats);
        return stats;


    }

    @Override
    public List<QuestionSubmit> userSubmissions(Long userId) {
        QueryWrapper<QuestionSubmit>queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("userId",userId).eq("isDelete",0);
        return  this.list(queryWrapper);
    }

    /**
     * 获取用户做题水平
     * @param submissions
     * @return
     */
    public Map<String, Object> getLevel(List<QuestionSubmit> submissions) {
        Map<String, Object> level = new HashMap<>();

        return null;
    }
}
