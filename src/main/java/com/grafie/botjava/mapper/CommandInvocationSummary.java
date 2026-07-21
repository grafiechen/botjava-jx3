package com.grafie.botjava.mapper;

/**
 * 群指令调用聚合投影。
 */
public interface CommandInvocationSummary {

    String getCommandName();

    long getInvocationCount();

    long getSuccessCount();

    double getAverageElapsedMillis();
}
