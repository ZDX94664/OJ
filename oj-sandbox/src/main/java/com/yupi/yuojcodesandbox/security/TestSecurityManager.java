package com.yupi.yuojcodesandbox.security;

import cn.hutool.core.io.FileUtil;

import java.io.File;
import java.nio.charset.Charset;

/**
 * 测试安全管理器
 */
public class TestSecurityManager {

    public static void main(String[] args) {

        int a=Integer.parseInt(args[0]);
        int b=Integer.parseInt(args[1]);
        System.out.println("结果："+a+b);
    }
}
