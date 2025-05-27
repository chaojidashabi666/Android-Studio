/*
 * Copyright (c) 2022. UnknownNetworkService Group
 * This file is created by UnknownObject at 2022 - 9 - 18
 */

//包声明:指定该类所在的包
package com.qrs.maincarcontrolapp.gui;

//静态导入MotionEffect中的TAG常量，用于日志标记
import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;
//静态导入Thread的sleep方法，便于后续代码直接调用sleep
import static java.lang.Thread.sleep;

//导入Android相关库
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
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Formatter;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

//导入AndroidX相关库
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

//导入三方库与本地项目类
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
import com.qrs.maincarcontrolapp.detect.YoloV5CarTypeNcnn;
import com.qrs.maincarcontrolapp.detect.YoloV5TrafficLightNcnn;
import com.qrs.maincarcontrolapp.detect.YoloV5TrafficLogoNcnn;
import com.qrs.maincarcontrolapp.msg.OCRRecognizeRes;
import com.qrs.maincarcontrolapp.msg.QRCodeRes;
import com.qrs.maincarcontrolapp.msg.TrafficLightRes;
import com.qrs.maincarcontrolapp.msg.TrafficSignRes;
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

//导入OpenCV相关类
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
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
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

//导入Java标准库
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

//MainActivity类，继承自AppCompatActivity，应用主界面
public class MainActivity extends AppCompatActivity
{
	//检测模型相关成员变量
	//红绿灯检测模型，YOLOv5
	private YoloV5TrafficLightNcnn yolov5TrafficLightNcnn = new YoloV5TrafficLightNcnn();

	//红绿灯分类模型，Efficientnet
	private EfficientnetTrafficLightNcnn efficientnetTrafficLightNcnn = new EfficientnetTrafficLightNcnn();

	//ORC识别模型
	private PaddleOCRNcnn paddleOCRNcnn = new PaddleOCRNcnn();

	//交通标志检测模型
	private YoloV5TrafficLogoNcnn yoloV5TrafficLogoNcnn = new YoloV5TrafficLogoNcnn();

	//车型检测模型
	private YoloV5CarTypeNcnn yoloV5CarTypeNcnn = new YoloV5CarTypeNcnn();

	//数据接收处理器,用于消息分发和业务逻辑处理
	private final Handler recvHandler;
	//主车相机地址，默认值不重要
	private String IPCamera = "10.254.254.100";
	//摄像头当前图像
	private Bitmap currImage;
	//存储小车的IP
	private String IPCar;
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

	//红绿灯检测
	//静态代码块：加载本地OpenCV和YOLOv5动态库
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

	//构造函数：初始化Handler，处理各种消息和回调
	@SuppressLint("HandlerLeak")
	public MainActivity()
	{
		//Handler用于处理消息队列中的消息，实现主线程与工作线程的通信
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
						//创建指令解码器对象
						CommandDecoder decoder = new CommandDecoder(recv);
						//判断指令是否准备好
						if (decoder.CommandReady())
						{
							ToastLog("Command Decode Ready.", false, false);
							//新建线程执行指令对应的任务
							Thread th_run_command = new Thread(() ->
							{
								byte[] sBuf = null;
								//根据主命令类型分发任务
								//根据解码出的主命令类型进行分支处理
								switch (decoder.GetMainCommand())
								{
									//收到全自动指令，返回程序自检状态
									case Commands.RECEIVE_FULL_AUTO:
										//日志记录：收到全自动指令
										LogUtil.log("get RECEIVE_FULL_AUTO Command");
										//发送系统自检状态数据包
										dtc_client.Send(SystemStatusCommand());
										break;
									//收到QR指令，开始识别二维码，回传识别成功的数据
									case Commands.RECEIVE_QR:
										//日志记录：收到二维码指令
										LogUtil.log("get QrCode detect Commond");
										//获取当前二维码图片
										cameraRequest.get(CameraRequest.STATIC_SIGNS_URL);
										//等待1.5秒，确保图片获取完成
										Sleep(1500);
										//保存二维码图片到本地
										MediaUtils.saveBitmapToGallery(MainActivity.this, MainActivity.this.currImage, "QRCode");
										//调用二维码识别方法
										sBuf = RecognizeQrCodeByWechatWithRotate(false, null);
										//日志记录识别结果
										LogUtil.log("返回二维码的识别结果：" + TextFilter.bytesToHexString(sBuf));
										//发送二维码识别结果数据
										dtc_client.Send(sBuf);
										break;
									//收到TRAFFIC_LIGHT指令，开始识别交通灯，回传识别成功的数据
									case Commands.RECEIVE_TRAFFIC_LIGHT:
										//日志记录：收到交通灯指令
										LogUtil.log("get TRAFFIC_LIGHT detect Commond");
										//获取交通灯图片
										cameraRequest.get(CameraRequest.TRAFFIC_LIGHT_URL);
										//等待1.5秒，确保图片获取完成
										Sleep(1500);
										//保存交通灯图片
										MediaUtils.saveBitmapToGallery(MainActivity.this, MainActivity.this.currImage, "TrafficLight");
										//调用交通灯识别方法
										sBuf = RecognizeTrafficLight();
										//日志记录识别结果
										LogUtil.log("返回交通灯识别结果：" + TextFilter.bytesToHexString(sBuf));
										//发送交通灯识别结果
										dtc_client.Send(sBuf);
										break;
									//收到SHAPE_COLOR指令，开始识别形状颜色，回传识别成功的数据
									case Commands.RECEIVE_SHAPE_COLOR:
										//日志记录：收到形状颜色指令
										LogUtil.log("get SHAPE_COLOR detect Commond");
										//获取形状颜色相关图片
										cameraRequest.get(CameraRequest.DISPLAY_URL);
										//等待1.5秒，确保图片获取完成
										Sleep(1500);
										//保存图片
										MediaUtils.saveBitmapToGallery(MainActivity.this, MainActivity.this.currImage, "TrafficLight");
										//调用形状颜色识别方法
										RecognizeShapeColor();
										break;
									//收到CAR_ID指令，开始识别车牌号，回传识别成功的数据
									case Commands.RECEIVE_CAR_ID:
										//日志记录：收到车牌识别指令
										LogUtil.log("get CAR_ID detect Commond");
										//获取车牌图片
										cameraRequest.get(CameraRequest.DISPLAY_URL);
										//等待1.5秒，确保图片获取完成
										Sleep(1500);
										//保存车牌图片
										MediaUtils.saveBitmapToGallery(MainActivity.this, MainActivity.this.currImage, "CARID");
										//调用车牌识别方法
										sBuf = RecognizeCarID();
										//日志记录结果
										LogUtil.log("返回车牌识别结果！" + TextFilter.bytesToHexString(sBuf));
										//发送车牌识别结果
										dtc_client.Send(sBuf);
										break;
									//收到TRAFFIC_SIGN指令，开始识别交通标志，回传识别成功的数据
									case Commands.RECEIVE_TRAFFIC_SIGN:
										//日志记录：收到交通标志指令
										LogUtil.log("收到交通标志的识别指令！");
										//获取交通标志图片
										cameraRequest.get(CameraRequest.DISPLAY_URL);
										//等待1.5秒，确保图片获取完成
										Sleep(1500);
										//保存图片
										MediaUtils.saveBitmapToGallery(MainActivity.this, MainActivity.this.currImage, "TRAFFICSIGN");
										//调用交通标志识别方法
										sBuf = RecognizeTrafficSign();
										//日志记录结果
										LogUtil.log("返回交通标志的识别结果！" + TextFilter.bytesToHexString(sBuf));
										//发送交通标志识别结果
										dtc_client.Send(sBuf);
										break;
									//收到OCR指令，开始识别文本，回传识别成功的数据
									case Commands.RECEIVE_TEXT_OCR:
										//日志记录：收取OCP文本识别指令
										LogUtil.log("收到OCR的识别指令！");
										//获取文本图片
										cameraRequest.get(CameraRequest.STATIC_SIGNS_URL);
										//等待1.5秒，确保图片获取完成
										Sleep(1500);
										//调用文本识别方法
										sBuf = OCRRecognizeText();
										//日志记录结果
										LogUtil.log("返回OCR的识别结果！" + TextFilter.bytesToHexString(sBuf));
										//发送文本识别结果
										dtc_client.Send(sBuf);
										break;

									//收到车型识别指令，开始识别车型，回传识别成功的数据
									case Commands.RECEIVE_VEHICLE:
										/*日志记录：收到车型识别指令
										LogUtil.log("收到车型的识别指令！");
										获取车型图片
										new CameraRequest().get(CameraRequest.DISPLAY_URL);
										等待
										Sleep(1500);
										调用车型识别
										sBuf = RecognizeVehicle();
										if(sBuf != null && sBuf.length > 0){
											LogUtil.log("返回车型的识别结果！" + TextFilter.bytesToHexString(sBuf));
											dtc_client.Send(sBuf);
										}
										else{
										日志记录
											LogUtil.log("车型识别失败！");
										}
									*/
										break;
									// 收到破损车牌识别指令，开始识别，回传结果
									case Commands.RECEIVE_BROKEN_CAR_ID:
										// 日志记录：收到破损车牌指令
										LogUtil.log("收到破损车牌的识别指令！");
										// 获取图片
										cameraRequest.get(CameraRequest.DISPLAY_URL);
										// 等待
										Sleep(1500);
										// 获取破损车牌相关数据
										sBuf = decoder.GetDataCommand();
										// 日志记录
										LogUtil.log("返回破损车牌的识别结果！" + TextFilter.bytesToHexString(sBuf));
										// 发送破损车牌结果
										dtc_client.Send(sBuf);
										break;
									//收到未知指令，回传异常指令，表示无法解析当前指令
									default:
										//创建异常编码器
										CommandEncoder error = new CommandEncoder();
										// 发送“指令无法匹配”错误包
										dtc_client.Send(error.GenerateCommand(Commands.CMD_NOT_MATCH, (byte) 0x00, (byte) 0x00, (byte) 0x00));
										break;
								}
							});
							// 启动线程执行命令处理逻辑
							//启动新线程
							th_run_command.start();
						}
						//指令解析失败，回传异常指令，表示无法解析当前指令
						else
						{
							// 创建一个CommandEncoder对象，用于生成协议命令字节流
							CommandEncoder error = new CommandEncoder();
							// 调用GenerateCommand方法生成一个“指令无法匹配”(CMD_NOT_MATCH)的异常命令包，填充三个参数为0x00
							// 使用dtc_client的ThreadSend方法将该异常命令包异步（线程方式）发送给主车
							dtc_client.ThreadSend(error.GenerateCommand(Commands.CMD_NOT_MATCH, (byte) 0x00, (byte) 0x00, (byte) 0x00));
						}
					}
					//收到NULL，输出日志，不做操作
					//没有收到有效数据
					else
						ToastLog("NULL Received", true, false);
				}
			}
		};
	}


	// onResume生命周期方法：每次界面可见时都会调用，用于初始化OpenCV
	@Override
	public void onResume()
	{
		// 调用父类的onResume方法，保证生命周期正常
		super.onResume();
		// 检查OpenCV库是否已经在本地集成
		if (!OpenCVLoader.initDebug()) {
			// 如果本地没有集成OpenCV库，日志打印，并使用OpenCV Manager异步初始化
			Log.d("MainActivity", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
		} else {
			// 如果本地集成了OpenCV库，日志打印，直接回调成功
			Log.d("MainActivity", "OpenCV library found inside package. Using it!");
			mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		}
	}

	// OpenCV加载回调对象，继承自BaseLoaderCallback

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			// TODO Auto-generated method stub
			// 检查OpenCV加载状态
			switch (status){
				case BaseLoaderCallback.SUCCESS:
					// 加载成功时，输出日志
					Log.i("MainActivity", "成功加载");
					break;
				default:
					// 其它情况调用父类处理，并输出失败日志
					super.onManagerConnected(status);
					Log.i("MainActivity", "加载失败");
					break;
			}
		}
	};
	
	//处理二维码数据，使用从C++代码中导出的算法
	private byte[] ProcessQRData(ArrayList<String> qr_data)
	{
		// 用于存储有效的二维码内容
		String validate_result = "";
		// 创建指令编码器对象
		CommandEncoder encoder = new CommandEncoder();
		// 创建文本过滤器对象
		TextFilter filter = new TextFilter();
		// 遍历待检测的二维码数据
		for (String data : qr_data)
		{
			// 仅保留中文内容
			String processed_data = filter.ChineseOnly(data);
			// 如果有有效内容，则保存并退出循环
			if (processed_data.length() > 0)
			{
				validate_result = processed_data;
				break;
			}
		}
		// 如果没有有效内容，返回二维码失败的协议包
		if (validate_result.equals(""))
			return encoder.GenerateCommand(Commands.QR_FAILED, (byte) 0, (byte) 0, (byte) 0);
		else
			// 否则应该加密返回内容（功能未实现，暂时返回null）
			//return MainCarAES.CalcAES(validate_result);
			return null;
	}
	
	//处理彩色二维码数据，使用从C++代码中导出的算法
	private byte[] ProcessColoredQRData(String qr_data)
	{
		// 创建命令编码器
		CommandEncoder encoder = new CommandEncoder();
		// 如果内容为空，返回二维码失败协议包
		if (qr_data.equals(""))
			return encoder.GenerateCommand(Commands.QR_FAILED, (byte) 0, (byte) 0, (byte) 0);
		else
			// 否则应该加密返回（功能未实现，返回null）
//			return MainCarAES.CalcAES(qr_data);
			return null;
	}
	
	//获取程序自检指令，根据自检状态返回成功或失败
	private byte[] SystemStatusCommand()
	{
		// 创建命令编码器
		CommandEncoder encoder = new CommandEncoder();
		// 如果系统和文件状态都为true，返回自检成功包
		if (SystemStatus && FileStatus)
			return encoder.GenerateCommand(Commands.STATUS_SUCCESS, (byte) 0, (byte) 0, (byte) 0);
		else// 否则返回自检失败包
			return encoder.GenerateCommand(Commands.STATUS_FAILED, (byte) 0, (byte) 0, (byte) 0);
	}
	
	//识别二维码
	private byte[] RecognizeQrCode(boolean colored, GlobalColor target_color) {
		// String QRCodeStr = null; 用于存储识别到的二维码字符串内容

//		Bitmap bitmap;
//		try {
//			bitmap = BitmapFactory.decodeStream(this.getAssets().open("qr04.png"));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
		// String QRCodeStr = null; 用于存储识别到的二维码字符串内容
		String QRCodeStr = null;
		// 如果不区分颜色，直接识别当前图片二维码
		if (!colored) {
			QRCodeStr = QRCodeUtils.QRCodeAcquire(currImage);
			System.out.println(QRCodeStr);
		} else {
			//// 彩色二维码识别流程（未实现）
		}
		// 把二维码字符串转为GBK字节流，便于协议传输
		byte[] bytes = null;
		try {
			bytes = QRCodeStr.getBytes("GBK");
		} catch (UnsupportedEncodingException e) {
			// 如果转码失败直接抛出异常
			throw new RuntimeException(e);
		}
		// 创建二维码协议包对象
		QRCodeRes qrCodeRes = new QRCodeRes();
		// 设置二维码字节内容
		qrCodeRes.setQrCode(bytes);
		// 返回打包后的二维码协议数据
		return qrCodeRes.pack();
	}

	/**
	 * 每次旋转5度，旋转180度，使用微信的接口检测二维码
	 * @param colored 是否要区分颜色
	 * @param target_color 目标颜色的数值，当不需要区分颜色时设置为null，当需要区分颜色时需要指定颜色的值，与Commands类中颜色的值对应
	 * @return QRCodeRes打包后的字节数组，当没有检测到时，字节填一个0
	 */
	private byte[] RecognizeQrCodeByWechatWithRotate(boolean colored, Integer target_color){
		// 获取当前摄像头图片
		Bitmap bitmap = this.currImage;
		// 使用自定义方法检测二维码，返回所有检测到的二维码内容和颜色
		Map<String, Integer> m = this.getQrcodeResult(colored, bitmap);
		// 构造二维码协议数据对象
		QRCodeRes res = new QRCodeRes();
		// 如果检测结果为空或没有检测到内容，直接返回空协议包
		if(m == null || m.size() == 0){
			// 日志打印未检测到二维码
			LogUtil.log("None Qrcode detected");
			res.setQrCode(null);
			return res.pack();
		}
		byte[] bytes = null;
		//如果识别需要区分颜色
		if(colored){
			// 遍历所有检测到的二维码内容
			for(String s : m.keySet()){
				// 没有指定目标颜色直接返回空
				if(target_color == null){
					LogUtil.log("Specify color detect but no color assigned!");
					res.setQrCode(null);
					return res.pack();
				}

				// 找到与目标颜色相符的二维码内容
				if(m.get(s).intValue() == target_color.intValue()){
					try {
						// 按GBK编码转为字节流
						bytes = s.getBytes("GBK");
					} catch (UnsupportedEncodingException e) {
						// 转码失败直接返回空
						res.setQrCode(null);
						return res.pack();
					}
					// 设置协议数据内容
					res.setQrCode(bytes);
					// 日志打印识别结果
					LogUtil.log("qrCode without color detect result is :" + s);
					return res.pack();
				}
			}
		}
		else{
			// 不区分颜色时，直接取第一个检测到的二维码内容
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
		// 所有分支都未匹配到，返回空协议包
		return res.pack();
	}
	// 静态整型数组result，内容为实验数据（行数注释为样本编号）
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

	/**
	 * 检测图片中的二维码，支持旋转检测和颜色分类
	 * @param colored 是否对二维码进行颜色识别
	 * @param bitmap  待检测的图片
	 * @return Map<String, Integer>，key为二维码内容，value为颜色（或0表示不区分颜色）
	 */

	private Map getQrcodeResult(boolean colored, Bitmap bitmap) {
		//保存识别结果，字符串为二维码内容，Mat为区域矩阵
		// 保存识别结果，key为二维码内容，value为颜色（或0）
		Map<String, Integer> resultMap = new HashMap<>();

		// OpenCV的Mat对象，用于存储图片像素数据
		Mat rgbaMat = new Mat();
		// 将Bitmap格式图片转换为Mat格式（RGBA）
		Utils.bitmapToMat(bitmap, rgbaMat);
		//从负90度开始旋转
		int angle = -90;
		//旋转180度到+90度，每次旋转5度，检测1次,共36次，每次5度，覆盖-90到+85度
		for(int i = 0;i < 36;i++){
			// 对图片进行旋转
			Mat mat = rotateImage(rgbaMat, angle + (5 * i));
			// 新建Bitmap用于微信二维码检测
			Bitmap b1 = Bitmap.createBitmap(rgbaMat.cols(), rgbaMat.rows(), Bitmap.Config.ARGB_8888);
			// mat转为Bitmap
			Utils.matToBitmap(mat, b1);

			// 存储二维码检测到的区域点
			List<Mat> points = new ArrayList<Mat>();
			// 存储二维码检测到的字符串内容
			List<String> result1;
			// 使用微信二维码SDK检测二维码内容、区域
			result1 = WeChatQRCodeDetector.detectAndDecode(b1, points);
			// 遍历所有检测到的二维码
			for(int j = 0;j < result1.size();j++){
				// 如果该二维码内容未被添加过
				if(!resultMap.keySet().contains(result1.get(j))){
					// 不区分颜色，直接记录内容和颜色0
					if(!colored){
						resultMap.put(result1.get(j), 0);
					}
					else{
						// 需要对二维码做颜色判断
						Mat hsvMat = new Mat();
						// mat转为HSV，用于颜色分析
						Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_RGB2HSV);
						// 获取二维码区域点集
						Mat matPoints = points.get(j);
						List<Point> areaPoints = new ArrayList<>();
						// 将区域点集转为Point对象
						for(int k = 0; k < matPoints.rows();k++){
							areaPoints.add(new Point(matPoints.get(k, 0)[0], matPoints.get(k, 1)[0]));
						}
						// 通过工具类分析区域颜色，返回色号

						byte color = CvUtils.getQrcodeColorOfWechat(hsvMat, areaPoints);
						// 记录内容及颜色
						resultMap.put(result1.get(j), new Integer(color));
						// 释放HSV矩阵资源
						hsvMat.release();
					}
				}
			}
			// 释放旋转后的mat资源
			mat.release();
		}

		// 返回所有识别到的二维码及颜色
		return resultMap;
		//return null;
	}

	/**
	 * 交通灯识别主流程，YOLOv5检测+EfficientNet分类+OpenCV补充
	 * @return 打包好的交通灯识别协议数据包
	 */

	private byte[] RecognizeTrafficLight() {
		// 用YOLOv5检测交通灯（得到目标框数组）
		ResultObj[] yoloObjs = yolov5TrafficLightNcnn.Detect(currImage, false);
		// 如果检测不到任何目标
		if(yoloObjs == null || yoloObjs.length == 0){
			Log.e(TAG, "检测失败");
			LogUtil.log("检测失败, 没有找到白板！");
			//直接交给Efficientnet分类
			//返回默认红灯（1），打包协议
			TrafficLightRes trafficLightRes = new TrafficLightRes();
			trafficLightRes.setLightColor((byte)1);
			return trafficLightRes.pack();
		}
		// 筛选置信度最高的目标
		ResultObj yoloTarget = null;
		float maxScore = -1;
		for(ResultObj item : yoloObjs){
			if(item.prob > maxScore){
				maxScore = item.prob;
				yoloTarget = item;
			}
		}
		// 将当前图片转为Mat格式
		Mat mat = new Mat();
		Utils.bitmapToMat(currImage, mat);
		// 打印yolo目标信息
		System.out.println("res.score = " + yoloTarget);
		// 如果目标不合法
		if(!yoloTarget.isValid()){
			System.out.println("预测错误" );
			TrafficLightRes trafficLightRes = new TrafficLightRes();
			trafficLightRes.setLightColor((byte)1);
			return trafficLightRes.pack();
		}

		 // 根据检测框截取ROI区域
		Rect roi = new Rect((int)yoloTarget.x, (int)yoloTarget.y, (int)yoloTarget.w, (int)yoloTarget.h);
		Mat croppedMat = new Mat(mat, roi);
		// ROI转为RGB格式
		Mat rgbMat = new Mat();
		Imgproc.cvtColor(croppedMat, rgbMat, Imgproc.COLOR_RGBA2RGB);
		// 转为Bitmap以便EfficientNet分类
		//截取出目标框交给efficientnet分类
		Bitmap croppedBitmap = Bitmap.createBitmap(rgbMat.cols(), rgbMat.rows(), Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(croppedMat, croppedBitmap);

		// 释放资源
		mat.release();
		croppedMat.release();

		// 用EfficientNet分类交通灯
		int sign = efficientnetTrafficLightNcnn.Detect(croppedBitmap, false);

		// 如果EfficientNet无法判断（3），用opencv再次判定
		if(sign == 3){
			LogUtil.log("无法通过分类获取灯的颜色，将通过opencv进行检测");
			sign = this.judgeByOpencv(rgbMat);
			LogUtil.log("Opencv检测结果为：" + sign);
		}

		// 释放RGB矩阵资源
		rgbMat.release();
		byte color = 0;
		// 分类结果转换为协议色值
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

		//组包返回

		TrafficLightRes trafficLightRes = new TrafficLightRes();
		trafficLightRes.setLightColor(color);
		return trafficLightRes.pack();
//		return new byte[]{(byte)sign};
	}

	/**
	 * 用于通过OpenCV分析交通灯灯色的辅助方法。
	 * @param croppedMat 输入的交通灯区域Mat（RGB格式）。
	 * @return 0=绿灯，1=红灯，2=黄灯，-1=检测异常
	 */
	private int judgeByOpencv(Mat croppedMat) {

		// 如果宽小于高或高度太小，判为无效
		if(croppedMat.cols() < croppedMat.rows() || croppedMat.rows() <= 20){
			//检测到的内容不正确
			return -1;
		}
		// 克隆输入Mat，后续处理不破坏原数据
		Mat image = croppedMat.clone();
		// 新建Mat存储HSV格式
		Mat hsvMat = new Mat();
		// 转换颜色空间：RGB->HSV
		Imgproc.cvtColor(croppedMat, hsvMat, Imgproc.COLOR_RGB2HSV);

		// 遍历每个像素，对V通道亮度进行二值化
		for(int r = 0;r < hsvMat.rows();r++){
			for(int c = 0;c < hsvMat.cols();c++){
				double[] colors = hsvMat.get(r, c);
				// 如果亮度V<250，置黑
//				Log.d("MainActivity", String.format("colors:%f, %f, %f" ,(float)colors[0], colors[1], colors[2]));
				if(colors[2] < 250){
					image.put(r, c, 0, 0, 0);
				}
				// 否则置白
				else {
					image.put(r, c, 255,255,255);
				}
			}
		}
		// 调试保存二值化图片
		Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
		MediaUtils.saveBitmapToGallery(MainActivity.this, bitmap, "opencv1");

		// 形态学操作：闭运算（去除小噪点）
		Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
		Imgproc.morphologyEx(image, image, Imgproc.MORPH_CLOSE, kernel);

		// 转灰度
		Mat grayMat = new Mat();
		Imgproc.cvtColor(image, grayMat, Imgproc.COLOR_RGB2GRAY);

		// 调试保存处理后的灰度图片
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
		// 如果没有有效轮廓，判为异常
		// 找到最大轮廓的最小外接矩形
		if (maxContour == null) {
			return -1;

		}
		// 找到最大轮廓的最小外接矩形
		RotatedRect minRect = Imgproc.minAreaRect(new MatOfPoint2f(maxContour.toArray()));

		// 获取最小外接矩形的信息
		// 获取外接矩形四个角点
		minRect.points(points);

		// 打印调试信息
		Log.d(TAG, String.format("0x=%f,1x=%f,2x=%f,cols=%d", points[0].x, points[1].x, points[2].x, croppedMat.cols()));
		// 统计最左和最右的x坐标
		double minx =Math.min(Math.min(points[0].x, points[1].x),Math.min(points[2].x, points[3].x));
		double maxx =Math.max(Math.max(points[0].x, points[1].x),Math.max(points[2].x, points[3].x));
		// 经验规则：最左侧的点小于高度，判为红灯
		if(minx< croppedMat.rows()){
			return 1;
		}
		// 最右侧点大于宽度一半，判为绿灯
		else if(maxx > croppedMat.cols() / 2){
			return 0;
		}
		// 其余情况视为黄灯
		else{
			return 2;
		}

	}

	//识别形状颜色
	private void RecognizeShapeColor()
	{

	}
	
	//识别车牌
	private byte[] RecognizeCarID() {
//		Bitmap bitmap;
//		try {
//			bitmap = BitmapFactory.decodeStream(this.getAssets().open("6.jpg"));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
		// 通过OCR工具类获取实例，并传入paddleOCRNcnn模型
		CameraOCRUtils ocrUtils = CameraOCRUtils.getInstance(this.paddleOCRNcnn);
		// 调用OCR工具类的车牌识别方法，传入当前图片，返回车牌结果字节数组
		byte[] carID = ocrUtils.carIDProcess(currImage);
		// 如果没有识别到车牌，返回null
		if(carID == null){
			return null;
		}
		// 新建协议打包对象
		OCRRecognizeRes ocrRecognizeRes = new OCRRecognizeRes();
		// 设置识别到的车牌号
		ocrRecognizeRes.setOCRCarID(carID);
		// 打包并返回车牌协议数据
		return ocrRecognizeRes.packCarID();
	}
	
	//识别交通标志
	private byte[] RecognizeTrafficSign() {
		// 使用YOLOv5交通标志模型检测当前图片，返回检测目标数组
		ResultObj[] yoloObjs = yoloV5TrafficLogoNcnn.Detect(currImage, false);
		// 如果没有检测到任何交通标志
		if(yoloObjs == null || yoloObjs.length == 0){
			Log.e(TAG, "检测失败");
			// 新建协议对象，设置标志类型为0，打包返回
			//直接交给Efficientnet分类
			//返回0
			TrafficSignRes trafficSignRes = new TrafficSignRes();
			trafficSignRes.setTrafficSign((byte) 0);
			return trafficSignRes.pack();
		}
		// 查找置信度最高的检测目标
		ResultObj yoloTarget = null;
		float maxScore = -1;
		for(ResultObj item : yoloObjs){
			if(item.prob > maxScore){
				maxScore = item.prob;
				yoloTarget = item;
			}
		}
		// 初始化交通标志类型
		byte sign = 0;
		// 根据检测到的标签名，转换为协议中的交通标志类型
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
		// 新建协议对象，设置交通标志类型，打包返回

		TrafficSignRes trafficSignRes = new TrafficSignRes();
		trafficSignRes.setTrafficSign(sign);
		return trafficSignRes.pack();
	}

	
	//识别静态文本
	private byte[] OCRRecognizeText() {
		// 获取OCR工具类实例
		CameraOCRUtils ocrUtils = CameraOCRUtils.getInstance(this.paddleOCRNcnn);
		// 调用文本识别方法
		byte[] text = ocrUtils.textProcess(currImage);
		// 新建协议对象
		OCRRecognizeRes ocrRecognizeRes = new OCRRecognizeRes();
		// 设置识别文本
		ocrRecognizeRes.setOCRText(text);
		// 打包文本协议数据

		byte[] buf = ocrRecognizeRes.packText();
		// 控制台输出调试
		return buf;
	}
	
	//识别车型（目前未实现实际模型推理）
	private byte[] RecognizeVehicle()
	{
//		Bitmap bitmap = null;
//		Module module = null;
//		try {
//			bitmap = BitmapFactory.decodeStream(this.getAssets().open("001.png"));
//			module = Module.load(VehicleTypeUtils.assetFilePath(this, "modle22.pt"));
//		} catch (IOException e) {
//			Log.e("PytorchHelloWorld", "Error reading assets", e);
//			finish();
//		}
//		VehicleTypeUtils vehicleUtils = new VehicleTypeUtils();
//		byte sign = vehicleUtils.vehicleTypeRecognize(bitmap, module, pic_received);
		// 返回测试协议包，标志类型设为1
		TrafficSignRes trafficSignRes = new TrafficSignRes();
		trafficSignRes.setTrafficSign((byte)1);
		return trafficSignRes.pack();
	}

	//识别破损车牌
	private byte[] RecognizeBrokenCarID(byte[] brokenCarID) {
		// 获取OCR工具类实例
		CameraOCRUtils ocrUtils = CameraOCRUtils.getInstance(this.paddleOCRNcnn);
		// 识别破损车牌
		byte[] carID = ocrUtils.borkenCarIDProcess(currImage, brokenCarID);
		// 新建协议对象
		OCRRecognizeRes ocrRecognizeRes = new OCRRecognizeRes();
		// 设置识别的车牌号
		ocrRecognizeRes.setOCRCarID(carID);
		// 打包协议数据

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
		// 新建命令编码器
		CommandEncoder encoder = new CommandEncoder();
		// 新建线程异步发送命令
		Thread th_send = new Thread(() ->
		{
			// 发送命令并根据结果弹出toast提示
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
	
	//用于调试页面的发送调试信息函数（通过输入框内容）
	protected void DebugPageCallback_SendByInput(Activity context)
	{
		try
		{
			// 读取输入框内容并解析为16进制
			byte cmd0 = (byte) Integer.parseInt(((EditText) context.findViewById(R.id.edit_cmd0)).getText().toString(), 16);
			byte cmd1 = (byte) Integer.parseInt(((EditText) context.findViewById(R.id.edit_cmd1)).getText().toString(), 16);
			byte cmd2 = (byte) Integer.parseInt(((EditText) context.findViewById(R.id.edit_cmd2)).getText().toString(), 16);
			byte cmd3 = (byte) Integer.parseInt(((EditText) context.findViewById(R.id.edit_cmd3)).getText().toString(), 16);
			// 生成协议命令
			CommandEncoder encoder = new CommandEncoder();
			byte[] cmd = encoder.GenerateCommand(cmd0, cmd1, cmd2, cmd3);
			// 发送命令
			dtc_client.ThreadSend(cmd);
			// 打印调试信息到日志区
			//print debug information
			Message.obtain(recvHandler, Flags.PRINT_DATA_ARRAY, cmd).sendToTarget();
		}
		catch (Exception e)
		{
			ToastLog("Input Data Invalidate", true, false);
		}
	}

	// 测试用例：发送交通灯协议包
	protected void trafficLightResDebug(Activity context){
		// 构造协议测试数据
		//发送的样例
		//TrafficLightRes trafficLightRes = new TrafficLightRes();
		byte[] buffer = new byte[] {0x55, (byte) 0xAA, (byte) 0xA4, 0x06, 0x41, 0x34, 0x33, 0x39, 0x4B, 0x37, 0x69, (byte) 0xBB};
		System.out.println(buffer);
		// 发送测试包
		//dtc_client.Send(buffer);
		dtc_client.ThreadSend(buffer);
		// 打印调试信息到日志区
		Message.obtain(recvHandler,Flags.PRINT_DATA_ARRAY,buffer).sendToTarget();
	}
	
	//提供给单元测试页面用于初始化按钮的回调函数
	// 单元测试页面的按钮回调初始化
	protected void InitSingleFunctionTestUnit(Activity context)
	{
		// 摄像头上移
		context.findViewById(R.id.btn_camera_up).setOnClickListener(view ->
		{
			Thread th_send = new Thread(() ->
			{
				if (!CameraOperator.SendCommand(IPCamera, Flags.CAMERA_UP, Flags.CAMERA_STEP_2))
					ToastLog("Camera Command (UP) Failure.", false, true);
			});
			th_send.start();
		});

		// 摄像头下移
		context.findViewById(R.id.btn_camera_down).setOnClickListener(view ->
		{
			Thread th_send = new Thread(() ->
			{
				if (!CameraOperator.SendCommand(IPCamera, Flags.CAMERA_DOWN, Flags.CAMERA_STEP_2))
					ToastLog("Camera Command (DOWN) Failure.", false, true);
			});
			th_send.start();
		});

		// 摄像头左移
		context.findViewById(R.id.btn_camera_left).setOnClickListener(view ->
		{
			Thread th_send = new Thread(() ->
			{
				if (!CameraOperator.SendCommand(IPCamera, Flags.CAMERA_LEFT, Flags.CAMERA_STEP_2))
					ToastLog("Camera Command (LEFT) Failure.", false, true);
			});
			th_send.start();
		});

		// 摄像头右移
		context.findViewById(R.id.btn_camera_right).setOnClickListener(view ->
		{
			Thread th_send = new Thread(() ->
			{
				if (!CameraOperator.SendCommand(IPCamera, Flags.CAMERA_RIGHT, Flags.CAMERA_STEP_2))
					ToastLog("Camera Command (RIGHT) Failure.", false, true);
			});
			th_send.start();
		});

		// 黑白二维码识别测试
		context.findViewById(R.id.btn_start_qr).setOnClickListener(view ->
		{
			ToastLog("QR Code Started", false, false);
			//默认情况下使用黑白二维码识别
			ToastLog("QR Result: " + ByteArray2String(RecognizeQrCode(false, GlobalColor.INVALIDATE)), false, false);
			context.finish();
		});

		// 红色二维码识别测试
		context.findViewById(R.id.btn_start_red_qr).setOnClickListener(view ->
		{
			ToastLog("QR Code Started (Red)", false, false);
			//针对彩色二维码单独测试二维码识别
			ToastLog("QR (Red) Result: " + ByteArray2String(RecognizeQrCode(true, GlobalColor.RED)), false, false);
			context.finish();
		});

		// 绿色二维码识别测试
		context.findViewById(R.id.btn_start_green_qr).setOnClickListener(view ->
		{
			ToastLog("QR Code Started (Green)", false, false);
			//针对彩色二维码单独测试二维码识别
			ToastLog("QR (Green) Result: " + ByteArray2String(RecognizeQrCode(true, GlobalColor.GREEN)), false, false);
			context.finish();
		});

		// 黄色二维码识别测试
		context.findViewById(R.id.btn_start_yellow_qr).setOnClickListener(view ->
		{
			ToastLog("QR Code Started (Yellow)", false, false);
			//针对彩色二维码单独测试二维码识别
			ToastLog("QR (Yellow) Result: " + ByteArray2String(RecognizeQrCode(true, GlobalColor.YELLOW)), false, false);
			context.finish();
		});

		// 交通灯识别测试
		context.findViewById(R.id.btn_start_light).setOnClickListener(view ->
		{
			ToastLog("Traffic Light Started", false, false);
//			ToastLog("TL Result: " + ByteArray2String(RecognizeTrafficLight()), false, false);
			context.finish();
		});

		// 形状颜色识别测试
		context.findViewById(R.id.btn_start_color_shape).setOnClickListener(view ->
		{
			ToastLog("Color Shape Started", false, false);
			ToastLog("CS Result: ", false, false);
			RecognizeShapeColor();
			context.finish();
		});

		// 车牌识别测试
		context.findViewById(R.id.btn_start_car_id).setOnClickListener(view ->
		{
			ToastLog("Car ID Started", false, false);
			Thread th_debug = new Thread(this::RecognizeCarID);
			th_debug.start();
			ToastLog("CID Finished", false, false);
			context.finish();
		});

		// 交通标志识别测试
		context.findViewById(R.id.btn_start_sign).setOnClickListener(view ->
		{
			ToastLog("Traffic Sign Started", false, false);
			ToastLog("TS Result: " + ByteArray2String(RecognizeTrafficSign()), false, false);
			context.finish();
		});

		// OCR识别测试
		context.findViewById(R.id.btn_start_ocr).setOnClickListener(view ->
		{
			ToastLog("OCR Started", false, false);
			Thread th_debug = new Thread(this::OCRRecognizeText);
			th_debug.start();
			context.finish();
		});

		// TFT翻页测试
		context.findViewById(R.id.btn_tft_page_down).setOnClickListener(view ->
		{
			CommandEncoder encoder = new CommandEncoder();
			dtc_client.ThreadSend(encoder.GenerateCommand(Commands.TFT_PAGE_DOWN, (byte) 0, (byte) 0, (byte) 0));
			ToastLog("TFT Page Down Command Send.", false, true);
		});

		// 运动控制测试
		context.findViewById(R.id.btn_movement_control).setOnClickListener(view -> startActivity(new Intent(this, MovementController.class)));

		// 崩溃按钮，主动触发空指针异常，实现快速退出调试
		context.findViewById(R.id.btn_crash).setOnClickListener(view ->
		{
			//崩溃按钮的作用：频繁调试时省去手动退出程序，清理后台的操作，节省时间。
			dtc_client.DisableAutoReconnect();
			dtc_client.CloseConnection();    //关闭通信
			throw new NullPointerException();    //通过异常来崩溃。
		});

		// 其它功能按钮（未实现）
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
		// 获取ImageView控件，用于显示摄像头图片
		pic_received = findViewById(R.id.camera_image);

		// 获取文本显示控件，用于显示日志和Toast内容
		text_toast = findViewById(R.id.text_toast);

		// 设置文本框为可滚动模式，方便显示多行日志
		text_toast.setMovementMethod(ScrollingMovementMethod.getInstance());

		// 设置“调试页面”按钮点击事件，跳转到DebugPage
		findViewById(R.id.btn_debug_page).setOnClickListener(view ->
		{
			// 设置父页面，用于回调
			DebugPage.Parent = this;
			// 启动DebugPage Activity
			startActivity(new Intent(this, DebugPage.class));
		});

		// 设置“功能测试”按钮点击事件，跳转到SingleFunctionTest
		findViewById(R.id.btn_function_test).setOnClickListener(view ->
		{
			SingleFunctionTest.Parent = this;
			startActivity(new Intent(this, SingleFunctionTest.class));
		});

		// 设置“赛题任务”按钮点击事件，跳转到RaceTasks
		findViewById(R.id.btn_race_tasks).setOnClickListener(view ->
		{
			RaceTasks.Parent = this;
			startActivity(new Intent(this, RaceTasks.class));
		});


		// 设置“退出程序”按钮点击事件，关闭通信并退出程序
		findViewById(R.id.btn_program_exit).setOnClickListener(view ->
		{
			dtc_client.DisableAutoReconnect();// 禁用自动重连
			dtc_client.CloseConnection();    //关闭通信
			System.exit(0);			// 退出程序
		});

		// 长按日志文本框，清空日志
		findViewById(R.id.text_toast).setOnLongClickListener(view ->
		{
			// 弹出提示
			ToastLog("Log Cleared", true, false);
			//清空内容
			((TextView) view).setText("");
			return true;
		});

		// 长按摄像头图片，预留保存图片（已注释）
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

	// 打印日志到界面和系统
	@SuppressLint("SetTextI18n")
	private void ToastLog(String text, boolean real_toast, boolean on_thread)
	{
		// 如果text_toast未初始化，则重新获取
		if (text_toast == null)
			text_toast = findViewById(R.id.text_toast);
		// 如果需要显示Android原生Toast
		if (real_toast)
			Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
		// 如果需要在Handler线程中显示
		if (on_thread)
			Message.obtain(recvHandler, Flags.PRINT_SYSTEM_LOG, text).sendToTarget();
		else
		{
			// 日志输出到Logcat，便于调试
			Log.i("ToastBackup", text);
			// 日志追加到文本框
			text_toast.setText(text_toast.getText().toString() + "\n" + text);
		}
	}
	
	//等待一段时间
	// 休眠指定毫秒数
	private void Sleep(long ms)
	{
		try
		{
			sleep(ms); // 调用Thread.sleep
		}
		catch (InterruptedException ignored)
		{
		}
	}
	
	//启动获取摄像头拍摄到的图片的线程
	// 启动获取摄像头图片的线程，定时刷新currImage
	private void StartCameraImageUpdate(int duration)
	{
		Thread th_image = new Thread(() ->
		{
			while (true)
			{
				// 获取摄像头图片
				currImage = CameraOperator.GetImage(IPCamera);
				// 通知主线程刷新图片
				recvHandler.sendEmptyMessage(Flags.RECEIVED_IMAGE);
				// 等待下一次刷新
				Sleep(duration);
			}
		});
		// 启动线程
		th_image.start();
		ToastLog("Camera Update Thread: " + th_image.getState(), false, true);
	}

	// 旋转图片工具方法
	private Mat rotateImage(Mat inputImage, float angle) {
		// 新建旋转后Mat
		Mat rotatedMat = new Mat();
		// 获取旋转中心点
		Point center = new Point(inputImage.cols() / 2, inputImage.rows() / 2);
		// 计算旋转矩阵
		Mat rotationMatrix = Imgproc.getRotationMatrix2D(center, angle, 1.0);
		// 对图片进行仿射变换实现旋转
		Imgproc.warpAffine(inputImage, rotatedMat, rotationMatrix, new Size(inputImage.cols(), inputImage.rows()));
		// 返回旋转后的Mat
		return rotatedMat;
	}


	// MainActivity生命周期onCreate方法，程序入口
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// 检查并请求 WRITE_EXTERNAL_STORAGE 权限（如果需要）
		// 检查并请求写存储权限
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {
			// 请求权限
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
		}

		//"{abcd}abcd3efg"
		// 设置标题栏
		
		Objects.requireNonNull(getSupportActionBar()).setTitle("主车远端服务程序");
		System.out.println("启动");
		//初始化图形用户界面和控件
		InitGUI();
		ToastLog("GUI Init Finished.", false, false);

		//异步初始化OpenCV
		OpenCV.initAsync(this);
//		//初始化WeChatQRCodeDetector
		// 初始化微信二维码识别库
		WeChatQRCodeDetector.init(this);

		// 生成日志文件名
		String fileName = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
		LogUtil.init(this, fileName + ".log");

		// 示例表达式
		// 示例表达式，测试exp4j表达式解析库
		String expression = "2 * 3 + 5";

		// 使用 exp4j 解析表达式
		Expression exp = new ExpressionBuilder(expression).build();
		double result = exp.evaluate();

		// 显示结果


//		MediaUtils.saveBitmapToGallery(this, bitmap, "erweima001");
//
//		LogUtil.log("存储图片成功！");

		// 初始化检测模型，逐个检测并打印失败日志
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
//				bitmap = BitmapFactory.decodeStream(this.getAssets().open(fn));
//			} catch (IOException e) {
//				throw new RuntimeException(e);
//			}
//
//			this.currImage = bitmap;
//			byte[] res = this.RecognizeTrafficLight();
//			if(res.length == 0)continue;
//			int s = (int)res[0];
//
//		}
//
		//Log.d("Result", String.format("right:%d, wrong:%d, rw:%d, yw:%d", right, wrong, rw, yw));
		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory.decodeStream(this.getAssets().open("c008.png"));

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		ResultObj[] yoloObjs = yoloV5CarTypeNcnn.Detect(bitmap, false);

//		return;
//		Bitmap bitmap;
//		try {
//			String fn = "qr01.jpg";
//			Log.d("Check", "检测图片：" + fn);
//			bitmap = BitmapFactory.decodeStream(this.getAssets().open(fn));
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//		this.currImage = bitmap;
//		RecognizeQrCodeByWechatWithRotate(false, 0);
		// 获取主车IP地址（网关地址）
		wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		dhcpInfo = wifiManager.getDhcpInfo();
		IPCar = Formatter.formatIpAddress(dhcpInfo.gateway);
		ToastLog("DHCP Server Address: " + IPCar, false, false);

		//建立连接
		// 初始化通信核心（wifi或串口
		if (CommunicationUsingWifi)
			dtc_client = new WifiTransferCore(IPCar, 60000, recvHandler);
		else
			dtc_client = new SerialPortTransferCore(SerialPortPath, 115200, recvHandler);
		// 新建线程尝试连接服务器
		Thread th_connect = new Thread(() ->
		{
			if (dtc_client.Connect())
				ToastLog("Client Connected", false, true);
			else
				ToastLog("Client Connect Failed", false, true);
		});

		// 等待连接线程结束
		th_connect.start();
		while (th_connect.isAlive())

			//Wait for Connection Thread  // 等待10毫秒再检查
			Sleep(10);

		//启动自动重连
		dtc_client.EnableAutoReconnect();

		//寻找摄像头，获取其IP地址
		// 寻找摄像头并获取其IP地址，启动图片刷新线程
		Thread camera_thread = new Thread(() ->
		{
			boolean success;
			ToastLog("CameraSearchThread: Camera Thread Start", false, true);
			cameraSearcher = new CameraSearcher();
			do
			{
				success = cameraSearcher.SearchOnce();
			}

			// 循环直到找到摄像头
			while (!success);    //避免”while循环具有空体“警告
			IPCamera = cameraSearcher.GetCameraIP();
			ToastLog("CameraSearchThread: Camera Found. IP: " + IPCamera, false, true);
			ToastLog("Camera Address: " + IPCamera, false, true);
			StartCameraImageUpdate(50);
			// 50ms刷新一次图片
			//这里是程序初始化的最后一步，能到达此处标志着自检通过
			// 所有初始化完成，系统自检通过
			SystemStatus = true;
		});
		camera_thread.start();
		// 初始请求一次静态标志物图片
		cameraRequest.get(CameraRequest.STATIC_SIGNS_URL);

		//获取外部存储权限并释放识别使用的标准图和OCR的训练模型
		// 权限页面：获取外部存储权限并释放模型文件
		Intent permission_page = new Intent(this, PermissionGetter.class);
		startActivityForResult(permission_page, permission_request_code);


	}

	// 权限请求结果回调
	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		// 判断是否为本程序的权限请求
		if (requestCode == permission_request_code)
		{
			if (resultCode == RESULT_OK)
			{
				// 释放图片和OCR模型文件
				ImageReleaser releaser = new ImageReleaser(this);
				OCRDataReleaser releaser_ocr = new OCRDataReleaser(this);
				FileStatus = (releaser.ReleaseAllImage() && releaser_ocr.ReleaseAllFiles());
				// 打印释放结果日志
				ToastLog(releaser.toString(), false, false);
				ToastLog(releaser_ocr.toString(), false, false);
			}
			else
				// 权限或释放失败
				FileStatus = false;
		}
	}


	// Activity销毁时关闭通信
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		//Activity销毁时关闭通信
		// 禁止自动重连并关闭通信
		dtc_client.DisableAutoReconnect();
		dtc_client.CloseConnection();
	}
}