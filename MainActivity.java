/*
 * Copyright (c) 2022. UnknownNetworkService Group
 * This file is created by UnknownObject at 2022 - 9 - 18
 */

package com.qrs.maincarcontrolapp.gui;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;


import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;
import static com.qrs.maincarcontrolapp.tools.CvUtils.LOWER_YELLOW;
import static com.qrs.maincarcontrolapp.tools.CvUtils.UPPER_YELLOW;
import static com.qrs.maincarcontrolapp.tools.CvUtils.getVehicleColor;
import static java.lang.Thread.sleep;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.text.format.Formatter;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.qrs.maincarcontrolapp.detect.YoloV5CarTypeNcnn;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.king.wechat.qrcode.WeChatQRCodeDetector;
import com.qrs.maincarcontrolapp.R;
import com.qrs.maincarcontrolapp.communicate.CommandDecoder;
import com.qrs.maincarcontrolapp.communicate.CommandEncoder;
import com.qrs.maincarcontrolapp.communicate.DataTransferCore;
import com.qrs.maincarcontrolapp.communicate.SerialPortTransferCore;
import com.qrs.maincarcontrolapp.communicate.WifiTransferCore;
import com.qrs.maincarcontrolapp.constants.Commands;
import com.qrs.maincarcontrolapp.constants.Flags;
import com.qrs.maincarcontrolapp.constants.GlobalColor;
import com.qrs.maincarcontrolapp.detect.EfficientnetTrafficLightNcnn;
import com.qrs.maincarcontrolapp.detect.PaddleOCRNcnn;
import com.qrs.maincarcontrolapp.detect.ResultObj;
import com.qrs.maincarcontrolapp.detect.YoloV5TrafficLightNcnn;
import com.qrs.maincarcontrolapp.detect.YoloV5TrafficLogoNcnn;
import com.qrs.maincarcontrolapp.msg.OCRRecognizeRes;
import com.qrs.maincarcontrolapp.msg.QRCodeRes;
import com.qrs.maincarcontrolapp.tools.CameraOCRUtils;
import com.qrs.maincarcontrolapp.tools.CameraRequest;
import com.qrs.maincarcontrolapp.tools.CvUtils;
import com.qrs.maincarcontrolapp.tools.ImageReleaser;
import com.qrs.maincarcontrolapp.tools.LogUtil;
import com.qrs.maincarcontrolapp.tools.MediaUtils;
import com.qrs.maincarcontrolapp.tools.OCRDataReleaser;
import com.qrs.maincarcontrolapp.tools.QRCodeUtils;
import com.qrs.maincarcontrolapp.tools.TextFilter;
import com.qrs.maincarcontrolapp.tools.camera.CameraOperator;
import com.qrs.maincarcontrolapp.tools.camera.CameraSearcher;

import org.opencv.OpenCV;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity
{
	private YoloV5TrafficLightNcnn yolov5TrafficLightNcnn = new YoloV5TrafficLightNcnn();
	private EfficientnetTrafficLightNcnn efficientnetTrafficLightNcnn = new EfficientnetTrafficLightNcnn();

	private YoloV5CarTypeNcnn yoloV5CarTypeNcnn = new YoloV5CarTypeNcnn();

	private PaddleOCRNcnn paddleOCRNcnn = new PaddleOCRNcnn();
	private YoloV5TrafficLogoNcnn yoloV5TrafficLogoNcnn = new YoloV5TrafficLogoNcnn();

	//数据接收处理器
	private final Handler recvHandler;
	//主车相机地址，默认值不重要
	private String IPCamera = "10.254.254.100";
	//摄像头当前图像
	private Bitmap currImage;
	//存储小车的IP
	private String IPCar = '192.168.31.134';
	//通信客户端
	private DataTransferCore dtc_client;
	//DHCP相关信息，用于获取IP地址
	private DhcpInfo dhcpInfo;
	//WIFI管理器
	private WifiManager wifiManager;
	//控件——存储从摄像头获取到的图像
	private ImageView pic_received;
	//控件——存储Toast的内容，由ToastLog()进行管理
	private TextView text_toast;
	//主车摄像头移动
	CameraRequest cameraRequest = new CameraRequest();
	//寻找主车相机
	private CameraSearcher cameraSearcher = null;
	//程序状态，通过所有自检后会设置为true
	private boolean SystemStatus = false;
	//文件状态，获取到存储权限并释放完毕所有文件会设置为true+
	private boolean FileStatus = false;
	//申请存储空间使用的请求码
	private final int permission_request_code = 0x100942;
	//串口通信的设备名
	private final String SerialPortPath = "/dev/ttyS4";
	//通信通道标志，true为Wifi，false为串口
	private final boolean CommunicationUsingWifi = true;

	byte b = 0 ;

	byte rec =0;
	//红绿灯检测



	static {
		System.loadLibrary("opencv_java3");
		System.loadLibrary("yolov5ncnn");
	}

	//调试用16进制数组
	private final String[] byte_str = {
			"0x00", "0x01", "0x02", "0x03", "0x04", "0x05", "0x06", "0x07", "0x08", "0x09", "0x0A", "0x0B", "0x0C", "0x0D", "0x0E", "0x0F",
			"0x10", "0x11", "0x12", "0x13", "0x14", "0x15", "0x16", "0x17", "0x18", "0x19", "0x1A", "0x1B", "0x1C", "0x1D", "0x1E", "0x1F",
			"0x20", "0x21", "0x22", "0x23", "0x24", "0x25", "0x26", "0x27", "0x28", "0x29", "0x2A", "0x2B", "0x2C", "0x2D", "0x2E", "0x2F",
			"0x30", "0x31", "0x32", "0x33", "0x34", "0x35", "0x36", "0x37", "0x38", "0x39", "0x3A", "0x3B", "0x3C", "0x3D", "0x3E", "0x3F",
			"0x40", "0x41", "0x42", "0x43", "0x44", "0x45", "0x46", "0x47", "0x48", "0x49", "0x4A", "0x4B", "0x4C", "0x4D", "0x4E", "0x4F",
			"0x50", "0x51", "0x52", "0x53", "0x54", "0x55", "0x56", "0x57", "0x58", "0x59", "0x5A", "0x5B", "0x5C", "0x5D", "0x5E", "0x5F",
			"0x60", "0x61", "0x62", "0x63", "0x64", "0x65", "0x66", "0x67", "0x68", "0x69", "0x6A", "0x6B", "0x6C", "0x6D", "0x6E", "0x6F",
			"0x70", "0x71", "0x72", "0x73", "0x74", "0x75", "0x76", "0x77", "0x78", "0x79", "0x7A", "0x7B", "0x7C", "0x7D", "0x7E", "0x7F",
			"0x80", "0x81", "0x82", "0x83", "0x84", "0x85", "0x86", "0x87", "0x88", "0x89", "0x8A", "0x8B", "0x8C", "0x8D", "0x8E", "0x8F",
			"0x90", "0x91", "0x92", "0x93", "0x94", "0x95", "0x96", "0x97", "0x98", "0x99", "0x9A", "0x9B", "0x9C", "0x9D", "0x9E", "0x9F",
			"0xA0", "0xA1", "0xA2", "0xA3", "0xA4", "0xA5", "0xA6", "0xA7", "0xA8", "0xA9", "0xAA", "0xAB", "0xAC", "0xAD", "0xAE", "0xAF",
			"0xB0", "0xB1", "0xB2", "0xB3", "0xB4", "0xB5", "0xB6", "0xB7", "0xB8", "0xB9", "0xBA", "0xBB", "0xBC", "0xBD", "0xBE", "0xBF",
			"0xC0", "0xC1", "0xC2", "0xC3", "0xC4", "0xC5", "0xC6", "0xC7", "0xC8", "0xC9", "0xCA", "0xCB", "0xCC", "0xCD", "0xCE", "0xCF",
			"0xD0", "0xD1", "0xD2", "0xD3", "0xD4", "0xD5", "0xD6", "0xD7", "0xD8", "0xD9", "0xDA", "0xDB", "0xDC", "0xDD", "0xDE", "0xDF",
			"0xE0", "0xE1", "0xE2", "0xE3", "0xE4", "0xE5", "0xE6", "0xE7", "0xE8", "0xE9", "0xEA", "0xEB", "0xEC", "0xED", "0xEE", "0xEF",
			"0xF0", "0xF1", "0xF2", "0xF3", "0xF4", "0xF5", "0xF6", "0xF7", "0xF8", "0xF9", "0xFA", "0xFB", "0xFC", "0xFD", "0xFE", "0xFF"};

	//原生函数导入，用于长按保存的功能
//	private static native boolean SaveImage(Bitmap img, String time);






	@SuppressLint("HandlerLeak")
	public MainActivity()
	{
		//接收内部消息的处理器，用于处理内部消息
		recvHandler = new Handler()
		{
			@SuppressLint("SetTextI18n")
			@Override
			public void handleMessage(Message msg)
			{
				super.handleMessage(msg);
				//内部消息：收到图片；执行操作：更新GUI上的图片
				if (msg.what == Flags.RECEIVED_IMAGE)
					pic_received.setImageBitmap(currImage);
				//内部消息：打印数组；操作：打印收到的数组
				if (msg.what == Flags.PRINT_DATA_ARRAY)
				{
					byte[] data = (byte[]) msg.obj;
					if (data != null)
						ToastLog("SEND: [" + ByteArray2String(data) + "]", false, false);
				}
				//内部消息：打印日志；操作：打印收到的日志
				if (msg.what == Flags.PRINT_SYSTEM_LOG)
				{
					String str = (String) msg.obj;
					if (str != null)
						ToastLog(str, false, false);
				}
				//内部消息：收到主车数据；执行操作：解析指令并执行
				if (msg.what == Flags.RECEIVED_CAR_DATA)
				{
					byte[] recv = (byte[]) msg.obj;
					if (recv != null)
					{
						//打印接收到的指令
						ToastLog("RECV: [" + ByteArray2String(recv) + "]", false, false);
						//解析指令
						CommandDecoder decoder = new CommandDecoder(recv);
						if (decoder.CommandReady())
						{
							ToastLog("Command Decode Ready.", false, false);
							Thread th_run_command = new Thread(() ->
							{
								byte[] sBuf = null;
								switch (decoder.GetMainCommand())
								{
									//收到全自动指令，返回程序自检状态
									case Commands.RECEIVE_FULL_AUTO:
										LogUtil.log("get RECEIVE_FULL_AUTO Command");
										dtc_client.Send(SystemStatusCommand());
										break;
									//收到QR指令，开始识别二维码，回传识别成功的数据
									case Commands.RECEIVE_QR:
//										LogUtil.log("get QrCode detect Commond");
										cameraRequest.get(CameraRequest.STATIC_SIGNS_URL);

										MediaUtils.saveBitmapToGallery(MainActivity.this, MainActivity.this.currImage, "QRCode");
										Sleep(1500);
										Sleep(1500);
										byte[] Buf = RecognizeQrcode4Color();
//										byte[] Buf = RecognizeQrCodeByWechatWithRotate3ColorOnlyRed();
//										byte[] Buf = null;



//										try {
//											MainActivity.this.currImage = BitmapFactory.decodeStream(MainActivity.this.getAssets().open("054.png"));
//										} catch (IOException e) {
//											throw new RuntimeException(e);
//										}
//										byte[] Buf = getVehicle(currImage);
										//LogUtil.log("返回二维码的识别结果：" + TextFilter.bytesToHexString(sBuf));
										if(Buf == null){
											CommandEncoder ce1 = new CommandEncoder();
											byte[] arr3 = ce1.GenerateCommand((byte)0xB2, (byte)0x00, (byte)0x00, (byte)0x00);
											dtc_client.Send(arr3);
											break;
										}
										CommandEncoder qr = new CommandEncoder();
										byte[] qrr = qr.GenerateCommand((byte)0xA2, Buf[0], Buf[1],Buf[2],(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00, (byte)0x00);


										dtc_client.Send(qrr);

										break;
//										LogUtil.log("get QrCode detect Commond");
//										cameraRequest.get(CameraRequest.STATIC_SIGNS_URL);
//										Sleep(1500);
//										MediaUtils.saveBitmapToGallery(MainActivity.this, MainActivity.this.currImage, "QRCode");
//										sBuf = RecognizeQrCode(false, null);
//
//										LogUtil.log("返回二维码的识别结果：" + TextFilter.bytesToHexString(sBuf));
//										dtc_client.Send(sBuf);
//										break;
									//收到TRAFFIC_LIGHT指令，开始识别交通灯，回传识别成功的数据
									case Commands.RECEIVE_TRAFFIC_LIGHT:
										LogUtil.log("get TRAFFIC_LIGHT detect Commond");
										cameraRequest.get(CameraRequest.TRAFFIC_LIGHT_URL);
										Sleep(1500);
										Sleep(1500);
										MediaUtils.saveBitmapToGallery(MainActivity.this, MainActivity.this.currImage, "TrafficLight");


//											try {
//	MainActivity.this.currImage = BitmapFactory.decodeStream(MainActivity.this.getAssets().open("dir1/red.jpg"));
//	} catch (IOException e) {
//		throw new RuntimeException(e);
//	}


										byte light = RecognizeTrafficLight();
//										LogUtil.log("返回交通灯识别结果：" + TextFilter.bytesToHexString(sBuf));
										CommandEncoder ght = new CommandEncoder();
										byte[] lig = ght.GenerateCommand((byte)0xA3,light,(byte)0x00,(byte)0x00);
										dtc_client.Send(lig);
										break;
									//收到SHAPE_COLOR指令，开始识别形状颜色，回传识别成功的数据
									case Commands.RECEIVE_SHAPE_COLOR:
										LogUtil.log("get SHAPE_COLOR detect Commond");
										cameraRequest.get(CameraRequest.DISPLAY_URL);
										Sleep(1500);
										MediaUtils.saveBitmapToGallery(MainActivity.this, MainActivity.this.currImage, "TrafficLight");
										RecognizeShapeColor();
										break;
									//收到CAR_ID指令，开始识别车牌号，回传识别成功的数据
									case Commands.RECEIVE_CAR_ID:
										LogUtil.log("get CAR_ID detect Commond");
										cameraRequest.get(CameraRequest.DISPLAY_URL);
										Sleep(1500);
										Sleep(1500);
										MediaUtils.saveBitmapToGallery(MainActivity.this, MainActivity.this.currImage, "CARID");
										Sleep(1500);
										String res =null;

										if(rec == 0) {
											 rec = RecognizeVehicle2();
										}
										if(rec==1) {
											if (b == 0) {
												b = RecognizeVehicle();
												CommandEncoder ce1 = new CommandEncoder();
												byte[] arr3 = ce1.GenerateCommand((byte) 0xB4, (byte) 0x00, (byte) 0x00, (byte) 0x00);
												dtc_client.Send(arr3);

												break;
											}
										}

										if(b != 0) {
											 res = RecognizeCarID();
										}
//			try {
//				MainActivity.this.currImage = BitmapFactory.decodeStream(MainActivity.this.getAssets().open("054.png"));
//			} catch (IOException e) {
//				throw new RuntimeException(e);
//			}
//										byte[] res = getVehicle(currImage);

										if (res != null) {
//											byte ct = RecognizeVehicle();
//											if (ct != CameraOCRUtils.CAR_ID_COLOR) {
//												CommandEncoder ce1 = new CommandEncoder();
//												byte[] arr3 = ce1.GenerateCommand((byte) 0xB4, (byte) 0x00, (byte) 0x00, (byte) 0x00);
//												dtc_client.Send(arr3);
//											}

											byte[] arr = res.getBytes() ;
											CommandEncoder ce = new CommandEncoder();
											byte[] arr1 = ce.GenerateCommand((byte) 0xA4, arr[0], arr[1], arr[2], arr[3], arr[4], arr[5],b,(byte)0x00);
//											CommandEncoder ce1 = new CommandEncoder();
//											byte[] arr2 = ce1.GenerateCommand((byte) 0xA5, arr[3], arr[4], arr[5]);
//											CommandEncoder ce2 = new CommandEncoder();
//											byte[] arr3 = ce2.GenerateCommand((byte)0xAE, ct, (byte)0x00,(byte)0x00);
											dtc_client.Send(arr1);
//											Sleep(2000);
//											dtc_client.Send(arr2);
//											Sleep(2000);
//											dtc_client.Send(arr3);
										} else {
											CommandEncoder ce1 = new CommandEncoder();
											byte[] arr3 = ce1.GenerateCommand((byte) 0xB4, (byte) 0x00, (byte) 0x00, (byte) 0x00);
											dtc_client.Send(arr3);
										}
										break;
									//收到TRAFFIC_SIGN指令，开始识别交通标志，回传识别成功的数据
									case Commands.RECEIVE_TRAFFIC_SIGN:
										LogUtil.log("收到交通标志的识别指令！");
										cameraRequest.get(CameraRequest.DISPLAY_URL);
										Sleep(1500);
										MediaUtils.saveBitmapToGallery(MainActivity.this, MainActivity.this.currImage, "TRAFFICSIGN");
										Sleep(1500);
										Sleep(1500);

										byte sign = RecognizeTrafficSign();

										if(sign != 0){
											CommandEncoder ce1 = new CommandEncoder();
											byte[] arr3 = ce1.GenerateCommand((byte) 0xA7, sign, (byte) 0x00,(byte) 0x00);
											dtc_client.Send(arr3);
											break;
										}

//										if(sign == 0){
//											CommandEncoder ce1 = new CommandEncoder();
//											byte[] arr3 = ce1.GenerateCommand((byte) 0xB7, (byte) 0x00, (byte) 0x00, (byte) 0x00);
//											dtc_client.Send(arr3);
//											break;
//										}
										CommandEncoder ce1 = new CommandEncoder();
										byte[] arr3 = ce1.GenerateCommand((byte) 0xb7, (byte) 0x00, (byte) 0x00, (byte) 0x00);
										dtc_client.Send(arr3);
										break;
									//收到OCR指令，开始识别文本，回传识别成功的数据
									case Commands.RECEIVE_TEXT_OCR:
										LogUtil.log("收到OCR的识别指令！");
										cameraRequest.get(CameraRequest.STATIC_SIGNS_URL);
										Sleep(1500);
										sBuf = OCRRecognizeText();

										CommandEncoder ce22 = new CommandEncoder();
										byte[] arr4 = ce22.GenerateCommand((byte) 0xD1, sBuf[0], sBuf[1],sBuf[2],sBuf[3],sBuf[4],sBuf[5],sBuf[6],sBuf[7]);
										CommandEncoder ce23 = new CommandEncoder();
										byte[] arr41 = ce23.GenerateCommand((byte) 0xD2, sBuf[8], sBuf[9],sBuf[10],sBuf[11],sBuf[12],sBuf[13],sBuf[14],sBuf[15]);
										CommandEncoder ce24 = new CommandEncoder();
										byte[] arr42 = ce24.GenerateCommand((byte) 0xD2, sBuf[16], sBuf[17],sBuf[18],sBuf[19],sBuf[20],sBuf[21],sBuf[22],sBuf[23]);
										CommandEncoder ce25 = new CommandEncoder();
										byte[] arr43 = ce25.GenerateCommand((byte) 0xD2, sBuf[24], sBuf[25],sBuf[26],sBuf[27],(byte) 0x00,(byte) 0x00,(byte) 0x00,(byte) 0x00);

										CommandEncoder ce28 = new CommandEncoder();
										byte[] arr47 = ce28.GenerateCommand((byte) 0xDD,  (byte) 0x00, (byte) 0x00, (byte) 0x00);

										dtc_client.Send(arr4);
										Sleep(1500);
										dtc_client.Send(arr41);
										Sleep(1500);
										dtc_client.Send(arr42);
										Sleep(1500);
										dtc_client.Send(arr43);
										Sleep(1500);

										dtc_client.Send(arr47);
										Sleep(1500);

										break;
									//收到车型识别指令，开始识别车型，回传识别成功的数据
									case Commands.RECEIVE_VEHICLE:
//										LogUtil.log("收到车型的识别指令！");
//										new CameraRequest().get(CameraRequest.DISPLAY_URL);
//										if(ct != 0){
//											CommandEncoder ce1 = new CommandEncoder();
//											byte[] arr3 = ce1.GenerateCommand((byte)0xAE, ct, (byte)0x00, (byte)0x00);
//											dtc_client.Send(arr3);
//										}
//
//										else{
//											LogUtil.log("车型识别失败！");
//										}
//										break;
									case Commands.RECEIVE_BROKEN_CAR_ID:
										LogUtil.log("收到破损车牌的识别指令！");
										cameraRequest.get(CameraRequest.DISPLAY_URL);
										Sleep(1500);
										sBuf = decoder.GetDataCommand();
										LogUtil.log("返回破损车牌的识别结果！" + TextFilter.bytesToHexString(sBuf));
										dtc_client.Send(sBuf);
										break;
									//收到未知指令，回传异常指令，表示无法解析当前指令
									default:
										CommandEncoder error = new CommandEncoder();
										dtc_client.Send(error.GenerateCommand(Commands.CMD_NOT_MATCH, (byte) 0x00, (byte) 0x00, (byte) 0x00));
										break;
								}
							});
							th_run_command.start();
						}
						//指令解析失败，回传异常指令，表示无法解析当前指令
						else
						{
							CommandEncoder error = new CommandEncoder();
							dtc_client.ThreadSend(error.GenerateCommand(Commands.CMD_NOT_MATCH, (byte) 0x00, (byte) 0x00, (byte) 0x00));
						}
					}
					//收到NULL，输出日志，不做操作
					else
						ToastLog("NULL Received", true, false);
				}
			}
		};
	}

	@Override
	public void onResume()
	{
		super.onResume();
		if (!OpenCVLoader.initDebug()) {
			Log.d("MainActivity", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
		} else {
			Log.d("MainActivity", "OpenCV library found inside package. Using it!");
			mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		}
	}

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			// TODO Auto-generated method stub
			switch (status){
				case BaseLoaderCallback.SUCCESS:
					Log.i("MainActivity", "成功加载");
					break;
				default:
					super.onManagerConnected(status);
					Log.i("MainActivity", "加载失败");
					break;
			}
		}
	};

	//处理二维码数据，使用从C++代码中导出的算法
	private byte[] ProcessQRData(ArrayList<String> qr_data)
	{
		String validate_result = "";
		CommandEncoder encoder = new CommandEncoder();
		TextFilter filter = new TextFilter();
		for (String data : qr_data)
		{
			String processed_data = filter.ChineseOnly(data);
			if (processed_data.length() > 0)
			{
				validate_result = processed_data;
				break;
			}
		}
		if (validate_result.equals(""))
			return encoder.GenerateCommand(Commands.QR_FAILED, (byte) 0, (byte) 0, (byte) 0);
		else
			//return MainCarAES.CalcAES(validate_result);
			return null;
	}


	//处理彩色二维码数据，使用从C++代码中导出的算法
	private byte[] ProcessColoredQRData(String qr_data)
	{
		CommandEncoder encoder = new CommandEncoder();
		if (qr_data.equals(""))
			return encoder.GenerateCommand(Commands.QR_FAILED, (byte) 0, (byte) 0, (byte) 0);
		else
//			return MainCarAES.CalcAES(qr_data);
			return null;
	}

	//获取程序自检指令，根据自检状态返回成功或失败
	private byte[] SystemStatusCommand()
	{
		CommandEncoder encoder = new CommandEncoder();
		if (SystemStatus && FileStatus)
			return encoder.GenerateCommand(Commands.STATUS_SUCCESS, (byte) 0, (byte) 0, (byte) 0);
		else
			return encoder.GenerateCommand(Commands.STATUS_FAILED, (byte) 0, (byte) 0, (byte) 0);
	}


	//识别二维码
	private byte[] RecognizeQrCode(boolean colored, GlobalColor target_color) {

//		Bitmap bitmap;
//		try {
//			bitmap = BitmapFactory.decodeStream(this.getAssets().open("qr04.png"));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}

		String QRCodeStr = null;
		if (!colored) {
			QRCodeStr = QRCodeUtils.QRCodeAcquire(currImage);
			System.out.println(QRCodeStr);
		} else {
			//彩色二维码
		}

		QRCodeRes qrCodeRes = new QRCodeRes();

		if(QRCodeStr == null || QRCodeStr.isEmpty()){
			qrCodeRes.setQrCode(null);
		}
		else{
			byte[] bytes = null;
			try {
				bytes = QRCodeStr.getBytes("GBK");
				qrCodeRes.setQrCode(bytes);
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}

		return qrCodeRes.pack();
	}

	/**
	 * 每次旋转5度，旋转180度，使用微信的接口检测二维码
	 * @param colored 是否要区分颜色
	 * @param target_color 目标颜色的数值，当不需要区分颜色时设置为null，当需要区分颜色时需要指定颜色的值，与Commands类中颜色的值对应
	 * @return QRCodeRes打包后的字节数组，当没有检测到时，字节填一个0
	 */
	private byte[] RecognizeQrCodeByWechatWithRotate(boolean colored, Integer target_color){

				Bitmap bitmap;
		try {
			bitmap = BitmapFactory.decodeStream(this.getAssets().open("j2qr.png"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}


//		Bitmap bitmap = this.currImage;
		//获取二维码的检测结果
		Map<String, Integer> m = this.getQrcodeResult(colored, bitmap,4);
		QRCodeRes res = new QRCodeRes();
		//没有检测到时处理异常
		if(m == null || m.size() == 0){
			//错误处理
			LogUtil.log("None Qrcode detected");
			res.setQrCode(null);
			return res.pack();
		}
		byte[] bytes = null;
		//如果包含颜色
		if(colored){
			for(String s : m.keySet()){
				if(target_color == null){
					LogUtil.log("Specify color detect but no color assigned!");
					res.setQrCode(null);
					return res.pack();
				}

				if(m.get(s).intValue() == target_color.intValue()){
					try {
						bytes = s.getBytes("GBK");
					} catch (UnsupportedEncodingException e) {
						res.setQrCode(null);
						return res.pack();
					}
					res.setQrCode(bytes);
					LogUtil.log("qrCode without color detect result is :" + s);
					return res.pack();
				}
			}
		}
		else{
			for(String s : m.keySet()){
				try {
					bytes = s.getBytes("GBK");
				} catch (UnsupportedEncodingException e) {
					res.setQrCode(null);
					return res.pack();
				}
				res.setQrCode(bytes);
				LogUtil.log("qrCode without color detect result is :" + s);
				return res.pack();
			}
		}

		return res.pack();
	}


	private byte[] RecognizeQrCodeByWechatWithRotatemn(boolean colored, Integer target_color){

//		Bitmap bitmap;
//		try {
//			bitmap = BitmapFactory.decodeStream(this.getAssets().open("j2qr.png"));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//

		Bitmap bitmap = this.currImage;
		//获取二维码的检测结果
		Map<String, Integer> m = this.getQrcodeResult(colored, bitmap);
		QRCodeRes res = new QRCodeRes();
		//没有检测到时处理异常
		if(m == null || m.size() == 0){
			//错误处理
			LogUtil.log("None Qrcode detected");
			res.setQrCode(null);
			return res.pack();
		}
		byte[] bytes = null;
		//如果包含颜色
		if(colored){
			for(String s : m.keySet()){
				if(target_color == null){
					LogUtil.log("Specify color detect but no color assigned!");
					res.setQrCode(null);
					return res.pack();
				}

				if(m.get(s).intValue() == target_color.intValue()){
					try {
						bytes = s.getBytes("GBK");
					} catch (UnsupportedEncodingException e) {
						res.setQrCode(null);
						return res.pack();
					}
					res.setQrCode(bytes);
					LogUtil.log("qrCode without color detect result is :" + s);
					return res.pack();
				}
			}
		}
		else{
			for(String s : m.keySet()){
				try {
					bytes = s.getBytes("GBK");
				} catch (UnsupportedEncodingException e) {
					res.setQrCode(null);
					return res.pack();
				}
				res.setQrCode(bytes);
				LogUtil.log("qrCode without color detect result is :" + s);
				return res.pack();
			}
		}

		return res.pack();
	}

	private byte[] RecognizeQrCodeByWechatWithRotateRSA(boolean colored, Integer target_color){

		Bitmap bitmap = this.currImage;
		//获取二维码的检测结果
		Map<String, Integer> m = this.getQrcodeResult(false, bitmap);
		String str = "";

		//遍历取出
		for(String v : m.keySet()){
			str = v;
		}
		//通过‘/’拆分
		String[] newstr = str.split("/");
		//判断数量是否正确
		if(newstr.length != 2){
			return null;
		}
		//取出数字部分1
		String str0 = newstr[0];
		int a = str0.indexOf("<");
		int b = str0.indexOf(">");
		String str3 = str0.substring(a,b);
		//按‘，’分割
		String[] nums = str3.split(",");
		//转成int
		List<Integer> num1 = new ArrayList();
		for(int i=0 ;i<nums.length ; i++){
			num1.add(Integer.parseInt(nums[i]));
		}
		//取出数字部分2
		String str1 = newstr[1];
		str3 = str1.substring(a,b);
		nums = str3.split(",");
		List<Integer> num2 = new ArrayList();
		for( int i =0 ;i<nums.length ; i++){
			num2.add(Integer.parseInt(nums[i]));
		}


		QRCodeRes res = new QRCodeRes();
		//没有检测到时处理异常
		if(m == null || m.size() == 0){
			//错误处理
			LogUtil.log("None Qrcode detected");
			res.setQrCode(null);
			return res.pack();
		}
		byte[] bytes = null;
		//如果包含颜色
		if(colored){
			for(String s : m.keySet()){
				if(target_color == null){
					LogUtil.log("Specify color detect but no color assigned!");
					res.setQrCode(null);
					return res.pack();
				}

				if(m.get(s).intValue() == target_color.intValue()){
					try {
						bytes = s.getBytes("GBK");
					} catch (UnsupportedEncodingException e) {
						res.setQrCode(null);
						return res.pack();
					}
					res.setQrCode(bytes);
					LogUtil.log("qrCode without color detect result is :" + s);
					return res.pack();
				}
			}
		}
		else{
			for(String s : m.keySet()){
				try {
					bytes = s.getBytes("GBK");
				} catch (UnsupportedEncodingException e) {
					res.setQrCode(null);
					return res.pack();
				}
				res.setQrCode(bytes);
				LogUtil.log("qrCode without color detect result is :" + s);
				return res.pack();
			}
		}

		return res.pack();
	}
	private byte[] RecognizeQrCodeByWechatWithRotateRSAWenZi(boolean colored, Integer target_color){

		Bitmap bitmap;
		try {
			bitmap = BitmapFactory.decodeStream(this.getAssets().open("qr.png"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}


		byte[] textBytes = new byte[32];
//		Bitmap bitmap = this.currImage;
		//获取二维码的检测结果
		Map<String, Integer> m = this.getQrcodeResult(false, bitmap);
		String str = "";

		//遍历取出
		for(String v : m.keySet()){
			str = v;
			byte[] b = str.getBytes();
			if(b[0]>=33 && b[0]<=126){

			}
			else{

				try {
					String str1 = "为中华民族伟大复兴而读书";
					textBytes = str1.getBytes("GBK");
					System.out.println("text：" + str);
					System.out.println("OCRTEXT：" + textBytes);
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
				return textBytes;
			}
		}

		return null;
	}

	private byte[] RecognizeQrCodeByWechatWithRotate3Color(){
		Bitmap bitmap;
		try {
			bitmap = BitmapFactory.decodeStream(this.getAssets().open("qr4.jpg"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		//Bitmap bitmap = this.currImage;
		//获取二维码的检测结果
		Map<String, Integer> m1 = getQrcodeResult(true, bitmap);

//		Map<String, Integer> m1 =new HashMap<String,Integer>();
//		String str = "abc";
//		str.indexOf("abc");
		String str1="";
		String str2="";
		String str3="";
		byte[] dat = new byte[3];

//		m1.put("<HERE IS A SIMPLE EXAMPLE>",1);
//		m1.put("31<EXAMPLE>",2);
//		m1.put("yu7<33>",3);
		for(String key : m1.keySet()){
				Integer value = m1.get(key);
			int beginInx = key.indexOf("<");
			int endInx = key.indexOf(">");
			String str4 = key.substring(beginInx+1,endInx);
			if(value == 4){
				str1= str4;
			}else if(value ==2){
				str2 = str4;
			}else if(value == 3){
				str3 = str4;
			}
		}
		if(str3 == ""){
			return null;
		}
		int n = str1.indexOf(str2);
		int x1 = Integer.parseInt(str3);
		int x = x1/10;
		int y = x1%10;
		//var m101 = str.substring(1,19);

		dat[2] = (byte) n;
		dat[0] = (byte) x;
		dat[1] = (byte) y;
		return dat;



	}



	private byte[] RecognizeQrCodeByWechatWithRotate3ColorOnlyRed(){
//		Bitmap bitmap;
//		try {
//			bitmap = BitmapFactory.decodeStream(this.getAssets().open("t44444.png"));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}

		Bitmap bitmap = this.currImage;
		//获取二维码的检测结果
		Map<String, Integer> m1 = getQrcodeResult(true, bitmap);

//		Map<String, Integer> m1 =new HashMap<String,Integer>();
//		String str = "abc";
//		str.indexOf("abc");
		String str1="";

		int s =0;
		byte[] dat = new byte[3];

//		m1.put("<HERE IS A SIMPLE EXAMPLE>",1);
//		m1.put("31<EXAMPLE>",2);
//		m1.put("yu7<33>",3);
		for(String key : m1.keySet()){

			Integer value = m1.get(key);
			if(value == 4){
				str1= key;
			}
		}
		byte[] bytes = str1.getBytes();
		for(int i =0 ;i < bytes.length ;i++){

			if(((bytes[i] >='1' && bytes[i]<='9') || (bytes[i] >='A' && bytes[i]<='F'))&& s==0 ){
				dat[s] = bytes[i];
				s++;
			}
			if(bytes[i] >='0' && bytes[i]<='2' && s==1 ){
				dat[s] = bytes[i];
				s++;
			}
			if(((bytes[i] >='0' && bytes[i]<='9') || (bytes[i] >='A' && bytes[i]<='F'))&& s==2 ){
				dat[s] = bytes[i];
			}
		}

		return dat;

	}


	private byte[] RecognizeQrcode3Color(){
		String str1 = "";
		String str2 = "";
		String str3 = "";
		Bitmap bitmap = this.currImage;
		//获取二维码的检测结果
		//Map<String, Integer> m1 = getQrcodeResult(true, bitmap);
		Map<String, Integer> m1 =new HashMap<String,Integer>();
		int max = 9;
		m1.put("A2b\\BnY(yEeFf,3”GgHd)",1);
		for(String key : m1.keySet()){
			 str1 = key;
			 byte[] bytes = str1.getBytes();
			 //[65, 50, 98...] onCreate 方法   static的用法
			for(byte b : bytes){
				if(b > '0' && b <= '9'){

				}
			}


			 char[] ch = str1.toCharArray();
			for(int i=0;i<ch.length;i++){
				if(ch[i] >'0' && ch[i]<'9'){
					int max1 = Character.getNumericValue(ch[i]);
					if(max1<max){
						max = max1;
					}
				}
			}
		}
		System.out.println(max);
		return null;
	}


	private byte[] RecognizeQrcode4Color(){

//				Bitmap bitmap;
//		try {
//			bitmap = BitmapFactory.decodeStream(this.getAssets().open("q22.jpg"));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}


		Bitmap bitmap = this.currImage;
		//获取二维码的检测结果
		Map<String, Integer> m1 = getQrcodeResult(true, bitmap,4);
//		Map<String, Integer> m1 =new HashMap<String,Integer>();
		if(m1 == null || m1.keySet().size() == 0){
			return null;
		}
		byte[] qr = extracted(m1);


		return qr;
	}



	private byte[] RecognizeQrcode(){
		Bitmap bitmap;
		try {
			bitmap = BitmapFactory.decodeStream(this.getAssets().open("25.png"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
//		Bitmap bitmap = this.currImage;
		String str1 = "";
		String str2 = "";
		//获取二维码的检测结果
		Map<String, Integer> m1 = getQrcodeResult(true, bitmap);
		for(String key : m1.keySet()){
			if(key.length() == 8){
				str1 = key;
			}else{
				str2 = key;
			}

		}
		byte[] b1 = str1.getBytes();
		byte[] b2 = {1,0,0,0,1,0};
		int j=0;
		for(int i=0;i< b1.length;i++){

			byte b = b1[i];
			if(b2[j] == 1){
				if(b>='A'&&b<='Z'){
					b2[j] = b;
					j++;
				}
			}
			else if(b2[j] == 0){
				if(b>='0'&&b<='9'){
					b2[j] = b;
					j++;
				}
			}
		}

		return b2;
	}

	private byte[] RecognizeQrcodeMn2(){
		Bitmap bitmap;
		try {
			bitmap = BitmapFactory.decodeStream(this.getAssets().open("qrm4.png"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
//		Bitmap bitmap = this.currImage;
		String str1 = "";
		String str2 = "";
		//获取二维码的检测结果
		Map<String, Integer> m1 = getQrcodeResult(false, bitmap);

		for(String st : m1.keySet()){
			if(st.indexOf("<") > 0 ){
				str1 = st;
			}
		}
		int a1 = str1.indexOf("<");
		int a2 = str1.indexOf(">");
		String str11 = str1.substring(a1+1,a2);
		int qr = Integer.parseInt(str11);
		int x = qr/10;
		int y = qr%10;
		byte[] dat = new byte[2];
		dat[0]=(byte) x;
		dat[1]=(byte)y;


		return dat;
	}

    private byte[] RecognizeQrcodeMn29() {
        Bitmap bitmap;
        try {
            bitmap = BitmapFactory.decodeStream(this.getAssets().open("Mn3/4.jpg"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
//		Bitmap bitmap = this.currImage;
        String str1 = "";
        String str2 = "";
        //获取二维码的检测结果
        Map<String, Integer> m1 = getQrcodeResult(false, bitmap);

        for(String st : m1.keySet()) {
            if(st.length() > str1.length()){
                str1 = st;
                str2 = st;
            }
            if(st.length() > str2.length()){
                str2 = st;
            }
            if(st.length() < str2.length()){
                str1 = st;
            }
        }
        int a1 = str1.indexOf("<");
        int a2 = str1.indexOf(">");
        String sr1 = str1.substring(a1+1,a2);
        int b1 = str2.indexOf("<");
        int b2 = str2.indexOf(">");
        String sr2 = str2.substring(b1+1,b2);

		try {
			// 解密的密钥
			String keyString = "makelife";
			// 转换密钥
			SecretKey secretKey = new SecretKeySpec(keyString.getBytes(), "DES");

			// 加密数据
			String encryptedHexData = "0BCA8B4BEED9F7E0DF85F1A450BCE9A8";
			byte[] encryptedData = hexStringToByteArray(encryptedHexData);

			// 执行解密
			Cipher cipher = Cipher.getInstance("DES");
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			byte[] decryptedData = cipher.doFinal(encryptedData);

			// 输出原始数据
			String originalData = new String(decryptedData);
			System.out.println("解密后的数据: " + originalData);

			// 提取字母部分
			String alphaData = originalData.replaceAll("[^a-zA-Z]", "");
			System.out.println("提取的字母部分: " + alphaData);

			// 转换为ASCII码
			byte[] asciiBytes = alphaData.getBytes();

			return asciiBytes;
		} catch (Exception e) {
			e.printStackTrace();
		}

	return null;
    }

    // 将十六进制字符串转换为字节数组
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }




	static int[] result = new int[]{
			2,2,0,2,0, 1,1,2,0,2,//450
			1,0,0,0,1, 2,2,2,2,2,//460
			1,0,0,0,0, 0,0,0,2,2,//470
			2,2,1,2,1, 1,0,1,0,0,//480
			2,2,1,2,0, 1,0,0,0,1,
			0,0,1,1,0, 1,0,2,1,2,
			1,2,2,1,2, 0,2,2,2,1,
			0,2,2,0,1, 2,2,0,2,0, //520
			0,1,2,1,1, 0,2,2,0,2
	};

	private Map getQrcodeResult(boolean colored, Bitmap bitmap) {
		//保存识别结果，字符串为二维码内容，Mat为区域矩阵
		Map<String, Integer> resultMap = new HashMap<>();

		Mat rgbaMat = new Mat();
		Utils.bitmapToMat(bitmap, rgbaMat);
		//从负90度开始旋转
		int angle = -90;
		//旋转180度到+90度，每次旋转5度，检测1次
		for(int i = 0;i < 18;i++){
			Mat mat = rotateImage(rgbaMat, angle + (10 * i));
			Bitmap b1 = Bitmap.createBitmap(rgbaMat.cols(), rgbaMat.rows(), Bitmap.Config.ARGB_8888);
			Utils.matToBitmap(mat, b1);

			List<Mat> points = new ArrayList<Mat>();
			List<String> result1;
			result1 = WeChatQRCodeDetector.detectAndDecode(b1, points);

			for(int j = 0;j < result1.size();j++){
				if(!resultMap.keySet().contains(result1.get(j))){
					if(!colored){
						resultMap.put(result1.get(j), 0);
					}

					else{
						Mat hsvMat = new Mat();
						Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_RGB2HSV);
						Mat matPoints = points.get(j);
						List<Point> areaPoints = new ArrayList<>();
						for(int k = 0; k < matPoints.rows();k++){
							areaPoints.add(new Point(matPoints.get(k, 0)[0], matPoints.get(k, 1)[0]));
						}
						byte color = CvUtils.getQrcodeColorOfWechat(hsvMat, areaPoints);
						resultMap.put(result1.get(j), new Integer(color));
						hsvMat.release();
					}
				}
			}
			mat.release();

		}

		return resultMap;

		//return null;
	}



	private Map getQrcodeResult(boolean colored, Bitmap bitmap,int qrnum) {
		//保存识别结果，字符串为二维码内容，Mat为区域矩阵
		Map<String, Integer> resultMap = new HashMap<>();

		Mat rgbaMat = new Mat();
		Utils.bitmapToMat(bitmap, rgbaMat);
		//从负90度开始旋转
		int angle = -90;
		//旋转180度到+90度，每次旋转5度，检测1次
		for(int i = 0;i < 18;i++){
			Mat mat = rotateImage(rgbaMat, angle + (10 * i));
			Bitmap b1 = Bitmap.createBitmap(rgbaMat.cols(), rgbaMat.rows(), Bitmap.Config.ARGB_8888);
			Utils.matToBitmap(mat, b1);

			List<Mat> points = new ArrayList<Mat>();
			List<String> result1;
			result1 = WeChatQRCodeDetector.detectAndDecode(b1, points);

			for(int j = 0;j < result1.size();j++){
				if(!resultMap.keySet().contains(result1.get(j))){
					if(!colored){
						resultMap.put(result1.get(j), 0);
					}

					else{
						Mat hsvMat = new Mat();
						Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_RGB2HSV);
						Mat matPoints = points.get(j);
						List<Point> areaPoints = new ArrayList<>();
						for(int k = 0; k < matPoints.rows();k++){
							areaPoints.add(new Point(matPoints.get(k, 0)[0], matPoints.get(k, 1)[0]));
						}
						byte color = CvUtils.getQrcodeColorOfWechat(hsvMat, areaPoints);
						resultMap.put(result1.get(j), new Integer(color));
						hsvMat.release();
					}
				}
			}
			mat.release();

			if(result1.size() >= qrnum){
				return resultMap;
			}
		}

		return resultMap;

		//return null;
	}

	//识别交通灯
	private byte RecognizeTrafficLight() {
//		try {
//			this.currImage = BitmapFactory.decodeStream(this.getAssets().open("209.jpg"));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}



		ResultObj[] yoloObjs = yolov5TrafficLightNcnn.Detect(currImage, false);
		if(yoloObjs == null || yoloObjs.length == 0){
			Log.e(TAG, "检测失败");
			LogUtil.log("检测失败, 没有找到白板！");
			//直接交给Efficientnet分类
			//返回0
//			TrafficLightRes trafficLightRes = new TrafficLightRes();
//			trafficLightRes.setLightColor((byte)1);
//			return trafficLightRes.pack();
			return 0;
		}

		ResultObj yoloTarget = null;
		float maxScore = -1;
		for(ResultObj item : yoloObjs){
			if(item.prob > maxScore){
				maxScore = item.prob;
				yoloTarget = item;
			}
		}

		Mat mat = new Mat();
		Utils.bitmapToMat(currImage, mat);

		System.out.println("res.score = " + yoloTarget);
		if(!yoloTarget.isValid()){
			System.out.println("预测错误" );
//			TrafficLightRes trafficLightRes = new TrafficLightRes();
//			trafficLightRes.setLightColor((byte)1);
//			return trafficLightRes.pack();
			return 0;
		}

		// 截取指定区域
		Rect roi = new Rect((int)yoloTarget.x, (int)yoloTarget.y, (int)yoloTarget.w, (int)yoloTarget.h);
		Mat croppedMat = new Mat(mat, roi);
		Mat rgbMat = new Mat();
		Imgproc.cvtColor(croppedMat, rgbMat, Imgproc.COLOR_RGBA2RGB);
		//截取出目标框交给efficientnet分类
		Bitmap croppedBitmap = Bitmap.createBitmap(rgbMat.cols(), rgbMat.rows(), Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(croppedMat, croppedBitmap);

		// 释放资源
		mat.release();
		croppedMat.release();

		int sign = efficientnetTrafficLightNcnn.Detect(croppedBitmap, false);

		if(sign == 3){
			LogUtil.log("无法通过分类获取灯的颜色，将通过opencv进行检测");
			sign = this.judgeByOpencv(rgbMat);
			LogUtil.log("Opencv检测结果为：" + sign);
		}


		byte color = 0;
		switch (sign){
			case 0:
				Log.d(TAG, "是绿灯");
				LogUtil.log("是绿灯");
				color = Commands.TRAFFIC_LIGHT_GREEN;
				break;
			case 1:
 				Log.d(TAG, "是红灯");
				LogUtil.log("是红灯");
				color = Commands.TRAFFIC_LIGHT_RED;
				break;
			case 2:
				Log.d(TAG, "是黄灯");
				LogUtil.log("是黄灯");
				color = Commands.TRAFFIC_LIGHT_YELLOW;
				break;
			default:
				LogUtil.log("错误的分类");
				Log.e(TAG, "错误的分类");
				break;
		}

//		TrafficLightRes trafficLightRes = new TrafficLightRes();
//		trafficLightRes.setLightColor(color);
//		return trafficLightRes.pack();
//		return new byte[]{(byte)sign};
		return color;
	}

	private int judgeByOpencv(Mat croppedMat) {

		if(croppedMat.cols() < croppedMat.rows() || croppedMat.rows() <= 20){
			//检测到的内容不正确
			return -1;
		}
		Mat image = croppedMat.clone();
		Mat hsvMat = new Mat();
		Imgproc.cvtColor(croppedMat, hsvMat, Imgproc.COLOR_RGB2HSV);

		for(int r = 0;r < hsvMat.rows();r++){
			for(int c = 0;c < hsvMat.cols();c++){
				double[] colors = hsvMat.get(r, c);
				//Log.d("MainActivity", String.format("colors:%f, %f, %f" ,(float)colors[0], colors[1], colors[2]));
				if(colors[2] < 250){
					image.put(r, c, 0, 0, 0);
				}
				else {
					image.put(r, c, 255,255,255);
				}
			}
		}

		Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
		MediaUtils.saveBitmapToGallery(MainActivity.this, bitmap, "opencv1");

		Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
		Imgproc.morphologyEx(image, image, Imgproc.MORPH_CLOSE, kernel);

		Mat grayMat = new Mat();
		Imgproc.cvtColor(image, grayMat, Imgproc.COLOR_RGB2GRAY);

		bitmap = Bitmap.createBitmap(grayMat.cols(), grayMat.rows(), Bitmap.Config.ARGB_8888);
		MediaUtils.saveBitmapToGallery(MainActivity.this, bitmap, "opencv2");

		// 查找轮廓
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(grayMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		Object[] objects = contours.toArray();
		// 找到最大轮廓
		double maxArea = -1;
		MatOfPoint maxContour = null;

		for (MatOfPoint contour : contours) {
			double area = Imgproc.contourArea(contour);
			if (area > maxArea) {
				maxArea = area;
				maxContour = contour;
			}
		}

		Point[] points = new Point[4];
		// 找到最大轮廓的最小外接矩形
		if (maxContour == null) {
			return -1;

		}

		RotatedRect minRect = Imgproc.minAreaRect(new MatOfPoint2f(maxContour.toArray()));

		// 获取最小外接矩形的信息
		minRect.points(points);

		Log.d(TAG, String.format("0x=%f,1x=%f,2x=%f,cols=%d", points[0].x, points[1].x, points[2].x, croppedMat.cols()));
		//如果最左侧的点小于高度，必然是红灯
		double minx =Math.min(Math.min(points[0].x, points[1].x),Math.min(points[2].x, points[3].x));
		double maxx =Math.max(Math.max(points[0].x, points[1].x),Math.max(points[2].x, points[3].x));
		if(minx< croppedMat.rows() * 3 / 4){
			return 1;
		}
		else if(maxx > croppedMat.cols() / 2){
			return 0;
		}
		else{
			return 2;
		}

	}

	//识别形状颜色
	private void RecognizeShapeColor()
	{

	}

	//车牌颜色
	private byte Carcolor() {
//		Bitmap bitmap;
//		try {
//			curImage = BitmapFactory.decodeStream(this.getAssets().open("054.png"));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
		CameraOCRUtils ocrUtils = CameraOCRUtils.getInstance(this.paddleOCRNcnn);
		//ocrUtils.carColorProcess(bitmap);
		//byte carcolor = ocrUtils.carColorProcess(currImage);
		//byte carcolor = ocrUtils.carColorProcess(bitmap);


		return 0;
	}


	//识别车牌
	private String RecognizeCarID() {
//		Bitmap bitmap;
//		try {
//			currImage = BitmapFactory.decodeStream(this.getAssets().open("121.jpg"));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
		CameraOCRUtils ocrUtils = CameraOCRUtils.getInstance(this.paddleOCRNcnn);
		String carID = ocrUtils.carIDProcess(currImage);
//		String carID = ocrUtils.carIDProcess2(bitmap);
//		OCRRecognizeRes ocrRecognizeRes = new OCRRecognizeRes();
//		ocrRecognizeRes.setOCRCarID(carID);
		return carID;

	}


	//识别交通标志
	private byte RecognizeTrafficSign() {
//
//		try {
//				MainActivity.this.currImage = BitmapFactory.decodeStream(MainActivity.this.getAssets().open("c0.png"));
//			} catch (IOException e) {
//				throw new RuntimeException(e);
//			}


		ResultObj[] yoloObjs = yoloV5TrafficLogoNcnn.Detect(currImage, false);
		if(yoloObjs == null || yoloObjs.length == 0){
			Log.e(TAG, "检测失败");
			//直接交给Efficientnet分类
			//返回0
//			TrafficSignRes trafficSignRes = new TrafficSignRes();
//			trafficSignRes.setTrafficSign((byte) 0);
//			return trafficSignRes.pack();
			return 0;
		}
		if(yoloObjs.length > 1){
			Log.e(TAG, "检测失败");
			return 0;
		}


		ResultObj yoloTarget = null;
		float maxScore = -1;
		for(ResultObj item : yoloObjs){
			if(item.prob > maxScore){
				maxScore = item.prob;
				yoloTarget = item;
			}
		}




		byte sign = 0;
		switch (yoloTarget.label) {
			case "ban":

				sign = Commands.TRAFFIC_SIGN_TYPE_NO_ENTRY;
				break;
			case "back":
				sign = Commands.TRAFFIC_SIGN_TYPE_U_TURN;
				break;
			case "left":
				sign = Commands.TRAFFIC_SIGN_TYPE_TURN_LEFT;
				break;
			case "right":
				sign = Commands.TRAFFIC_SIGN_TYPE_TURN_RIGHT;
				break;
			case "straight":
				sign = Commands.TRAFFIC_SIGN_TYPE_STRAIGHT;
				break;
			case "nostraight":
				sign = Commands.TRAFFIC_SIGN_TYPE_NO_STRAIGHT;
				break;
			case "limit":
				sign = Commands.TRAFFIC_SIGN_TYPE_LIMIT;
		}



		//根据标签名获取sign的值

//		TrafficSignRes trafficSignRes = new TrafficSignRes();
//		trafficSignRes.setTrafficSign(sign);
//		return trafficSignRes.pack();
		return sign;
	}


	//识别静态文本
	private byte[] OCRRecognizeText() {

//		Bitmap bitmap;
//		try {
//			currImage = BitmapFactory.decodeStream(this.getAssets().open("t1111.png"));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}

		CameraOCRUtils ocrUtils = CameraOCRUtils.getInstance(this.paddleOCRNcnn);
		byte[] text = ocrUtils.textProcess(currImage);
//		byte[] text = ocrUtils.textProcess(bitmap);

		return text;
	}


	//车型颜色(黄色)
	public byte[] getVehicle(){
//				Bitmap bitmap;
//		try {
//			currImage = BitmapFactory.decodeStream(this.getAssets().open("12.jpg"));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}

		Bitmap bitmap = this.currImage;

		// 将Bitmap转换为Mat
		Mat rgbaMat = new Mat();
		Utils.bitmapToMat(bitmap, rgbaMat);

		// 将RGBA颜色空间转换为HSV颜色空间
		Mat hsvMat = new Mat();
		Imgproc.cvtColor(rgbaMat, hsvMat, Imgproc.COLOR_RGB2HSV);

		//使用OCR进行识别
		//paddleOCRNcnn.Detect(bitmap, false);
		ResultObj[] yoloObjs = yoloV5CarTypeNcnn.Detect(bitmap, false);


		PaddleOCRNcnn.Obj[] results = paddleOCRNcnn.Detect(bitmap, false);
		for (PaddleOCRNcnn.Obj m : results) {
			String strCardId = new TextFilter().LetterAndNumber(m.label);
			if(strCardId.length() != 6 ){
				continue;
			}
			List<Point> points = new ArrayList<>();
			points.add(new Point(m.x0, m.y0));
			points.add(new Point(m.x1, m.y1));
			points.add(new Point(m.x2, m.y2));
			points.add(new Point(m.x3, m.y3));

			for(ResultObj xy : yoloObjs){
				if(m.x0 > xy.x && m.x0 <xy.x+xy.w){
					if(m.y0 >xy.y && m.y0 <xy.y+xy.h){
						int num = getVehicleColor(bitmap,xy, UPPER_YELLOW,LOWER_YELLOW);

						if(num >1000){
							byte sign = 0;
							switch (xy.label) {
								case "truck":
									sign = Commands.COLOR_BLUE;
									break;
								case "car":
									sign = Commands.COLOR_GREEN;
									break;
								case "motor":
									sign = Commands.COLOR_YELLOW;
									break;

							}
							byte[] b = new byte[strCardId.length()+1];
							byte[] b1 = strCardId.getBytes();
							for (int i =0 ;i<strCardId.length();i++){
								b[i] = b1[i];
							}
							b[6] = sign;
							return b;
						}
						else return null;
					}
				}

			}
		}


		return null;
	}



	//车型颜色(对应)
	public byte[] getVehicle2(){

//				Bitmap bitmap;
//		try {
//			currImage = BitmapFactory.decodeStream(this.getAssets().open("c3.png"));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}

		Bitmap bitmap = this.currImage;

		// 将Bitmap转换为Mat
		Mat rgbaMat = new Mat();
		Utils.bitmapToMat(bitmap, rgbaMat);

		// 将RGBA颜色空间转换为HSV颜色空间
		Mat hsvMat = new Mat();
		Imgproc.cvtColor(rgbaMat, hsvMat, Imgproc.COLOR_RGB2HSV);

		//使用OCR进行识别
		//paddleOCRNcnn.Detect(bitmap, false);
		ResultObj[] yoloObjs = yoloV5CarTypeNcnn.Detect(bitmap, false);


		PaddleOCRNcnn.Obj[] results = paddleOCRNcnn.Detect(bitmap, false);
		for (PaddleOCRNcnn.Obj m : results) {
			String strCardId = new TextFilter().LetterAndNumber(m.label);
			if(strCardId.length() != 6 ){
				continue;
			}
			List<Point> points = new ArrayList<>();
			points.add(new Point(m.x0, m.y0));
			points.add(new Point(m.x1, m.y1));
			points.add(new Point(m.x2, m.y2));
			points.add(new Point(m.x3, m.y3));
			//车型
			for(ResultObj xy : yoloObjs){
				if(m.x0 > xy.x && m.x0 <xy.x+xy.w){
					if(m.y0 >xy.y && m.y0 <xy.y+xy.h){
//						int num = getVehicleColor(bitmap,xy, UPPER_YELLOW,LOWER_YELLOW);
						String id = xy.label;



						if(Objects.equals(b, id)){
							byte[] b = new byte[strCardId.length()+1];
							byte[] b1 = strCardId.getBytes();
							for (int i =0 ;i<strCardId.length();i++){
								b[i] = b1[i];
							}

							return b;
						}
					}
				}

			}
		}


		return null;
	}







	// 车牌在车型内
	public byte[] getVehicleMM2(){

//				Bitmap bitmap;
//		try {
//			currImage = BitmapFactory.decodeStream(this.getAssets().open("mnjs.jpg"));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
		Bitmap bitmap = this.currImage;



		int max = 0;
		// 将Bitmap转换为Mat
		Mat rgbaMat = new Mat();
		Utils.bitmapToMat(bitmap, rgbaMat);

		// 将RGBA颜色空间转换为HSV颜色空间
		Mat hsvMat = new Mat();
		Imgproc.cvtColor(rgbaMat, hsvMat, Imgproc.COLOR_RGB2HSV);

		//使用OCR进行识别车型
		//paddleOCRNcnn.Detect(bitmap, false);
		ResultObj[] yoloObjs = yoloV5CarTypeNcnn.Detect(bitmap, false);

		//车牌
		PaddleOCRNcnn.Obj[] results = paddleOCRNcnn.Detect(bitmap, false);
		for (PaddleOCRNcnn.Obj m : results) {
			String strCardId = new TextFilter().LetterAndNumber(m.label);
			if(strCardId.length() != 6 ){
				continue;
			}
			List<Point> points = new ArrayList<>();
			points.add(new Point(m.x0, m.y0));
			points.add(new Point(m.x1, m.y1));
			points.add(new Point(m.x2, m.y2));
			points.add(new Point(m.x3, m.y3));
			byte color = CvUtils.getCarIdColor(hsvMat, points);
			//车型
			for(ResultObj xy : yoloObjs) {

//				if(m.x0 > xy.x && m.x0 <xy.x+xy.w && color == 2){
//					byte[] b = new byte[strCardId.length()];
//					return b;
//				}
			if(xy.label != "bike"){
				if (m.x0 > xy.x && m.x0 < xy.x + xy.w) {
					if (m.y0 > xy.y && m.y0 < xy.y + xy.h) {
						byte sign = 0;
							switch (xy.label) {
								case "motor":
									sign = Commands.COLOR_BLUE;
									break;
								case "truck":
									sign = Commands.COLOR_GREEN;
									break;
								case "car":
									sign = Commands.COLOR_YELLOW;
									break;
							}
							if(sign == 3 && color == Commands.COLOR_BLUE){
								byte[] b = new byte[strCardId.length() + 1];
								byte[] b1 = strCardId.getBytes();
								for (int i = 0; i < strCardId.length(); i++) {
									b[i] = b1[i];
								}
								b[6] = sign;
								return b;
							}
						if(sign == 2 && color == Commands.COLOR_YELLOW){
							byte[] b = new byte[strCardId.length() + 1];
							byte[] b1 = strCardId.getBytes();
							for (int i = 0; i < strCardId.length(); i++) {
								b[i] = b1[i];
							}
							b[6] = sign;
							return b;
						}
						if(sign == 1 && color == Commands.COLOR_YELLOW){
							byte[] b = new byte[strCardId.length() + 1];
							byte[] b1 = strCardId.getBytes();
							for (int i = 0; i < strCardId.length(); i++) {
								b[i] = b1[i];
							}
							b[6] = sign;
							return b;
						}
					}
				}
			}
		}
	}
		return null;
}





	//识别车型
	private byte RecognizeVehicle() {
//		Bitmap bitmap = null;
//		try {
//			currImage = BitmapFactory.decodeStream(this.getAssets().open("054.png"));
//
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//		CameraOCRUtils ocrUtils = CameraOCRUtils.getInstance(this.paddleOCRNcnn);
//		ResultObj[] yoloObjs = yoloV5CarTypeNcnn.Detect(bitmap, false);
		ResultObj[] yoloObjs = yoloV5CarTypeNcnn.Detect(currImage, false);

		if(yoloObjs == null || yoloObjs.length == 0){
			Log.e(TAG, "检测失败");
			//直接交给Efficientnet分类
			//返回0
			return 0;
		}

		ResultObj yoloTarget = null;
		float maxScore = -1;
		for(ResultObj item : yoloObjs){
			if(item.prob > maxScore){
				maxScore = item.prob;
				yoloTarget = item;
			}
		}


		byte sign = 0;
		switch (yoloTarget.label) {
			case "truck":
				sign = Commands.COLOR_GREEN;
				break;
			case "car":
				sign = Commands.COLOR_BLUE;
				break;
			case "motor":
				sign = Commands.COLOR_YELLOW;
				break;

		}

//		String  sign = "";
//		switch (yoloTarget.label) {
//			case "truck":
//				sign = "truck";
//				break;
//			case "car":
//				sign = "car";
//				break;
//			case "motor":
//				sign = "motor";
//				break;
//
//		}

		return sign;
	}


	private byte RecognizeVehicle2() {
//		Bitmap bitmap = null;
//		try {
//			currImage = BitmapFactory.decodeStream(this.getAssets().open("054.png"));
//
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//		CameraOCRUtils ocrUtils = CameraOCRUtils.getInstance(this.paddleOCRNcnn);
//		ResultObj[] yoloObjs = yoloV5CarTypeNcnn.Detect(bitmap, false);
		ResultObj[] yoloObjs = yoloV5CarTypeNcnn.Detect(currImage, false);

		if(yoloObjs == null || yoloObjs.length == 0){
			Log.e(TAG, "检测失败");
			//直接交给Efficientnet分类
			//返回0
			return 0;
		}


			for (ResultObj item : yoloObjs) {
				if (item.prob > 0.6) {
					return 1 ;
				}
			}



//		byte sign = 0;
//		switch (yoloTarget.label) {
//			case "truck":
//				sign = Commands.COLOR_BLUE;
//				break;
//			case "car":
//				sign = Commands.COLOR_GREEN;
//				break;
//			case "motor":
//				sign = Commands.COLOR_YELLOW;
//				break;
//
//		}

//		String  sign = "";
//		switch (yoloTarget.label) {
//			case "truck":
//				sign = "truck";
//				break;
//			case "car":
//				sign = "car";
//				break;
//			case "motor":
//				sign = "motor";
//				break;
//
//		}

 		return 0;
	}




	private byte RecognizeVehicle1()
	{
		Bitmap bitmap = null;
		try {
			currImage = BitmapFactory.decodeStream(this.getAssets().open("car/car.png"));

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		CameraOCRUtils ocrUtils = CameraOCRUtils.getInstance(this.paddleOCRNcnn);
		//ResultObj[] yoloObjs = yoloV5CarTypeNcnn.Detect(currImage, false);
		ResultObj[] yoloObjs = yoloV5CarTypeNcnn.Detect(currImage, false);

		if(yoloObjs == null || yoloObjs.length == 0){
			Log.e(TAG, "检测失败");
			//直接交给Efficientnet分类
			//返回0
			return 0;
		}

		byte sign = 0;
		float maxScore = -1;
		for(ResultObj item : yoloObjs){
			if(item.label == "car"){
				sign =2;
			}
		}



		return sign;
	}



	private static byte[] extracted(Map<String, Integer> map) {
		int asd=0;
		List<Byte> shu2= new ArrayList<>();  //定义ArrayList集合
		for (String s : map.keySet()) {  //遍历
			List<Byte> shu1 = new ArrayList<>();  //定义ArrayList集合
			byte[] shuzhi2 = s.getBytes();    //将map中内容转换成字节型数组
			int color = map.get(s);      //取出键值1，2，3，4等
			if(color==4)
			{
				for (byte b : shuzhi2){              //将shuzhi2插入到shu2中
					if (b>='0'&&b<='9')
						shu1.add(b);
				}
				asd=shu1.get(0)-48;
			}
		}
		for (String s : map.keySet()) {  //遍历
			List<Byte> shu1= new ArrayList<>();  //定义ArrayList集合
			byte[] shuzhi2 = s.getBytes();    //将map中内容转换成字节型数组
			int color = map.get(s);      //取出键值1，2，3，4等
			if(color==1)
			{
				for (byte b : shuzhi2){              //将shuzhi2插入到shu2中
					if (b>='0'&&b<='9')
						shu1.add(b);
				}

				shu1.sort((x, y) ->{ return x - y;});
				int zx=shu1.get(1)- shu1.get(0);
				if(zx==asd)
				{
					shu2.addAll(shu1);
				}
			}
			if(color==2)
			{
				for (byte b : shuzhi2){              //将shuzhi2插入到shu2中
					if (b>='0'&&b<='9')
						shu1.add(b);
				}

				shu1.sort((x, y) ->{ return x - y;});
				int zx=shu1.get(1)- shu1.get(0);
				if(zx==asd)
				{
					shu2.addAll(shu1);
				}
			}
			if(color==3)
			{
				for (byte b : shuzhi2){              //将shuzhi2插入到shu2中
					if (b>='0'&&b<='9')
						shu1.add(b);
				}

				shu1.sort((x, y) ->{ return x - y;});
				int zx=shu1.get(1)- shu1.get(0);
				if(zx==asd)
				{
					shu2.addAll(shu1);
				}
			}

		}
		byte[] shuzhi2 = new byte[shu2.size()];        //定义数组
		for (int i=0;i<shuzhi2.length;i++) {
			shuzhi2[i] = shu2.get(i);    //将map中内容转换成字节型数组
		}

		return shuzhi2;
	}






	//识别破损车牌
	private byte[] RecognizeBrokenCarID(byte[] brokenCarID) {

		CameraOCRUtils ocrUtils = CameraOCRUtils.getInstance(this.paddleOCRNcnn);
		byte[] carID = ocrUtils.borkenCarIDProcess(currImage, brokenCarID);
		OCRRecognizeRes ocrRecognizeRes = new OCRRecognizeRes();
		ocrRecognizeRes.setOCRCarID("");

		return ocrRecognizeRes.packBrokenCarID();
	}

	//数组转字符串，仅用于调试输出
	private String ByteArray2String(byte[] arr) {
		StringBuilder msg_str = new StringBuilder();
		for (byte b : arr)
			msg_str.append(byte_str[b & 0xff]).append(" ");
		return msg_str.toString();
	}

	//向主车发送一条任务指令（改为protected允许类外调用）
	protected void SendTaskCommand(byte task_cmd)
	{
		CommandEncoder encoder = new CommandEncoder();
		Thread th_send = new Thread(() ->
		{
			if (dtc_client.Send(encoder.GenerateCommand(Commands.RUN_SINGE_TASK, task_cmd, (byte) 0x00, (byte) 0x00)))
				ToastLog("Send Task Command: " + byte_str[task_cmd & 0xff], false, true);
			else
				ToastLog("Failed to Send Task Command: " + byte_str[task_cmd & 0xff], false, true);
		});
		th_send.start();
	}

	/*
		以下protected函数用于在主界面外允许对主界面功能的调用，利用静态MainActivity对象绕过Android对不同Activity间函数调用的限制
		此操(mo)作(fa)仅用于实现UI分离和操作逻辑优化，与实际功能代码无关。
	 */
	//----------------------------------------由此处开始----------------------------------------
	//用于调试页面的发送调试信息函数
	protected void DebugPageCallback_DebugSend()
	{
		byte[] data = new byte[]{0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};
		dtc_client.ThreadSend(data);
	}

	//用于调试页面的发送调试信息函数
	protected void DebugPageCallback_SendByInput(Activity context)
	{
		try
		{
			byte cmd0 = (byte) Integer.parseInt(((EditText) context.findViewById(R.id.edit_cmd0)).getText().toString(), 16);
			byte cmd1 = (byte) Integer.parseInt(((EditText) context.findViewById(R.id.edit_cmd1)).getText().toString(), 16);
			byte cmd2 = (byte) Integer.parseInt(((EditText) context.findViewById(R.id.edit_cmd2)).getText().toString(), 16);
			byte cmd3 = (byte) Integer.parseInt(((EditText) context.findViewById(R.id.edit_cmd3)).getText().toString(), 16);
			CommandEncoder encoder = new CommandEncoder();
			byte[] cmd = encoder.GenerateCommand(cmd0, cmd1, cmd2, cmd3);
			dtc_client.ThreadSend(cmd);
			//print debug information
			Message.obtain(recvHandler, Flags.PRINT_DATA_ARRAY, cmd).sendToTarget();
		}
		catch (Exception e)
		{
			ToastLog("Input Data Invalidate", true, false);
		}
	}

	protected void trafficLightResDebug(Activity context){
		//发送的样例
		//TrafficLightRes trafficLightRes = new TrafficLightRes();
		byte[] buffer = new byte[] {0x55, (byte) 0xAA, (byte) 0xA4, 0x06, 0x41, 0x34, 0x33, 0x39, 0x4B, 0x37, 0x69, (byte) 0xBB};
		System.out.println(buffer);
		//dtc_client.Send(buffer);
		dtc_client.ThreadSend(buffer);
		Message.obtain(recvHandler,Flags.PRINT_DATA_ARRAY,buffer).sendToTarget();
	}

	//提供给单元测试页面用于初始化按钮的回调函数
	protected void InitSingleFunctionTestUnit(Activity context)
	{

		context.findViewById(R.id.btn_camera_up).setOnClickListener(view ->
		{
			Thread th_send = new Thread(() ->
			{
				if (!CameraOperator.SendCommand(IPCamera, Flags.CAMERA_UP, Flags.CAMERA_STEP_2))
					ToastLog("Camera Command (UP) Failure.", false, true);
			});
			th_send.start();
		});

		context.findViewById(R.id.btn_camera_down).setOnClickListener(view ->
		{
			Thread th_send = new Thread(() ->
			{
				if (!CameraOperator.SendCommand(IPCamera, Flags.CAMERA_DOWN, Flags.CAMERA_STEP_2))
					ToastLog("Camera Command (DOWN) Failure.", false, true);
			});
			th_send.start();
		});

		context.findViewById(R.id.btn_camera_left).setOnClickListener(view ->
		{
			Thread th_send = new Thread(() ->
			{
				if (!CameraOperator.SendCommand(IPCamera, Flags.CAMERA_LEFT, Flags.CAMERA_STEP_2))
					ToastLog("Camera Command (LEFT) Failure.", false, true);
			});
			th_send.start();
		});

		context.findViewById(R.id.btn_camera_right).setOnClickListener(view ->
		{
			Thread th_send = new Thread(() ->
			{
				if (!CameraOperator.SendCommand(IPCamera, Flags.CAMERA_RIGHT, Flags.CAMERA_STEP_2))
					ToastLog("Camera Command (RIGHT) Failure.", false, true);
			});
			th_send.start();
		});

		context.findViewById(R.id.btn_start_qr).setOnClickListener(view ->
		{
			//ToastLog("QR Code Started", false, false);
			//默认情况下使用黑白二维码识别
//			ToastLog("QR Result: " + RecognizeVehicle(), false, false);
//			String t1 = RecognizeCarID();
//			byte a1 = RecognizeVehicle();
//			byte a2 = Carcolor();
//			if(a1 == a2){
//				System.out.println(t1);
//			}
			cameraRequest.get(CameraRequest.STATIC_SIGNS_URL);
			context.finish();
		});

		context.findViewById(R.id.btn_start_red_qr).setOnClickListener(view ->
		{
			LogUtil.log("get CAR_ID detect Commond");
			cameraRequest.get(CameraRequest.DISPLAY_URL);
			Sleep(1500);
			Sleep(1500);
			MediaUtils.saveBitmapToGallery(MainActivity.this, MainActivity.this.currImage, "CARID");
			Sleep(1500);
			String res =null;

			if(rec == 0) {
				rec = RecognizeVehicle2();
			}
			if(rec==1) {
				if (b == 0) {
					b = RecognizeVehicle();
					CommandEncoder ce1 = new CommandEncoder();
					byte[] arr3 = ce1.GenerateCommand((byte) 0xB4, (byte) 0x00, (byte) 0x00, (byte) 0x00);
					dtc_client.Send(arr3);

					context.finish();
				}
			}

			if(b != 0) {
				res = RecognizeCarID();
			}
//			try {
//				MainActivity.this.currImage = BitmapFactory.decodeStream(MainActivity.this.getAssets().open("054.png"));
//			} catch (IOException e) {
//				throw new RuntimeException(e);
//			}
//										byte[] res = getVehicle(currImage);

			if (res != null) {
//											byte ct = RecognizeVehicle();
//											if (ct != CameraOCRUtils.CAR_ID_COLOR) {
//												CommandEncoder ce1 = new CommandEncoder();
//												byte[] arr3 = ce1.GenerateCommand((byte) 0xB4, (byte) 0x00, (byte) 0x00, (byte) 0x00);
//												dtc_client.Send(arr3);
//											}

				byte[] arr = res.getBytes() ;
				CommandEncoder ce = new CommandEncoder();
				byte[] arr1 = ce.GenerateCommand((byte) 0xA4, arr[0], arr[1], arr[2], arr[3], arr[4], arr[5],b,(byte)0x00);
//											CommandEncoder ce1 = new CommandEncoder();
//											byte[] arr2 = ce1.GenerateCommand((byte) 0xA5, arr[3], arr[4], arr[5]);
//											CommandEncoder ce2 = new CommandEncoder();
//											byte[] arr3 = ce2.GenerateCommand((byte)0xAE, ct, (byte)0x00,(byte)0x00);
				dtc_client.Send(arr1);
//											Sleep(2000);
//											dtc_client.Send(arr2);
//											Sleep(2000);
//											dtc_client.Send(arr3);
			} else {
				CommandEncoder ce1 = new CommandEncoder();
				byte[] arr3 = ce1.GenerateCommand((byte) 0xB4, (byte) 0x00, (byte) 0x00, (byte) 0x00);
				dtc_client.Send(arr3);
			}
			context.finish();
		});

		context.findViewById(R.id.btn_start_green_qr).setOnClickListener(view ->
		{
			LogUtil.log("get CAR_ID detect Commond");
			cameraRequest.get(CameraRequest.DISPLAY_URL);
			Sleep(1500);
			Sleep(1500);
			MediaUtils.saveBitmapToGallery(MainActivity.this, MainActivity.this.currImage, "CARID");
			Sleep(1500);
			String res =null;

			if(rec == 0) {
				rec = RecognizeVehicle2();
			}
			if(rec==1) {
				if (b == 0) {
					b = RecognizeVehicle();
					CommandEncoder ce1 = new CommandEncoder();
					byte[] arr3 = ce1.GenerateCommand((byte) 0xB4, (byte) 0x00, (byte) 0x00, (byte) 0x00);
					dtc_client.Send(arr3);

					context.finish();
				}
			}

			if(b != 0) {
				res = RecognizeCarID();
			}
//			try {
//				MainActivity.this.currImage = BitmapFactory.decodeStream(MainActivity.this.getAssets().open("054.png"));
//			} catch (IOException e) {
//				throw new RuntimeException(e);
//			}
//										byte[] res = getVehicle(currImage);

			if (res != null) {
//											byte ct = RecognizeVehicle();
//											if (ct != CameraOCRUtils.CAR_ID_COLOR) {
//												CommandEncoder ce1 = new CommandEncoder();
//												byte[] arr3 = ce1.GenerateCommand((byte) 0xB4, (byte) 0x00, (byte) 0x00, (byte) 0x00);
//												dtc_client.Send(arr3);
//											}

				byte[] arr = res.getBytes() ;
				CommandEncoder ce = new CommandEncoder();
				byte[] arr1 = ce.GenerateCommand((byte) 0xA4, arr[0], arr[1], arr[2], arr[3], arr[4], arr[5],b,(byte)0x00);
//											CommandEncoder ce1 = new CommandEncoder();
//											byte[] arr2 = ce1.GenerateCommand((byte) 0xA5, arr[3], arr[4], arr[5]);
//											CommandEncoder ce2 = new CommandEncoder();
//											byte[] arr3 = ce2.GenerateCommand((byte)0xAE, ct, (byte)0x00,(byte)0x00);
				dtc_client.Send(arr1);
//											Sleep(2000);
//											dtc_client.Send(arr2);
//											Sleep(2000);
//											dtc_client.Send(arr3);
			} else {
				CommandEncoder ce1 = new CommandEncoder();
				byte[] arr3 = ce1.GenerateCommand((byte) 0xB4, (byte) 0x00, (byte) 0x00, (byte) 0x00);
				dtc_client.Send(arr3);
			}
			context.finish();
		});

		context.findViewById(R.id.btn_start_yellow_qr).setOnClickListener(view ->
		{
			ToastLog("QR Code Started (Yellow)", false, false);
			//针对彩色二维码单独测试二维码识别
			ToastLog("QR (Yellow) Result: " + ByteArray2String(RecognizeQrCode(true, GlobalColor.YELLOW)), false, false);
			context.finish();
		});
		//交通灯
		context.findViewById(R.id.btn_start_light).setOnClickListener(view ->
		{

			cameraRequest.get(CameraRequest.TRAFFIC_LIGHT_URL);
			Sleep(3000);
			context.finish();

			//ToastLog("TL Result: " + ByteArray2String(RecognizeTrafficLight()), false, false);

		});
		//形状颜色
		context.findViewById(R.id.btn_start_color_shape).setOnClickListener(view ->
		{
//			ToastLog("Color Shape Started", false, false);
//			ToastLog("CS Result: ", false, false);
//			Carcolor();
			MediaUtils.saveBitmapToGallery(MainActivity.this, MainActivity.this.currImage, "");
			context.finish();



		});
		//车牌
		context.findViewById(R.id.btn_start_car_id).setOnClickListener(view ->
		{

			cameraRequest.get(CameraRequest.DISPLAY_URL);
//			ToastLog("Car ID Started", false, false);
//			Thread th_debug = new Thread(this::RecognizeCarID);
//			th_debug.start();
//			ToastLog("CID Finished", false, false);
			context.finish();
		});
		//标志
		context.findViewById(R.id.btn_start_sign).setOnClickListener(view ->
		{
			ToastLog("Traffic Sign Started", false, false);
			//ToastLog("TS Result: " + ByteArray2String(RecognizeTrafficSign()), false, false);
			context.finish();
		});
		//静态文本OCR
		context.findViewById(R.id.btn_start_ocr).setOnClickListener(view ->
		{
			ToastLog("OCR Started", false, false);
			Thread th_debug = new Thread(this::OCRRecognizeText);
			th_debug.start();
			context.finish();
		});

		context.findViewById(R.id.btn_tft_page_down).setOnClickListener(view ->
		{
			CommandEncoder encoder = new CommandEncoder();
			dtc_client.ThreadSend(encoder.GenerateCommand(Commands.TFT_PAGE_DOWN, (byte) 0, (byte) 0, (byte) 0));
			ToastLog("TFT Page Down Command Send.", false, true);
		});

		context.findViewById(R.id.btn_movement_control).setOnClickListener(view -> startActivity(new Intent(this, MovementController.class)));

		context.findViewById(R.id.btn_crash).setOnClickListener(view ->
		{
			//崩溃按钮的作用：频繁调试时省去手动退出程序，清理后台的操作，节省时间。
			dtc_client.DisableAutoReconnect();
			dtc_client.CloseConnection();    //关闭通信
			throw new NullPointerException();    //通过异常来崩溃。
		});

		context.findViewById(R.id.btn_os_shapecolor).setOnClickListener(view ->
		{
			//TODO
		});

		context.findViewById(R.id.btn_os_trafficsign).setOnClickListener(view ->
		{

		});

		context.findViewById(R.id.btn_os_vehicle).setOnClickListener(view ->
		{

		});
	}
	//----------------------------------------到此处终止----------------------------------------

	//初始化图形界面
	private void InitGUI()
	{
		pic_received = findViewById(R.id.camera_image);
		text_toast = findViewById(R.id.text_toast);

		text_toast.setMovementMethod(ScrollingMovementMethod.getInstance());

		findViewById(R.id.btn_debug_page).setOnClickListener(view ->
		{
			DebugPage.Parent = this;
			startActivity(new Intent(this, DebugPage.class));
		});

		findViewById(R.id.btn_function_test).setOnClickListener(view ->
		{
			SingleFunctionTest.Parent = this;
			startActivity(new Intent(this, SingleFunctionTest.class));
		});

		findViewById(R.id.btn_race_tasks).setOnClickListener(view ->
		{
			RaceTasks.Parent = this;
			startActivity(new Intent(this, RaceTasks.class));
		});

		findViewById(R.id.btn_program_exit).setOnClickListener(view ->
		{
			dtc_client.DisableAutoReconnect();
			dtc_client.CloseConnection();    //关闭通信
			System.exit(0);
		});

		findViewById(R.id.text_toast).setOnLongClickListener(view ->
		{
			ToastLog("Log Cleared", true, false);
			((TextView) view).setText("");
			return true;
		});

		findViewById(R.id.camera_image).setOnLongClickListener(view ->
		{
			String time = String.valueOf(System.currentTimeMillis());

			/*if (SaveImage(currImage, time))
				ToastLog("Image Saved", true, false);
			else
				ToastLog("Image Save Failure", true, false);*/


			return true;
		});
	}

	//打印日志
	@SuppressLint("SetTextI18n")
	private void ToastLog(String text, boolean real_toast, boolean on_thread)
	{
		if (text_toast == null)
			text_toast = findViewById(R.id.text_toast);
		if (real_toast)
			Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
		if (on_thread)
			Message.obtain(recvHandler, Flags.PRINT_SYSTEM_LOG, text).sendToTarget();
		else
		{
			Log.i("ToastBackup", text);
			text_toast.setText(text_toast.getText().toString() + "\n" + text);
		}
	}

	//等待一段时间
	private void Sleep(long ms)
	{
		try
		{
			sleep(ms);
		}
		catch (InterruptedException ignored)
		{
		}
	}

	//启动获取摄像头拍摄到的图片的线程
	private void StartCameraImageUpdate(int duration)
	{
		Thread th_image = new Thread(() ->
		{
			while (true)
			{
				currImage = CameraOperator.GetImage(IPCamera);
				recvHandler.sendEmptyMessage(Flags.RECEIVED_IMAGE);
				Sleep(duration);
			}
		});
		th_image.start();
		ToastLog("Camera Update Thread: " + th_image.getState(), false, true);
	}

	private Mat rotateImage(Mat inputImage, float angle) {
		Mat rotatedMat = new Mat();
		Point center = new Point(inputImage.cols() / 2, inputImage.rows() / 2);
		Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1.0);
		Imgproc.warpAffine(inputImage, rotatedMat, rotationMatrix, new Size(inputImage.cols(), inputImage.rows()));
		return rotatedMat;
	}



	@Override
	protected void onCreate(Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		// 检查并请求 WRITE_EXTERNAL_STORAGE 权限（如果需要）
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {
			// 请求权限
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
		}

		Objects.requireNonNull(getSupportActionBar()).setTitle("主车远端服务程序");
		System.out.println("启动");
		//初始化图形用户界面
		InitGUI();
		ToastLog("GUI Init Finished.", false, false);

		//初始化OpenCV
		OpenCV.initAsync(this);
//		//初始化WeChatQRCodeDetector
		WeChatQRCodeDetector.init(this);

		String fileName = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
		LogUtil.init(this, fileName + ".log");


//		MediaUtils.saveBitmapToGallery(this, bitmap, "erweima001");
//
//		LogUtil.log("存储图片成功！");

		boolean ret_init = yolov5TrafficLightNcnn.Init(getAssets());
		if (!ret_init)
		{
			Log.e("MainActivity", "yolov5TrafficLightNcnn Init failed");
		}
		ret_init = efficientnetTrafficLightNcnn.Init(getAssets());
		if (!ret_init)
		{
			Log.e("MainActivity", "efficientnetTrafficLightNcnn Init failed");
		}
		ret_init = yoloV5TrafficLogoNcnn.Init(getAssets());
		if (!ret_init)
		{
			Log.e("MainActivity", "yoloV5TrafficLogoNcnn Init failed");
		}
		ret_init = yoloV5CarTypeNcnn.Init(getAssets());
		if (!ret_init)
		{
			Log.e("MainActivity", "yoloV5CarTypeNcnn Init failed");
		}
		ret_init = paddleOCRNcnn.Init(getAssets());
		if (!ret_init)
		{
			Log.e("MainActivity", "paddleOCRNcnn Init failed");
		}

//		int right = 0, wrong = 0, rw = 0, yw = 0;
//		for(int i = 15;i < 18;i++){
//			Bitmap bitmap;
//			try {
//				String fn = String.format("dir4/%04d.jpg", i);
//				Log.d("Check", "检测图片：" + fn);
//				currImage = BitmapFactory.decodeStream(this.getAssets().open(fn));
//			} catch (IOException e) {
//				throw new RuntimeException(e);
//			}
//
//			byte[] res = this.RecognizeTrafficLight();
//			if(res.length == 0)continue;
//			int s = (int)res[0];
//
//		}
//
//		Log.d("Result", String.format("right:%d, wrong:%d, rw:%d, yw:%d", right, wrong, rw, yw));


		//车型
//		Bitmap bitmap = null;
//		try {
//			currImage = BitmapFactory.decodeStream(this.getAssets().open("t1.png"));
//
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//		//getVehicle(bitmap);
//		ResultObj[] yoloObjs = yoloV5CarTypeNcnn.Detect(bitmap, false);

//		try {
//			RecognizeQrcodeMn29();
//		} catch (NoSuchPaddingException e) {
//			throw new RuntimeException(e);
//		} catch (NoSuchAlgorithmException e) {
//			throw new RuntimeException(e);
//		} catch (InvalidKeyException e) {
//			throw new RuntimeException(e);
//		} catch (IllegalBlockSizeException e) {
//			throw new RuntimeException(e);
//		} catch (BadPaddingException e) {
//			throw new RuntimeException(e);
//		}
//二维码调试
//		RecognizeQrCodeByWechatWithRotate3Color();

		//获取主车IP地址
		wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		dhcpInfo = wifiManager.getDhcpInfo();
		IPCar = Formatter.formatIpAddress(dhcpInfo.gateway);
		ToastLog("DHCP Server Address: " + IPCar, false, false);

		//建立连接
		if (CommunicationUsingWifi)
			dtc_client = new WifiTransferCore(IPCar, 60000, recvHandler);
		else
			dtc_client = new SerialPortTransferCore(SerialPortPath, 115200, recvHandler);
		Thread th_connect = new Thread(() ->
		{
			if (dtc_client.Connect())
				ToastLog("Client Connected", false, true);
			else
				ToastLog("Client Connect Failed", false, true);
		});



		th_connect.start();
		while (th_connect.isAlive())
			Sleep(10);    //Wait for Connection Thread
		dtc_client.EnableAutoReconnect();    //启动自动重连

		//寻找摄像头，获取其IP地址
		Thread camera_thread = new Thread(() ->
		{
			boolean success;
			ToastLog("CameraSearchThread: Camera Thread Start", false, true);
			cameraSearcher = new CameraSearcher();
			do
			{
				success = cameraSearcher.SearchOnce();
			}
			while (!success);    //避免”while循环具有空体“警告
			IPCamera = cameraSearcher.GetCameraIP();
			ToastLog("CameraSearchThread: Camera Found. IP: " + IPCamera, false, true);
			ToastLog("Camera Address: " + IPCamera, false, true);
			StartCameraImageUpdate(50);
			//这里是程序初始化的最后一步，能到达此处标志着自检通过
			SystemStatus = true;
		});
		camera_thread.start();
		cameraRequest.get(CameraRequest.STATIC_SIGNS_URL);

		//获取外部存储权限并释放识别使用的标准图和OCR的训练模型
		Intent permission_page = new Intent(this, PermissionGetter.class);
		startActivityForResult(permission_page, permission_request_code);


	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == permission_request_code)
		{
			if (resultCode == RESULT_OK)
			{
				ImageReleaser releaser = new ImageReleaser(this);
				OCRDataReleaser releaser_ocr = new OCRDataReleaser(this);
				FileStatus = (releaser.ReleaseAllImage() && releaser_ocr.ReleaseAllFiles());
				ToastLog(releaser.toString(), false, false);
				ToastLog(releaser_ocr.toString(), false, false);
			}
			else
				FileStatus = false;
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		//Activity销毁时关闭通信
		dtc_client.DisableAutoReconnect();
		dtc_client.CloseConnection();
	}
}