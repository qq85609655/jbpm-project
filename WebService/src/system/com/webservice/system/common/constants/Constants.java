package com.webservice.system.common.constants;

/** 
 * <p>Description: [常量]</p>
 * @author  <a href="mailto: swpigris81@126.com">大牙-小白</a>
 * @version v0.1
 */
public class Constants {
    //---------------请款状态----------------------
    /**
     * 00-申请请款
     */
    public static String CASH_STATUS_00 = "00";
    /**
     * 01-发起审核
     */
    public static String CASH_STATUS_01 = "01";
    /**
     * 02-审核通过
     */
    public static String CASH_STATUS_02 = "02";
    /**
     * 03-审核驳回
     */
    public static String CASH_STATUS_03 = "03";
    /**
     * 04-发起审批
     */
    public static String CASH_STATUS_04 = "04";
    /**
     * 05-审批通过
     */
    public static String CASH_STATUS_05 = "05";
    /**
     * 06-审批驳回
     */
    public static String CASH_STATUS_06 = "06";
    
    //---------------审核请款类型------------------
    /**
     * 01 - 审核通过
     */
    public static String CHECK_PASS = "01";
    /**
     * 00 - 审核驳回
     */
    public static String CHECK_BAK = "00";
    /**
     * 11 - 审批通过
     */
    public static String APPROVE_PASS = "11";
    /**
     * 10 - 审批驳回
     */
    public static String APPROVE_BAK = "10";
    /**
     * 99 - 再次发起
     */
    public static String APPLY_AGAIN = "99";
    
    //----------------流程图ID---------------------
    /**
     * 请款流程图ID
     */
    public static String PROCESS_LOAN_ID = "com.webservice.cashRequest";
    /**
     * 请款流程图名字
     */
    public static String PROCESS_LOAN_NAME = "cashRequest.bpmn"; 
    /**
     * 系统默认用户
     */
    public static String ADMINISTRATOR = "Administrator";
}