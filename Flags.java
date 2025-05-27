package com.qrs.maincarcontrolapp.constants;

/**
 * Created by Guo on 2016/10/27.
 * Modified by UnknownObject at 2022-09-18
 */

//一些其他的常量值
public class Flags
{
	
	//日志TAG
	public static final String CLIENT_TAG = "MAIN-CAR-CLIENT";
	public static final String CLIENT_SEND_EX = "SEND-EX-RECEIVED";
	public static final String SUB_ACTIVITY_TAG = "SUB-ACTIVITY";
	
	//摄像头控制指令
	public static final int CAMERA_UP = 0;
	public static final int CAMERA_DOWN = 2;
	public static final int CAMERA_LEFT = 4;
	public static final int CAMERA_RIGHT = 6;
	public static final int CAMERA_STEP_0 = 0;
	public static final int CAMERA_STEP_1 = 1;
	public static final int CAMERA_STEP_2 = 2;
	public static final int CAMERA_RE_INIT = 2;
	public static final int CAMERA_SET_POS_1 = 32;
	public static final int CAMERA_GET_POS_1 = 33;
	public static final int CAMERA_SET_POS_2 = 34;
	public static final int CAMERA_GET_POS_2 = 35;
	public static final int CAMERA_SET_POS_3 = 36;
	public static final int CAMERA_GET_POS_3 = 37;
	public static final int CAMERA_SET_POS_4 = 38;
	public static final int CAMERA_GET_POS_4 = 39;
	
	//主/从车移动控制指令
	public static final byte CMD_PACKET_MAIN_CAR = (byte) 0xAA;
	public static final byte CMD_PACKET_SUB_CAR = (byte) 0x02;
	public static final byte CMD_PACKET_MOVE_FORWARD = (byte) 0x02;
	public static final byte CMD_PACKET_MOVE_BACKWARD = (byte) 0x03;
	public static final byte CMD_PACKET_MOVE_LEFT = (byte) 0x04;
	public static final byte CMD_PACKET_MOVE_RIGHT = (byte) 0x05;
	public static final byte CMD_PACKET_MOVE_STOP = (byte) 0x01;
	public static final byte CMD_PACKET_MOVE_TO_LINE = (byte) 0x06;
	
	//recvHandler指令
	public static final int RECEIVED_IMAGE = 11;
	public static final int RECEIVED_CAR_DATA = 12;
	public static final int PRINT_DATA_ARRAY = 13;
	public static final int PRINT_SYSTEM_LOG = 14;
	
	//交通灯颜色定义
	public enum TrafficLightColors
	{
		RED,
		YELLOW,
		GREEN,
		NULL
	}
}
