package com.oj.ojbackendquestionservice.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oj.ojbackendcommon.common.ErrorCode;
import com.oj.ojbackendcommon.constant.CommonConstant;
import com.oj.ojbackendcommon.exception.BusinessException;
import com.oj.ojbackendcommon.exception.ThrowUtils;
import com.oj.ojbackendcommon.utils.SqlUtils;
import com.oj.ojbackendmodel.model.dto.question.QuestionQueryRequest;
import com.oj.ojbackendmodel.model.entity.Question;
import com.oj.ojbackendmodel.model.entity.QuestionSubmit;
import com.oj.ojbackendmodel.model.entity.User;
import com.oj.ojbackendmodel.model.vo.QuestionVO;
import com.oj.ojbackendmodel.model.vo.UserVO;
import com.oj.ojbackendquestionservice.mapper.QuestionMapper;
import com.oj.ojbackendquestionservice.service.QuestionService;
import com.oj.ojbackendserviceclient.service.UserFeignClient;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
* @author 李鱼皮
* @description 针对表【question(题目)】的数据库操作Service实现
* @createDate 2023-08-07 20:58:00
*/
@Service
public class QuestionServiceImpl extends ServiceImpl<QuestionMapper, Question>
    implements QuestionService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private UserFeignClient userFeignClient;



    /**
     * 校验题目是否合法
     *
     * @param question
     * @param add
     */
    @Override
    public void validQuestion(Question question, boolean add) {
        if (question == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String title = question.getTitle();
        String content = question.getContent();
        String tags = question.getTags();
        String answer = question.getAnswer();
        String judgeCase = question.getJudgeCase();
        String judgeConfig = question.getJudgeConfig();
        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isAnyBlank(title, content, tags), ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("title", title).eq("isDelete", 0);
        if(this.exists(queryWrapper)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目已存在");
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(title) && title.length() > 80) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题过长");
        }
        if (StringUtils.isNotBlank(content) && content.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "内容过长");
        }
        if (StringUtils.isNotBlank(answer) && answer.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "答案过长");
        }
        if (StringUtils.isNotBlank(judgeCase) && judgeCase.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "判题用例过长");
        }
        if (StringUtils.isNotBlank(judgeConfig) && judgeConfig.length() > 8192) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "判题配置过长");
        }
    }

    /**
     * 获取查询包装类（用户根据哪些字段查询，根据前端传来的请求对象，得到 mybatis 框架支持的查询 QueryWrapper 类）
     *
     * @param questionQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        if (questionQueryRequest == null) {
            return queryWrapper;
        }
        Long id = questionQueryRequest.getId();
        String title = questionQueryRequest.getTitle();
        String content = questionQueryRequest.getContent();
        List<String> tags = questionQueryRequest.getTags();
        String answer = questionQueryRequest.getAnswer();
        Long userId = questionQueryRequest.getUserId();
        String sortField = questionQueryRequest.getSortField();
        String sortOrder = questionQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.like(StringUtils.isNotBlank(answer), "answer", answer);
        if (CollectionUtils.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public QuestionVO getQuestionVO(Question question, HttpServletRequest request) {
        QuestionVO questionVO = QuestionVO.objToVo(question);
        // 1. 关联查询用户信息
        Long userId = question.getUserId();
        User user = null;
        if (userId != null && userId > 0) {
            user = userFeignClient.getById(userId);
        }
        UserVO userVO = userFeignClient.getUserVO(user);
        questionVO.setUserVO(userVO);
        return questionVO;
    }


    @Override
    public Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request) {
        List<Question> questionList = questionPage.getRecords();
        Page<QuestionVO> questionVOPage = new Page<>(questionPage.getCurrent(), questionPage.getSize(), questionPage.getTotal());
        if (CollectionUtils.isEmpty(questionList)) {
            return questionVOPage;
        }
        // 1. 关联查询用户信息
        Set<Long> userIdSet = questionList.stream().map(Question::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userFeignClient.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 填充信息
        List<QuestionVO> questionVOList = questionList.stream().map(question -> {
            QuestionVO questionVO = QuestionVO.objToVo(question);
            Long userId = question.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            questionVO.setUserVO(userFeignClient.getUserVO(user));
            return questionVO;
        }).collect(Collectors.toList());
        questionVOPage.setRecords(questionVOList);
        return questionVOPage;
    }

    @Override
    public List<Question> recommendQuestions(Long userId,List<QuestionSubmit> userSubmissions) {
        String  nowDate = LocalDate.now().toString();
        //1. 获取用户最近三天内的题目ID集合

        String keyPattern = userId + "date:*";
        Set<String> keys = redisTemplate.keys(keyPattern);
        List<Object> values = new ArrayList<>();
        if (keys != null) {
            for (String key : keys) {
                Object value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    values.add(value);
                }
            }
        }
        List<Object> recentSubmissionIds=new ArrayList<>();
        if (values.isEmpty()) {
            recentSubmissionIds= new ArrayList<>();
        }else {
            recentSubmissionIds = (List<Object>) values.get(0);
        }


        if (userSubmissions.isEmpty()) {
            return getDefaultRecommendations(); // 默认推荐方法需自行实现
        }

        // 2. 批量预加载题目信息（解决N+1查询问题）
        Set<Long> questionIds = userSubmissions.stream()
                .map(QuestionSubmit::getQuestionId)
                .collect(Collectors.toSet());

        Map<Long, Question> questionMap = this.listByIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        // 3. 使用ThreadLocal缓存ObjectMapper（JSON解析优化）
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Double> tagFailureRatio = new HashMap<>();
        Map<String, Integer> tagTotalCount = new HashMap<>();

        // 4. 分析用户表现（优化后的标签统计）
        userSubmissions.forEach(submission -> {
            Question question = questionMap.get(submission.getQuestionId());
            if (question == null) return;

            try {
                JsonNode judgeNode = mapper.readTree(submission.getJudgeInfo());
                boolean isSuccess = "Accepted".equals(judgeNode.path("message").asText());

                Arrays.stream(question.getTags().split(","))
                        .filter(StringUtils::isNotBlank)
                        .forEach(tag -> {
                            tagTotalCount.merge(tag, 1, Integer::sum);
                            if (!isSuccess) {
                                tagFailureRatio.merge(tag, 1.0, Double::sum);
                            }
                        });
            } catch (JsonProcessingException e) {
                // 记录日志或按失败处理
            }
        });

        // 5. 计算失败率并筛选薄弱标签
        List<String> strugglingTags = tagTotalCount.entrySet().stream()
                .filter(entry -> {
                    double failures = tagFailureRatio.getOrDefault(entry.getKey(), 0.0);
                    return (failures / entry.getValue()) > 0.5;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (strugglingTags.isEmpty()) {
            return getDefaultRecommendations();
        }

        // 6. 构建标签查询条件（使用FIND_IN_SET优化匹配）
        LambdaQueryWrapper<Question> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Question::getIsDelete, 0);

        // 构建OR条件：tags字段包含任意一个薄弱标签
        strugglingTags.forEach(tag ->
                queryWrapper.or(wrapper ->
                        wrapper.apply("FIND_IN_SET({0}, tags)", tag)
                )
        );

        // 7. 获取推荐题目并过滤已做题目
        Set<Long> attemptedIds = questionMap.keySet();
        // 假定recentSubmissionIds内的对象是Long类型或能转换为Long类型
        Set<Long> recentSubmissionIdsSet = recentSubmissionIds.stream()
                .map(o -> ((Number) o).longValue()) // 如果recentSubmissionIds中的对象不是Long类型而是其他数字类型，如Integer，请根据实际情况调整。
                .collect(Collectors.toSet());

        List<Question> recommendations = this.list(queryWrapper).stream()
                .filter(q -> !attemptedIds.contains(q.getId())) // 已存在的过滤条件
                .filter(q -> !recentSubmissionIdsSet.contains(q.getId())|| recentSubmissionIdsSet==null||recentSubmissionIdsSet.isEmpty()) // 新增：过滤掉recentSubmissionIds中存在的id
                .sorted(Comparator
                        .comparingInt((Question q) -> countMatchingTags(q, strugglingTags))
                        .thenComparing(Question::getSubmitNum).reversed()
                )
                .limit(10) // 限制推荐数量
                .collect(Collectors.toList());
        //8.获取推荐题目id
        List<Long> recommendQuestionIds = recommendations.stream()
                .map(Question::getId)
                .collect(Collectors.toList());
        // 设置时区，例如 Asia/Shanghai
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        // 计算次日凌晨的时间
        ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(zoneId);
        // 计算当前时间到午夜的秒数差
        long secondsUntilMidnight = ChronoUnit.SECONDS.between(now, midnight);



        // 设置推荐题目缓存，过期时间为秒数差
        redisTemplate.opsForValue().set(
                userId+":recommendations", // 建议用冒号分隔键名
                recommendations,
                secondsUntilMidnight,
                TimeUnit.SECONDS
        );

        // 设置推荐题目id三天缓存
        ZonedDateTime threeDay = now.toLocalDate().plusDays(3).atStartOfDay(zoneId);
        long secondsUntilThreeDay = ChronoUnit.SECONDS.between(now, threeDay);
        redisTemplate.opsForValue().set(
                userId+"date:"+nowDate+"recommendations", // 建议用冒号分隔键名
                recommendQuestionIds,
                secondsUntilThreeDay,
                TimeUnit.SECONDS
        );

        System.out.println(recommendations);
        return recommendations.isEmpty() ? getDefaultRecommendations() : recommendations;
    }


    // 辅助方法：计算题目匹配的薄弱标签数量
    private int countMatchingTags(Question q, List<String> targetTags) {
        return (int) Arrays.stream(q.getTags().split(","))
                .filter(targetTags::contains)
                .count();
    }

    // 默认推荐方法（示例）
    private List<Question> getDefaultRecommendations() {
        return this.lambdaQuery()
                .eq(Question::getIsDelete, 0)
                .orderByDesc(Question::getSubmitNum)
                .last("LIMIT 10")
                .list();
    }


}






