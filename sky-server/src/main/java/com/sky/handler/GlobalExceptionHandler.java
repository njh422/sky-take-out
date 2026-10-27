package com.sky.handler;

import com.sky.constant.MessageConstant;
import com.sky.exception.BaseException;
import com.sky.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 全局异常处理器，处理项目中抛出的业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 唯一索引冲突提示语中重复值的匹配规则，例如：Duplicate entry '张三' for key 'employee.idx_username'
     */
    private static final Pattern DUPLICATE_ENTRY_PATTERN =
            Pattern.compile("Duplicate entry '(.*)' for key '(.*)'");

    /**
     * 捕获业务异常
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result exceptionHandler(BaseException ex){
        log.error("异常信息：{}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /**
     * 捕获数据库唯一约束冲突异常（如员工用户名重复）
     * MyBatis 会将 SQLIntegrityConstraintViolationException 翻译为 DuplicateKeyException，这里一并处理
     * @param ex
     * @return
     */
    @ExceptionHandler({DuplicateKeyException.class, SQLIntegrityConstraintViolationException.class})
    public Result duplicateKeyExceptionHandler(Exception ex){
        String causeMessage = getRootCauseMessage(ex);
        log.error("唯一约束冲突：{}", causeMessage);

        Matcher matcher = DUPLICATE_ENTRY_PATTERN.matcher(causeMessage);
        if (matcher.find()) {
            //取出重复的值，例如：张三
            String duplicateValue = matcher.group(1);
            return Result.error(duplicateValue + MessageConstant.ALREADY_EXISTS);
        }
        return Result.error(MessageConstant.UNKNOWN_ERROR);
    }

    /**
     * 获取最底层异常的描述信息
     * @param ex
     * @return
     */
    private String getRootCauseMessage(Throwable ex) {
        Throwable rootCause = ex;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause.getMessage() == null ? "" : rootCause.getMessage();
    }

}
