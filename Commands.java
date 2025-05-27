/*
 * Copyright (c) 2022. UnknownNetworkService Group
 * This file is created by UnknownObject at 2022 - 9 - 30
 */

package com.qrs.maincarcontrolapp.constants;

//关于指令的常量值
public class Commands
{
	//帧头/帧尾
	public static byte FRAME_HEAD_0 = (byte) 0x55;
	public static byte FRAME_HEAD_1 = (byte) 0xAA;
	public static byte FRAME_END = (byte) 0xBB;
	
	//指令校验失败
	public static byte CMD_NOT_MATCH = (byte) 0xEE;
	
	//系统自检状态
	public static byte STATUS_SUCCESS = (byte) 0xA1;
	public static byte STATUS_FAILED = (byte) 0xB1;
	
	//二维码
	public static byte QR_SUCCESS_1 = (byte) 0xA2;
	public static byte QR_SUCCESS_2 = (byte) 0xC2;
	public static byte QR_FAILED = (byte) 0xB2;
	
	//交通灯
	public static byte TRAFFIC_LIGHT_SUCCESS = (byte) 0xA3;
	public static byte TRAFFIC_LIGHT_FAILED = (byte) 0xB3;
	public static byte TRAFFIC_LIGHT_RED = (byte) 0x01;
	public static byte TRAFFIC_LIGHT_GREEN = (byte) 0x02;
	public static byte TRAFFIC_LIGHT_YELLOW = (byte) 0x03;
	
	//车牌
	public static byte CAR_ID_SUCCESS_FIRST = (byte) 0xA4;
	public static byte CAR_ID_FAILED = (byte) 0xB4;

	//破损车牌
	public static byte BROKEN_CAR_ID_SUCCESS_FIRST = (byte) 0xA5;
	public static byte BROKEN_CAR_ID_FAILED = (byte) 0xB5;

	//颜色
	public static final byte COLOR_UNKOWN = (byte) 0x00;
	public static final byte COLOR_BLUE = (byte) 0x01;
	public static final byte COLOR_GREEN = (byte) 0x02;
	public static final byte COLOR_YELLOW = (byte) 0x03;
	public static final byte COLOR_RED = (byte) 0x04;
	public static final byte COLOR_BLACK = (byte) 0x05;

	
	//形状颜色
	public static byte COLOR_SHAPE_SUCCESS = (byte) 0xA6;
	public static byte COLOR_SHAPE_DATA_PART2 = (byte) 0xC6;
	public static byte COLOR_SHAPE_DATA_PART3 = (byte) 0xD6;
	public static byte COLOR_SHAPE_FAILED = (byte) 0xB6;
	
	//交通标志
	public static byte TRAFFIC_SIGN_SUCCESS = (byte) 0xA7;
	public static byte TRAFFIC_SIGN_FAILED = (byte) 0xB7;
	public static byte TRAFFIC_SIGN_TYPE_NO_ENTRY = (byte) 0x06;
	public static byte TRAFFIC_SIGN_TYPE_NO_STRAIGHT = (byte) 0x05;
	public static byte TRAFFIC_SIGN_TYPE_STRAIGHT = (byte) 0x01;
	public static byte TRAFFIC_SIGN_TYPE_TURN_LEFT = (byte) 0x02;
	public static byte TRAFFIC_SIGN_TYPE_TURN_RIGHT = (byte) 0x03;
	public static byte TRAFFIC_SIGN_TYPE_U_TURN = (byte) 0x04;
	public static byte TRAFFIC_SIGN_TYPE_LIMIT = (byte) 0x07;
	//TFT显示器下翻一页
	public static byte TFT_PAGE_DOWN = (byte) 0xA8;
	
	//OCR（文本识别）
	public static byte OCR_TEXT_SUCCESS = (byte) 0xA9;
	public static byte OCR_TEXT_FAILED = (byte) 0xB9;
	public static byte OCR_TEXT_LENGTH = (byte) 0xC9;
	public static byte OCR_TEXT_DATA = (byte) 0xD9;
	public static byte OCR_TEXT_FINISH = (byte) 0xE9;
	
	//车型
	public static byte VEHICLE_SUCCESS = (byte) 0xAA;
	public static byte VEHICLE_FAILURE = (byte) 0xBA;
	public static byte VEHICLE_TYPE_BIKE = (byte) 0x0A;
	public static byte VEHICLE_TYPE_MOTOR = (byte) 0x0B;
	public static byte VEHICLE_TYPE_CAR = (byte) 0x0C;
	public static byte VEHICLE_TYPE_TRUCK = (byte) 0x0D;
	public static byte VEHICLE_TYPE_VAN = (byte) 0x0E;
	public static byte VEHICLE_TYPE_BUS = (byte) 0x0F;
	
	
	//运行指定任务
	public static byte RUN_SINGE_TASK = (byte) 0xA0;
	public static byte TASK_NUMBER_0 = 0x00;
	public static byte TASK_NUMBER_1 = 0x01;
	public static byte TASK_NUMBER_2 = 0x02;
	public static byte TASK_NUMBER_3 = 0x03;
	public static byte TASK_NUMBER_4 = 0x04;
	public static byte TASK_NUMBER_5 = 0x05;
	public static byte TASK_NUMBER_6 = 0x06;
	public static byte TASK_NUMBER_7 = 0x07;
	public static byte TASK_NUMBER_8 = 0x08;
	public static byte TASK_NUMBER_9 = 0x09;
	public static byte TASK_NUMBER_10 = 0x10;
	public static byte TASK_NUMBER_11 = 0x11;
	public static byte TASK_NUMBER_12 = 0x12;
	public static byte TASK_NUMBER_13 = 0x13;
	public static byte TASK_NUMBER_14 = 0x14;
	public static byte TASK_NUMBER_15 = 0x15;
	
	//全自动模式
	public static final byte RECEIVE_FULL_AUTO = (byte) 0xA0;
	
	//摄像头预设位置
	public static final byte RECEIVE_CAMERA_POS = (byte) 0xA1;
	public static final byte RECEIVE_CAMERA_POS1 = 0x01;
	public static final byte RECEIVE_CAMERA_POS2 = 0x02;
	public static final byte RECEIVE_CAMERA_POS3 = 0x03;
	public static final byte RECEIVE_CAMERA_POS4 = 0x04;
	
	//全自动模式使用的接收指令
	public static final byte RECEIVE_QR = (byte) 0xA2;
	public static final byte RECEIVE_TRAFFIC_LIGHT = (byte) 0xA3;
	public static final byte RECEIVE_CAR_ID = (byte) 0xA4;
	public static final byte RECEIVE_SHAPE_COLOR = (byte) 0xA5;
	public static final byte RECEIVE_TRAFFIC_SIGN = (byte) 0xA6;
	public static final byte RECEIVE_TEXT_OCR = (byte) 0xA7;
	public static final byte RECEIVE_OCR_DATA_OK = (byte) 0xB7;
	public static final byte RECEIVE_VEHICLE = (byte) 0xA8;

	public static final byte RECEIVE_BROKEN_CAR_ID = (byte) 0xA9;
}
